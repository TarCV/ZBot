// --------------------------------------------------------------------------
// Copyright (C) 2012-2013 Best-Ever & The Sentinel's Playground
// Copyright (C) 2021 TarCV
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// --------------------------------------------------------------------------

package org.bestever.bebot;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.catnip.entity.channel.GuildChannel;
import com.mewna.catnip.entity.channel.MessageChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.guild.UnavailableGuild;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.misc.Ready;
import com.mewna.catnip.entity.partials.HasName;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.shard.DiscordEvent;
import com.mewna.catnip.shard.GatewayIntent;
import org.bestever.serverquery.QueryManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;

import static org.bestever.bebot.AccountType.ADMIN;
import static org.bestever.bebot.AccountType.MODERATOR;
import static org.bestever.bebot.AccountType.REGISTERED;
import static org.bestever.bebot.AccountType.VIP;
import static org.bestever.bebot.AccountType.isAccountTypeOf;
import static org.bestever.bebot.Logger.LOGLEVEL_CRITICAL;
import static org.bestever.bebot.Logger.LOGLEVEL_DEBUG;
import static org.bestever.bebot.Logger.LOGLEVEL_IMPORTANT;
import static org.bestever.bebot.Logger.LOGLEVEL_NORMAL;
import static org.bestever.bebot.Logger.LOGLEVEL_TRIVIAL;
import static org.bestever.bebot.Logger.logMessage;

/**
 * This is where the bot methods are run and handling of channel/PM input are processed
 */
public class Bot {

	private final Catnip catnip;

	/**
	 * Path to the configuration file relative to the bot
	 */
	private String config_file;

	/**
	 * The lowest port (the base port) that the bot uses. This should NEVER be
	 * changed because the mysql table relies on the minimum port to stay the
	 * same so it can grab the proper ID (primary key) which corresponds to
	 * server storage and ports.
	 */
	public static int min_port;

	/**
	 * The highest included port number that the bot uses
	 */
	public static int max_port;

	/**
	 * When the bot was started
	 */
	public long time_started;

	/**
	 * A toggle variable for allowing hosting
	 */
	private boolean botEnabled = true;

	/**
	 * Contains the config data
	 */
	public ConfigData cfg_data;

	/**
	 * Contained a array list of all the servers
	 */
	public LinkedList<Server> servers;

	public HashMap<String, LinkedList<Server>> vSHashmap;

	/**
	 * Holds the timer (for timed broadcasts)
	 */
	public Timer timer;

	/**
	 * A query manager thread for handling external server requests
	 */
	private QueryManager queryManager;

	/**
	 * The amount of times the "terminate" command has been confirmed.
	 */
	private int terminateConfirmationTimes = 0;

	/**
	 *
	 */
	private long terminateTimestamp = System.currentTimeMillis();

	private Boolean debugMode = true;

	public final VersionParser versionParser;

	public boolean hasStarted = false;

	private String guildId;

	/**
	 * Set the bot up with the constructor
	 */
	public Bot(ConfigData cfgfile) {
		// Point our config data to what we created back in RunMe.java
		cfg_data = cfgfile;

		// Set up the logger
		Logger.setLogFile(cfg_data.bot_logfile);

		// Parse versions.json
		versionParser = new VersionParser(cfg_data.bot_versionsfile, this);

		// Set initial ports
		this.min_port = cfg_data.bot_min_port;
		this.max_port = cfg_data.bot_max_port;

		// Set up the notice timer (if set)
		if (cfg_data.bot_notice != null) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new NoticeTimer(this), 1000, cfg_data.bot_notice_interval * 1000L);
		}

		// Set up the server arrays
		this.servers = new LinkedList<>();
		this.vSHashmap = new HashMap<>();
		for (Version v : versionParser.list)
			this.vSHashmap.put(v.name, new LinkedList<Server>());

		// Set up MySQL
		 MySQL.setMySQL(this, cfg_data.mysql_host, cfg_data.mysql_user, cfg_data.mysql_pass, cfg_data.mysql_port, cfg_data.mysql_db);

		// Get the time the bot was started
		this.time_started = System.currentTimeMillis();

		// Begin a server query thread that will run
		queryManager = new QueryManager(this);
		queryManager.start();

		// Set up the bot and join the channel
		logMessage(LOGLEVEL_IMPORTANT, "Initializing ZBot");

		final CatnipOptions options = new CatnipOptions(cfg_data.discord_token);
		{
			final HashSet<GatewayIntent> gatewayIntents = new HashSet<>();
			gatewayIntents.addAll(GatewayIntent.UNPRIVILEGED_INTENTS);
			gatewayIntents.add(GatewayIntent.GUILD_PRESENCES);
			options.intents(gatewayIntents);
		}

		catnip = Catnip.catnip(options);
		catnip
				.observable(DiscordEvent.READY)
				.subscribe(
						this::onConnect,
						error -> {
							logMessage(LOGLEVEL_CRITICAL, "Exception occurred while connecting to the network, terminating bot!");
							System.exit(0);
							error.printStackTrace();
						}
				);

		catnip
				.observable(DiscordEvent.MESSAGE_CREATE)
				.subscribe(this::onMessage);

		catnip.connect();
	}

	/**
	 * Called when the bot connects to the irc server
	 * @param ready
	 */
	public void onConnect(Ready readyMsg) {
		// Set up MySQL (Just in case it didn't before it connected)
		 MySQL.setMySQL(this, cfg_data.mysql_host, cfg_data.mysql_user, cfg_data.mysql_pass, cfg_data.mysql_port, cfg_data.mysql_db);

		final Set<UnavailableGuild> guilds = readyMsg.guilds();
		if (guilds.size() != 1) {
			throw new  IllegalStateException("Exactly one guild should be available, got " + guilds);
		}
		this.guildId = guilds.iterator().next().id();

		sendMessageToCoreChannel("Hello, world!");
		sendLogInfoMessage("Bot started.");


		if (!hasStarted && MySQL.shouldRecover()) {
			sendMessageToCoreChannel("Recovering Servers, Please Wait!");
			sendMessageToCoreChannel("Note: All server passwords will be reset to defaults.");
			MySQL.doRecovery();
		}
		hasStarted = true;
	}

	/**
	 * Gets the minimum port to be used by the bot
	 * @return An integer containing the minimum port used
	 */
	public int getMinPort() {
		return min_port;
	}

	/**
	 * Returns the max port used by the bot
	 * @return An integer containing the max port used
	 */
	public int getMaxPort() {
		return max_port;
	}

	/**
	 * Reloads the configuration file
	 */
	public void reloadConfigFile() {
		try {
			this.cfg_data = new ConfigData(this.config_file);
		} catch (IOException e) {
			logMessage(LOGLEVEL_CRITICAL, "Could not reload configuration file.");
		}
	}

	/**
	 * Adds a wad to the automatic server startup
	 * @param wad String - the name of the wad
	 * @param sender
	 */
	public void addExtraWad(String wad, Member sender, MessageChannel channel) {
		if (!Functions.fileExists(cfg_data.bot_wad_directory_path + wad)) {
			channel.sendMessage("Error: file " + wad + " does not exist!");
			return;
		}
		for (String listWad : cfg_data.bot_extra_wads) {
			if (listWad.equalsIgnoreCase(wad)) {
				channel.sendMessage("Error: file " + listWad + " is already in the startup list!");
				return;
			}
		}
		cfg_data.bot_extra_wads.add(wad);
		channel.sendMessage("Added " + wad + " to the startup list!");
		sendLogModeratorMessage(bold(userInfo(sender)) + " adds " + bold(wad) + " to the startup WAD list");
	}

	public static String bold(String message) {
		return "**" + message + "**";
	}

	public static String bold(Number message) {
		return "**" + message + "**";
	}

	/**
	 * Removes a wad from the wad startup list
	 * @param wad String - name of the wad
	 * @param sender
	 */
	public void deleteExtraWad(String wad, Member sender, MessageChannel channel) {
		for (String listWad : cfg_data.bot_extra_wads) {
			if (listWad.equalsIgnoreCase(wad)) {
				cfg_data.bot_extra_wads.remove(wad);
				channel.sendMessage("Removed " + wad + " from the startup list!");
				sendLogModeratorMessage(bold(userInfo(sender)) + " removes " + bold(wad) + " from the startup WAD list");
				return;
			}
		}
		channel.sendMessage("Error: file " + wad + " was not found in the startup list!");
	}

	/**
	 * This function goes through the linkedlist of servers and removes servers
	 * @param server Server - the server object
	 */
	public void removeServerFromLinkedList(Server server) {
		logMessage(LOGLEVEL_DEBUG, "Removing server from linked list.");
		if (servers == null || servers.isEmpty())
			return;
		ListIterator<Server> it = servers.listIterator();
		while (it.hasNext()) {
			// Check if they refer to the exact same object via reference, if so then we want to remove that
			if (it.next() == server) {
				it.remove();
				return;
			}
		}
	}

	/**
	 * Returns a Server from the linked list based on the port number provided
	 * @param port The port to check
	 * @return The server object reference if it exists, null if there's no such object
	 */
	public Server getServer(int port) {
		logMessage(LOGLEVEL_TRIVIAL, "Getting server at port " + port + ".");
		if (servers == null || servers.isEmpty())
			return null;
		ListIterator<Server> it = servers.listIterator();
		Server desiredServer;
		while (it.hasNext()) {
			desiredServer = it.next();
			if (desiredServer.port == port)
				return desiredServer;
		}
		return null;
	}

	/**
	 * Returns a list of servers belonging to the specified user
	 * @param userId their IRC username
	 * @return a list of server objects
	 */
	@Nonnull
	public List<Server> getUserServers(String userId) {
		logMessage(LOGLEVEL_DEBUG, "Getting all servers from " + userId + ".");
		if (servers == null || servers.isEmpty())
			return Collections.emptyList();
		Server desiredServer;
		ListIterator<Server> it = servers.listIterator();
		List<Server> serverList = new ArrayList<>();
		while (it.hasNext()) {
			desiredServer = it.next();
			if (desiredServer.userId.equals(userId)) {
				serverList.add(desiredServer);
			}
		}
		return serverList;
	}
	
	@Nullable
	public List<Server> getAllServers() {
		if (servers == null || servers.isEmpty())
			return null;
		Server desiredServer;
		ListIterator<Server> it = servers.listIterator();
		List<Server> serverList = new ArrayList<>();
		while (it.hasNext()) {
			desiredServer = it.next();
			serverList.add(desiredServer);
		}
		return serverList;
	}

	/**
	 * This searches through the linkedlist to kill the server on that port,
	 * the method does not actually kill it, but signals a boolean to terminate
	 * which the thread that is running it will handle the termination itself and
	 * removal from the linkedlist.
	 * @param portString The port desired to kill
	 */
	private void killServer(String portString, Member sender, MessageChannel channel) {
		logMessage(LOGLEVEL_NORMAL, "Killing server on port " + portString + ".");
		// Ensure it is a valid port
		if (!Functions.isNumeric(portString)) {
			channel.sendMessage("Invalid port number (" + portString + "), not terminating server.");
			return;
		}

		// Since our port is numeric, parse it
		int port = Integer.parseInt(portString);

		// Handle users sending in a small value (thus saving time
		if (port < min_port) {
			channel.sendMessage("Invalid port number (ports start at " + min_port + "), not terminating server.");
			return;
		}

		// See if the port is in our linked list, if so signify for it to die
		Server targetServer = getServer(port);
		if (targetServer != null) {
			targetServer.auto_restart = false;
			String owner = targetServer.sender;
			sendLogModeratorMessage(bold(userInfo(sender)) + " requests kill of " + bold(owner) + "'s server on port "+ bold(portString));
			targetServer.killServer();
		}
		else
			channel.sendMessage("Error: Could not find a server with the port " + port + "!");
	}

	private static String userInfo(@Nullable Member sender) {
		if (sender == null) {
			return "<no member>";
		} else {
			return sender.nick() + "#" + sender.id();
		}
	}

	/**
	 * Toggles the auto-restart feature on or off
	 * @param keywords String[] - array of words in message sent
	 */
/*
	private void toggleAutoRestart(String[] keywords, String channel) {
		if (keywords.length == 2) {
			if (Functions.isNumeric(keywords[1])) {
				Server s = getServer(Integer.parseInt(keywords[1]));
				if (s.auto_restart) {
					s.auto_restart = false;
					channel.sendMessage("Autorestart disabled on server.");
				}
				else {
					s.auto_restart = true;
					channel.sendMessage("Autorestart set up on server.");
				}
			}
		}
		else
			channel.sendMessage("Correct usage is .autorestart <port>");
	}
*/
	/**
	 * Toggles the protected server state on or off (protected servers are immune to killinactive)
	 * @param keywords String[] - array of words in message sent
	 * @param channel
	 */
	private void protectServer(String[] keywords, MessageChannel channel) {
		if (keywords.length == 2) {
			if (Functions.isNumeric(keywords[1])) {
				Server s = getServer(Integer.parseInt(keywords[1]));
				if (s.protected_server) {
					s.protected_server = false;
					channel.sendMessage("Kill protection disabled.");
				}
				else {
					s.protected_server = true;
					channel.sendMessage("Kill protection enabled.");
				}
			}
		}
		else
			channel.sendMessage("Correct usage is .protect <port>");
	}

	/**
	 * Sends a message to all servers
	 * @param keywords String[] - array of words in message sent
	 * @param sender
	 */
	private void globalBroadcast(String[] keywords, Member sender, MessageChannel channel) {
		if (keywords.length > 1) {
			if (servers != null) {
				String message = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
				for (Server s : servers) {
					s.in.flush(); s.in.println("say \"\\cf--------------\\cc\";\n");
					s.in.flush(); s.in.println("say \"GLOBAL ANNOUNCEMENT: " + Functions.escapeQuotes(message) + "\";\n");
					s.in.flush(); s.in.println("say \"\\cf--------------\\cc\";\n");
				}
				channel.sendMessage("Global broadcast sent.");
				sendLogModeratorMessage(bold(userInfo(sender)) + " sends global announcement: " + message);
			}
			else {
				channel.sendMessage("There are no servers running at the moment.");
			}
		}
	}

	/**
	 * Sends a command to specified server
	 * @param recipient String - who to return the message to (since this can be accessed via PM as well as channel)
	 * @param level int - the user's level
	 * @param keywords String[] - message
	 * @param hostname
	 */
	private void sendCommand(AccountType level, String[] keywords, Member hostname, MessageChannel channel) {
		if (keywords.length > 2) {
			if (Functions.isNumeric(keywords[1])) {
				int port = Integer.parseInt(keywords[1]);
				Server s = getServer(port);
				if (s != null) {
					boolean isHoster = s.userId.equals(hostname.id());
					if (isHoster || isAccountTypeOf(level, MODERATOR)) {
						String entireMessage = Functions.implode(Arrays.copyOfRange(keywords, 2, keywords.length), " ");
						String thisMessage = entireMessage.split(";")[0]; // Only send the first message because stacked messages break somehow.. :/
						thisMessage = thisMessage.replaceAll("^\\s+","");
						String[] thisKeywords = thisMessage.split(" ");
						String command = thisKeywords[0];
						String args = Functions.implode(Arrays.copyOfRange(thisKeywords, 1, thisKeywords.length), " ");
						if (command.equalsIgnoreCase("sv_hostname")) {
							args = args.replace("$brand",cfg_data.bot_hostname_base);
						}
						if (!args.equals("") && (command.equalsIgnoreCase("sv_hostname") || command.equalsIgnoreCase("echo") || command.equalsIgnoreCase("say"))) {
							args = Functions.escapeQuotes(args);
							args = "\""+args+"\"";
						}
						{
							s.in.flush(); s.in.println("echo \"-> " + command + " " + Functions.escapeQuotes(args) + " (RCON by " + hostname + ")\";\n");
							s.in.flush(); s.in.println(command + " " + args + ";\n");
						}
						channel.sendMessage("Command '"+thisMessage+"' sent.");
						String logSender = (isHoster ? "their own" : bold(s.userId) + "'s");
						String logStr = bold(userInfo(hostname)) + " sends to " + logSender + " server on port " + port + ": " + thisMessage;
						if (isHoster)
							sendLogUserMessage(logStr);
						else
							sendLogModeratorMessage(logStr);
					}
					else
						channel.sendMessage("You do not own this server.");
				}
				else
					channel.sendMessage("Server does not exist.");
			}
			else
				channel.sendMessage("Port must be a number!");
		}
		else
			channel.sendMessage("Incorrect syntax! Correct syntax is .send <port> <command>");
	}
	
	/**
	 * Sends a command to all servers
	 * @param keywords String[] - array of words in message sent
	 * @param sender
	 */
	private void sendCommandAll(String[] keywords, Member sender, MessageChannel channel) {
		if (keywords.length > 1) {
			if (servers != null) {
				String entireMessage = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
				String thisMessage = entireMessage.split(";")[0]; // Only send the first message because stacked messages break somehow.. :/
				thisMessage = thisMessage.replaceAll("^\\s+","");
				String[] thisKeywords = thisMessage.split(" ");
				String command = thisKeywords[0];
				String args = Functions.implode(Arrays.copyOfRange(thisKeywords, 1, thisKeywords.length), " ");
				if (!args.equals("") && (command.equalsIgnoreCase("sv_hostname") || command.equalsIgnoreCase("sv_website") || command.equalsIgnoreCase("logfile"))) {
					channel.sendMessage("Error: Command " + command + " is not allowed to be sent to all servers");
				}
				else {
					if (!args.equals("") && (command.equalsIgnoreCase("sv_hostname") || command.equalsIgnoreCase("echo") || command.equalsIgnoreCase("say"))) {
						args = Functions.escapeQuotes(args);
						args = "\""+args+"\"";
					}
					for (Server s : servers) {
						s.in.flush(); s.in.println("echo \"-> " + command + " " + Functions.escapeQuotes(args) + " (RCON by " + sender + ")\";\n");
						s.in.flush(); s.in.println(command + " " + args + ";\n");
					}
					channel.sendMessage("Command '"+thisMessage+"' sent to all servers.");
					sendLogModeratorMessage(bold(userInfo(sender)) + " sends to all servers: " + thisMessage);
				}
			}
			else {
				channel.sendMessage("There are no servers running at the moment.");
			}
		}
	}

	/**
	 * Have the bot handle message events
	 */
	public void onMessage(Message msg) {
		final Member member = msg.member();
		final String message = msg.content();

		final MessageChannel channel = msg.channel().blockingGet();
		assert channel != null;

		if (channel.isDM()) {
			onPrivateMessage(msg);
			return;
		}

		// Perform these only if the message starts with a period (to save processing time on trivial chat)
		if (message.startsWith(".")) {
			// Generate an array of keywords from the message
			String[] keywords = message.split(" ");

			// Perform function based on input (note: login is handled by the MySQL function/class); also mostly in alphabetical order for convenience
			final AccountType userLevel = getRole(member);
			switch (keywords[0].toLowerCase()) {
				case ".commands":
					channel.sendMessage(processCommands(userLevel));
					break;
				case ".file":
					processFile(keywords, channel);
					break;
				case ".get":
					processGet(keywords, channel);
					break;
				case ".help":
					channel.sendMessage(Objects.requireNonNullElse(cfg_data.bot_help, "Error: No help available."));
					break;
				case ".owner":
					processOwner(keywords, channel);
					break;
				case ".servers":
					processServers(keywords, channel);
					break;
				case ".uptime":
					if (keywords.length == 1)
						channel.sendMessage("I have been running for " + Functions.calculateTime(System.currentTimeMillis() - time_started));
					else
						calculateUptime(keywords[1], channel);
					break;
				case ".liststartwads":
					channel.sendMessage("These wads are automatically loaded when a server is started: " + Functions.implode(cfg_data.bot_extra_wads, ", "));
					break;
				case ".versions":
					for (Version v : versionParser.list)
						channel.sendMessage(String.format("%s %s - %s", v.name, (v.isDefault ? "(default)" : ""), v.description));
					break;
				default:
					break;
			}
			if (isAccountTypeOf(userLevel, REGISTERED)) {
				switch (keywords[0].toLowerCase()) {
					case ".getinfo":
						processServerInfo(userLevel, keywords, channel, member);
						break;
					case ".host":
						processHost(userLevel, channel, member.id(), message, getMinPort());
						break;
					case ".kill":
						processKill(userLevel, keywords, member, channel, channel);
						break;
					case ".killmine":
						processKillMine(member, channel);
						break;
					case ".load":
						MySQL.loadSlot(member.id(), keywords, userLevel, channel);
						break;
					case ".save":
						channel.sendMessage("Please update slots at " + cfg_data.website_link + "/account");
//						MySQL.saveSlot(hostname, keywords);
						break;
					case ".slot":
						MySQL.showSlot(member.id(), keywords, channel);
						break;
//					case ".query":
//						handleQuery(keywords);
//						break;
					case ".send":
						sendCommand(userLevel, keywords, member, channel);
						break;
					case ".rcon":
					case ".logfile":
					case ".passwords":
						channel.sendMessage("Please use .getinfo <port> instead");
						break;
					default:
						break;
				}
			} else {
				switch (keywords[0].toLowerCase()) {
					case ".getinfo":
					case ".host":
					case ".kill":
					case ".killmine":
					case ".load":
//					case ".query":
					case ".save":
					case ".slot":
					case ".rcon":
					case ".logfile":
					case ".passwords":
						channel.sendMessage("Sorry, I'm not allowed to speak to you");
						break;
					default:
						break;
				}
			}

			if (isAccountTypeOf(userLevel, VIP)) { // VIP
				switch (keywords[0].toLowerCase()) {
//					case ".autorestart":
//						toggleAutoRestart(keywords, channel);
//						break;
					case ".cpu":
						try {
							URL url;
							if (keywords.length > 1 && keywords[1] != null && !keywords[1].isEmpty()) {
								url = new URL("http://127.0.0.1/bot/cpu?port=" + keywords[1]);
							}
							else {
								url = new URL("http://127.0.0.1/bot/cpu");
							}
							URLConnection yc = url.openConnection();
							BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
							String inputLine;
							while ((inputLine = in.readLine()) != null) {
								channel.sendMessage(inputLine);
							}
							in.close();
						}
						catch (IOException e) {
							System.out.println(e);
						}
						break;
					case ".mem":
					case ".ram":
						try {
							URL url;
							if (keywords.length > 1 && keywords[1] != null && !keywords[1].isEmpty()) {
								url = new URL("http://127.0.0.1/bot/mem?port=" + keywords[1]);
							}
							else {
								url = new URL("http://127.0.0.1/bot/mem");
							}
							URLConnection yc = url.openConnection();
							BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
							String inputLine;
							while ((inputLine = in.readLine()) != null) {
								channel.sendMessage(inputLine);
							}
							in.close();
						}
						catch (IOException e) {
							System.out.println(e);
						}
						break;
					case ".protect":
						protectServer(keywords, channel);
						break;
					default:
						break;
				}
			} else {
				switch (keywords[0].toLowerCase()) {
//					case ".autorestart":
					case ".cpu":
					case ".mem":
					case ".protect":
						channel.sendMessage("Error: You do not have permission to use that command!");
						break;
					default:
						break;
				}
			}

			if (isAccountTypeOf(userLevel, MODERATOR)) { // MODERATOR
				switch (keywords[0].toLowerCase()) {
					case ".broadcast":
						globalBroadcast(keywords, member, channel);
						break;
					case ".killinactive":
						processKillInactive(keywords, member, channel);
						break;
					default:
						break;
				}
			} else {
				switch (keywords[0].toLowerCase()) {
					case ".broadcast":
					case ".killinactive":
						channel.sendMessage("Error: You do not have permission to use that command!");
						break;
					default:
						break;
				}
			}

			if (isAccountTypeOf(userLevel, ADMIN)) {
				switch (keywords[0].toLowerCase()) {
					case ".killall":
						processKillAll(member, channel);
						break;
					case ".killversion":
						processKillVersion(keywords, member, channel);
						break;
					case ".notice":
						setNotice(keywords, userLevel, channel);
						sendLogAdminMessage(bold(userInfo(member)) + " sets notice to " + Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "));
						break;
					case ".off":
						processOff(member, channel);
						break;
					case ".on":
						processOn(member, channel);
						break;
					case ".reloadconfig":
						reloadConfigFile();
						channel.sendMessage("Configuration file has been successfully reloaded.");
						sendLogAdminMessage(bold(userInfo(member)) + " reloaded the config file");
						break;
					case ".reloadversions":
						versionParser.load();
						channel.sendMessage("Versions file has been successfully reloaded.");
						sendLogAdminMessage(bold(userInfo(member)) + " reloaded the zandronum versions");
						break;
					case ".sendall":
						sendCommandAll(keywords, member, channel);
						break;
					case ".debug":
						debugMode = !debugMode;
						channel.sendMessage("Debug mode is now " + (debugMode ? "en" : "dis") + "abled.");
						sendLogAdminMessage(bold(userInfo(member)) + " " + (debugMode ? "en" : "dis") + "abled debug mode");
						break;
					case ".ipintel":
						cfg_data.ipintel_enabled = !cfg_data.ipintel_enabled;
						channel.sendMessage("IPIntel is now " + (cfg_data.ipintel_enabled ? "en" : "dis") + "abled.");
						sendLogAdminMessage(bold(userInfo(member)) + " " + (cfg_data.ipintel_enabled ? "en" : "dis") + "abled IPIntel checking");
						break;
					case ".clearrecovery":
						if (MySQL.clearRecovery()) {
							channel.sendMessage("Recovery cleared");
							sendLogAdminMessage(bold(userInfo(member)) + " cleared the recovery");
						}
						else {
							channel.sendMessage("Failed to clear recovery");
						}
						break;
					case ".updaterecovery":
						int added = 0;
						List<Server> servers = getAllServers();
						if (servers != null && servers.size() > 0)
							for (Server server : servers)
								if (!MySQL.serverInRecovery(server.server_id))
									if (MySQL.addServerToRecovery(server))
										added++;

						if (added != 0) {
							channel.sendMessage("Added " + added + " servers to recovery");
							sendLogAdminMessage("**" + userInfo(member) + "** updated the recovery (+"+added+" servers)");
						}
						else {
							channel.sendMessage("No servers to add to recovery");
						}
						break;
					default:
						break;
				}
			} else {
				switch (keywords[0].toLowerCase()) {
					case ".debug":
					case ".ipintel":
					case ".killall":
					case ".killversion":
					case ".notice":
					case ".off":
					case ".on":
					case ".reloadconfig":
					case ".reloadversions":
					case ".clearrecovery":
					case ".updaterecovery":
					case ".sendall":
						channel.sendMessage("Error: You do not have permission to use that command!");
						break;
					default:
						break;
				}
			}
		}
	}

	private AccountType getRole(Member member) {
		final Set<Role> roles;
		if (member != null) {
			roles = member.roles();
		} else {
			roles = Collections.emptySet();
		}
		return roles.stream()
				.map(HasName::name)
				.map(AccountType::fromString)
				.max(AccountType::compareTo)
				.orElse(AccountType.NONE);
	}

	/**
	 * Have the bot handle DM events
	 */
	public void onPrivateMessage(Message msg) {
		final User author = msg.author();
		assert author != null;

		final Member member = getGuild()
				.members()
				.findAny(m -> m.user().blockingGet().id().equals(author.id()));
		final String message = msg.content();

		final MessageChannel channel = msg.channel().blockingGet();
		assert channel != null;


		// As of now, you can only perform commands if you are logged in, so we don't need an else here
		// Generate an array of keywords from the message (similar to onMessage)
		String[] keywords = message.split(" ");
		final AccountType userLevel = getRole(member);
		switch (keywords[0].toLowerCase()) {
			case "register":
				if (keywords.length == 2)
					MySQL.registerAccount(member.id(), keywords[1], channel);
				else
					channel.sendMessage("Incorrect syntax! Usage is: register <password>");
				break;
			default:
				break;
		}
		if (isAccountTypeOf(userLevel, REGISTERED)) { // REGISTERED
			switch (keywords[0].toLowerCase()) {
				case ".commands":
					channel.sendMessage(processPrivateCommands(userLevel));
					break;
				case "changepass":
				case "changepassword":
				case "changepw":
					if (keywords.length == 2)
						MySQL.changePassword(member.id(), keywords[1], channel);
					else
						channel.sendMessage("Incorrect syntax! Usage is: /msg " + " changepw <new_password>");
					break;
				case ".getinfo":
					processServerInfo(userLevel, keywords, channel, member);
					break;
				case ".send":
					sendCommand(userLevel, keywords, member, channel);
					break;
				case ".rcon":
				case ".logfile":
				case ".passwords":
					channel.sendMessage("Please use .getinfo <port> instead");
					break;
				default:
					break;
			}
		} else {
			switch (keywords[0].toLowerCase()) {
				case ".changepass":
				case ".changepassword":
				case ".changepw":
				case ".getinfo":
				case ".send":
				case ".rcon":
				case ".logfile":
				case ".passwords":
					channel.sendMessage("Error: You are either not logged in with NickServ or your account is not registered with " + cfg_data.service_short + " - See " + cfg_data.website_link + "/register");
					break;
				default:
					break;
			}
		}

		if (isAccountTypeOf(userLevel, ADMIN)) { // ADMIN
			switch (keywords[0].toLowerCase()) {
				case ".addstartwad":
					if (keywords.length > 1)
						addExtraWad(Functions.implode(Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length), " "), member, channel);
					break;
				case ".delstartwad":
					if (keywords.length > 1)
						deleteExtraWad(Functions.implode(Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length), " "), member, channel);
					break;
				case ".msg":
					messageChannel(keywords, channel);
					sendLogAdminMessage("**" + userInfo(member) + "** sent message: **" + message.substring(message.indexOf(' ')+1));
					break;
				case ".action":
					channel.sendMessage("/me " + Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "));
					sendLogAdminMessage("**" + userInfo(member) + "** sent action: " + bold(message.substring(message.indexOf(' ')+1)));
					break;
				default:
					break;
			}
		}
	}

	private Guild getGuild() {
		return catnip.cache()
				.guild(guildId)
				.blockingGet();
	}

	/**
	 * This displays commands available for the user
	 * @param userLevel The level based on AccountType enumeration
	 */
	private String processCommands(AccountType userLevel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processComamnds().");
		String commands = "Public Commands: ";
		commands += ".commands .file .get .help .liststartwads .owner .servers .uptime .whoami ";
		if (AccountType.isAccountTypeOf(userLevel, REGISTERED))
			commands += "[R] .getinfo .host .kill .killmine .load .slot .versions ";
		if (AccountType.isAccountTypeOf(userLevel, VIP))
			commands += "[V] .cpu .mem .protect ";
		if (AccountType.isAccountTypeOf(userLevel, MODERATOR))
			commands += "[M] .broadcast .killinactive ";
		if (AccountType.isAccountTypeOf(userLevel, ADMIN))
			commands += "[A] .debug .ipintel .killall .killversion .notice .off .on .reloadconfig .reloadversions .sendall .clearrecovery .updaterecovery";
		return commands;
	}
	
	private String processPrivateCommands(AccountType userLevel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processPrivateCommands().");
		String commands = "Private Commands: ";
		if (AccountType.isAccountTypeOf(userLevel, REGISTERED))
			commands += ".changepassword .getinfo .send ";
		if (AccountType.isAccountTypeOf(userLevel, MODERATOR))
			commands += "[M] .reauth .rejoin ";
		if (AccountType.isAccountTypeOf(userLevel, ADMIN))
			commands += "[A] .action .addstartwad .delstartwad .msg .raw .terminate";
		return commands;
	}

	/**
	 * Broadcasts the uptime of a specific server
	 * @param port String - port numero
	 * @param channel
	 */
	public void calculateUptime(String port, MessageChannel channel) {
		if (Functions.isNumeric(port)) {
			int portValue = Integer.valueOf(port);
			Server s = getServer(portValue);
			if (s != null) {
				if (portValue >= min_port && portValue < max_port)
					channel.sendMessage(s.port + " has been running for " + Functions.calculateTime(System.currentTimeMillis() - s.time_started));
				else
					channel.sendMessage("Port must be between " + min_port + " and " + max_port);
			}
			else
				channel.sendMessage("There is no server running on port " + port);
		}
		else
			channel.sendMessage("Port must be a number (ex: .uptime 15000)");
	}

	/**
	 * Sets the notice (global announcement to all servers)
	 * @param keywords String[] - array of words (message)
	 * @param userLevel int - bitmask level
	 * @param channel
	 */
	public void setNotice(String[] keywords, AccountType userLevel, MessageChannel channel) {
		if (keywords.length == 1) {
			channel.sendMessage("Notice is: " + cfg_data.bot_notice);
			return;
		}
		if (isAccountTypeOf(userLevel, ADMIN)) {
			cfg_data.bot_notice = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
			channel.sendMessage("New notice has been set.");
		}
		else
			channel.sendMessage("You do not have permission to set the notice.");
	}

	/**
	 * Sends a message to the channel, from the bot
	 * @param keywords String[] - message, split by spaces
	 * @param channel String - name of the sender
	 */
	private void messageChannel(String[] keywords, MessageChannel channel) {
		if (keywords.length < 2 || !channel.isGuild())
			channel.sendMessage("Incorrect syntax! Correct usage is .msg your_message");
		else {
			String message = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");

			final Optional<GuildChannel> targetChannel = channelByName(cfg_data.discord_channel);
			targetChannel.ifPresent(ch -> ch.asMessageChannel().sendMessage(message));
		}
	}

	/**
	 * This checks to see if the file exists in the wad directory (it is lower-cased)
	 * @param keywords The keywords sent (should be a length of two)
	 * @param channel The channel to respond to
	 */
	private void processFile(String[] keywords, MessageChannel channel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processFile().");
		if (keywords.length == 2) {
			File file = new File(cfg_data.bot_wad_directory_path + Functions.cleanInputFile(keywords[1].toLowerCase()));
			if (file.exists())
				channel.sendMessage("File '" + keywords[1].toLowerCase() + "' exists on the server.");
			else
				channel.sendMessage("Not found!");
		} else
			channel.sendMessage("Incorrect syntax, use: .file <filename.wad>");
	}

	/**
	 * Gets a field requested by the user
	 * @param keywords The field the user wants
	 * @param channel
	 */
	private void processGet(String[] keywords, MessageChannel channel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processGet().");
		if (keywords.length != 3) {
			channel.sendMessage("Proper syntax: .get <port> <property>");
			return;
		}
		if (!Functions.isNumeric(keywords[1])) {
			channel.sendMessage("Port is not a valid number");
			return;
		}
		Server tempServer = getServer(Integer.parseInt(keywords[1]));
		if (tempServer == null) {
			channel.sendMessage("There is no server running on this port.");
			return;
		}
		channel.sendMessage(tempServer.getField(keywords[2]));
	}

	/**
	 * Passes the host command off to a static method to create the server
	 * @param userLevel The user's bitmask level
	 * @param channel IRC data associated with the sender
	 * @param hostname IRC data associated with the sender
	 * @param message The entire message to be processed
	 */
	public void processHost(AccountType userLevel, MessageChannel channel, String hostname, String message, int port) {
		logMessage(LOGLEVEL_NORMAL, "Processing the host command for " + hostname + " with the message \"" + message + "\".");
		if (botEnabled || isAccountTypeOf(userLevel, ADMIN)) {
			boolean autoRestart = (message.contains("autorestart=true") || message.contains("autorestart=on"));
			int slots = MySQL.getMaxSlots(hostname);
			int userServers = getUserServers(hostname).size();
			if (slots > userServers)
				Server.handleHostCommand(this, servers, channel, hostname, message, userLevel, autoRestart, port, null, false);
			else
				channel.sendMessage("You have reached your server limit (" + slots + ")");
		}
		else
			channel.sendMessage("The bot is currently disabled from hosting for the time being. Sorry for any inconvenience!");
	}

	/**
	 * Attempts to kill a server based on the port
	 * @param userLevel The user's bitmask level
	 * @param keywords The keywords to be processed
	 * @param hostname hostname from the sender
	 * @param sender
	 * @param channel
	 */
	private void processKill(AccountType userLevel, String[] keywords, Member hostname, MessageChannel sender, MessageChannel channel) {
		logMessage(LOGLEVEL_NORMAL, "Processing kill.");
		// Ensure proper syntax
		if (keywords.length != 2) {
			channel.sendMessage("Proper syntax: .kill <port>");
			return;
		}

		// Safety net
		if (servers == null) {
			channel.sendMessage("Critical error: Linkedlist is null, contact an administrator.");
			return;
		}

		// If server list is empty
		if (servers.isEmpty()) {
			channel.sendMessage("There are currently no servers running!");
			return;
		}

		// Registered can only kill their own servers
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			if (Functions.isNumeric(keywords[1])) {
				Server server = getServer(Integer.parseInt(keywords[1]));
				if (server != null) {
					if (server.userId.equalsIgnoreCase(hostname.id()) || isAccountTypeOf(userLevel, MODERATOR))
						if (server.serverprocess != null) {
							server.being_killed = true;
							if (server.userId.equalsIgnoreCase(hostname.id()))
							{
								server.being_killed_by_owner = true;
							}
							server.auto_restart = false;
							server.serverprocess.terminateServer();
						}
						else {
							channel.sendMessage("Error: Server process is null, contact an administrator.");
						}
					else {
						channel.sendMessage("Error: You do not own this server!");
					}
				}
				else {
					channel.sendMessage("Error: There is no server running on this port.");
				}
			} else {
				channel.sendMessage("Improper port number.");
			}
		// Admins/mods can kill anything
		} else if (isAccountTypeOf(userLevel, MODERATOR)) {
			killServer(keywords[1], hostname, channel); // Can pass string, will process it in the method safely if something goes wrong
		}
	}

	/**
	 * When requested it will kill every server in the linked list
	 * @param sender
	 * @param channel
	 */
	private void processKillAll(Member sender, MessageChannel channel) {
		logMessage(LOGLEVEL_IMPORTANT, "Processing killall.");
		// If we use this.servers instead of a temporary list, it will remove the servers from the list while iterating over them
		// This will throw a concurrent modification exception
		// As a temporary solution, we can create a temporary list that will hold the values of the real list at the time it was called
		List<Server> tempList = new LinkedList<>(servers);
		int serverCount = servers.size();
		if (tempList.size() > 0) {
			for (Server s : tempList) {
				s.hide_stop_message = true;
				s.being_killed = true;
				s.auto_restart = false;
				s.killServer();
			}
			channel.sendMessage(Functions.pluralize("Killed a total of " + serverCount + " server{s}.", serverCount));
			//if (channel != cfg_data.irc_channel)
			//	sendMessage(cfg_data.irc_channel, Functions.pluralize("Killed a total of " + serverCount + " server{s}.", serverCount));
			sendLogAdminMessage(Functions.pluralize(bold(userInfo(sender)) + " Killed a total of " + bold("" + serverCount) + " server{s}.", serverCount));
		} else
			channel.sendMessage("There are no servers running.");
	}

	private void processKillVersion(String[] keywords, Member sender, MessageChannel channel) {
		if (keywords.length != 2) {
			channel.sendMessage("Invalid amount of arguments. Syntax: .killversion <version>");
			return;
		}

		String version = keywords[1];

		if (!vSHashmap.containsKey(version)) {
			channel.sendMessage("Unknown version " + version);
			return;
		}

		List<Server> tempList = new ArrayList<>(vSHashmap.get(version));
		if (tempList.size() < 1) {
			channel.sendMessage("No servers to kill.");
			return;
		}

		int killed = 0;
		for (Server s : tempList) {
			s.hide_stop_message = true;
			s.being_killed = true;
			s.auto_restart = false;
			s.killServer();
			killed++;
		}

		sendLogAdminMessage(bold(userInfo(sender)) + " kills all " + killed + bold(keywords[1]) + " servers");
		channel.sendMessage("Killed a total of " + killed + " servers.");
		//if (channel != cfg_data.irc_channel)
		//	sendMessage(cfg_data.irc_channel, "Killed a total of " + killed + " servers.");
	}
	/**
	 * This will look through the list and kill all the servers that the hostname owns
	 * @param hostname The hostname of the person invoking this command
	 * @param channel
	 */
	private void processKillMine(Member hostname, MessageChannel channel) {
		logMessage(LOGLEVEL_TRIVIAL, "Processing killmine.");
		List<Server> servers = getUserServers(hostname.id());
		if (servers.isEmpty()) {
			channel.sendMessage("There are no servers running.");
		} else {
			ArrayList<String> ports = new ArrayList<>();
			for (Server s : servers) {
				s.auto_restart = false;
				s.being_killed = true;
				s.hide_stop_message = true;
				s.killServer();
				ports.add(String.valueOf(s.port));
			}
			if (!ports.isEmpty()) {
				sendLogUserMessage(Functions.pluralize("%s Killed their %d server{s} (%s)".formatted(bold(userInfo(hostname)), ports.size(), Functions.implode(ports, ", ")), ports.size()));
				channel.sendMessage(Functions.pluralize("Killed your %d server{s} (%s)".formatted(ports.size(), Functions.implode(ports, ", ")), ports.size()));
				//if (channel != cfg_data.irc_channel)
				//	sendMessage(cfg_data.irc_channel, Functions.pluralize(sender + " killed their " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") +")", ports.size()));
			}
			else {
				channel.sendMessage("You do not have any servers running.");
			}
		}
	}

	/**
	 * This will kill inactive servers based on the days specified in the second parameter
	 * @param keywords The field the user wants
	 * @param sender
	 * @param channel
	 */
	private void processKillInactive(String[] keywords, Member sender, MessageChannel channel) {
		logMessage(LOGLEVEL_NORMAL, "Processing a kill of inactive servers.");
		if (keywords.length < 2) {
			channel.sendMessage("Proper syntax: .killinactive <days since> (ex: use .killinactive 3 to kill servers that haven't seen anyone for 3 days)");
			return;
		}
		if (Functions.isNumeric(keywords[1])) {
			ArrayList<String> ports = new ArrayList<>();
			int numOfDays = Integer.parseInt(keywords[1]);
			if (numOfDays > 0) {
				if (servers == null || servers.isEmpty()) {
					channel.sendMessage("No servers to kill.");
					return;
				}
				channel.sendMessage("Killing servers with " + numOfDays + "+ days of inactivity.");
				//if (channel != cfg_data.irc_channel)
				//	sendMessage(cfg_data.irc_channel, "Killing servers with " + numOfDays + "+ days of inactivity.");
				// Temporary list to avoid concurrent modification exception
				List<Server> tempList = new LinkedList<>(servers);
				for (Server s : tempList) {
					if (System.currentTimeMillis() - s.serverprocess.last_activity > (Server.DAY_MILLISECONDS * numOfDays))
						if (!s.protected_server) {
							s.hide_stop_message = true;
							s.being_killed = true;
							s.auto_restart = false;
							ports.add(String.valueOf(s.port));
							s.serverprocess.terminateServer();
						}
				}
				if (ports.size() == 0) {
					channel.sendMessage("No servers were killed.");
				}
				else {
					sendLogAdminMessage(Functions.pluralize(bold(userInfo(sender)) + " Killed " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") + ")", ports.size()));
					channel.sendMessage(Functions.pluralize("Killed " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") + ")", ports.size()));
					//if (channel != cfg_data.irc_channel)
					//	sendMessage(cfg_data.irc_channel, Functions.pluralize("Killed " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") + ")", ports.size()));
				}
			} else {
				channel.sendMessage("Using zero or less for .killinactive is not allowed.");
			}
		} else {
			channel.sendMessage("Unexpected parameter for method.");
		}
	}

	/**
	 * Admins can turn off hosting with this
	 * @param sender
	 * @param channel
	 */
	private void processOff(Member sender, MessageChannel channel) {
		logMessage(LOGLEVEL_IMPORTANT, "An admin has disabled hosting.");
		if (botEnabled) {
			botEnabled = false;
			sendLogAdminMessage(bold(userInfo(sender)) + " disables hosting");
			channel.sendMessage("Bot disabled.");
			//if (channel != cfg_data.irc_channel)
			//	sendMessage(cfg_data.irc_channel, "Bot disabled.");
		}
	}

	/**
	 * Admins can re-enable hosting with this
	 * @param sender
	 * @param channel
	 */
	private void processOn(Member sender, MessageChannel channel) {
		logMessage(LOGLEVEL_IMPORTANT, "An admin has re-enabled hosting.");
		if (!botEnabled) {
			botEnabled = true;
			sendLogAdminMessage(bold(userInfo(sender)) + " enables hosting");
			channel.sendMessage("Bot enabled.");
			//if (channel != cfg_data.irc_channel)
			//	sendMessage(cfg_data.irc_channel, "Bot enabled.");
		}
	}

	/**
	 * This checks for who owns the server on the specified port
	 * @param keywords The keywords to pass
	 * @param channel
	 */
	private void processOwner(String[] keywords, MessageChannel channel) {
		logMessage(LOGLEVEL_DEBUG, "Processing an owner.");
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					Server s = getServer(Integer.parseInt(keywords[1]));
					if (s != null)
						channel.sendMessage("The owner of port " + keywords[1] + " is: " + s.sender + "[" + s.userId + "].");
					else
						channel.sendMessage("There is no server running on " + keywords[1] + ".");
				} else
					channel.sendMessage("Invalid port number.");
			} else
				channel.sendMessage("Improper syntax, use: .owner <port>");
	}

	/**
	 * Will attempt to query a server and generate a line of text
	 * @param keywords The keywords sent
	 */
	/*
	private void handleQuery(String[] keywords) {
		if (keywords.length == 2) {
			String[] ipFragment = keywords[1].split(":");
			if (ipFragment.length == 2) {
				if (ipFragment[0].length() > 0 && ipFragment[1].length() > 0 && Functions.isNumeric(ipFragment[1])) {
					int port = Integer.parseInt(ipFragment[1]);
					if (port > 0 && port < 65535) {
						sendMessageToChannel("Attempting to query " + keywords[1] + ", please wait...");
						ServerQueryRequest request = new ServerQueryRequest(ipFragment[0], port);
						if (!queryManager.addRequest(request))
							sendMessageToChannel("Too many people requesting queries. Please try again later.");
					} else
						sendMessageToChannel("Port value is not between 0 - 65536 (ends exclusive), please fix your IP:port and try again.");
				} else
					sendMessageToChannel("Missing (or too many) port delimiters, Usage: .query <ip:port>   (example: .query 98.173.12.44:20555)");
			} else
				sendMessageToChannel("Missing (or too many) port delimiters, Usage: .query <ip:port>   (example: .query 98.173.12.44:20555)");
		} else
			sendMessageToChannel("Usage: .query <ip:port>   (example: .query 98.173.12.44:20555)");
	}
	*/
	/**
	 * Handles RCON stuff
	 * @param userLevel int - the user's level (permissions)
	 * @param keywords String[] - message split by spaces
	 * @param channel String - the nickname of the sender
	 * @param hostname String - the hostname of the sender
	 */
	private void processServerInfo(AccountType userLevel, String[] keywords, MessageChannel channel, Member hostname) {
		logMessage(LOGLEVEL_NORMAL, "Processing a request for rcon (from " + channel + ").");
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					int port = Integer.parseInt(keywords[1]);
					Server s = getServer(port);
					if (s != null) {
						boolean isHoster = s.userId.equals(hostname.id());
						if (isHoster || isAccountTypeOf(userLevel, MODERATOR)) {
							String logSender = (isHoster ? "their own" : bold(s.sender) + "'s");
							String logStr = bold(userInfo(hostname)) + " requests server info for " + logSender + " server on port " + port;
							
							channel.sendMessage("Log File: " + cfg_data.static_link + "/logs/" + s.server_id + ".txt");
							channel.sendMessage("RCON Password: " + s.rcon_password);
							channel.sendMessage("Connect Password: " + s.connect_password);
							channel.sendMessage("Join Password: " + s.join_password);
							
							if (isHoster) {
								sendLogUserMessage(logStr);
							} else {
								sendLogModeratorMessage(logStr);
							}
						}
						else
							channel.sendMessage("You do not own this server.");
					}
					else
						channel.sendMessage("Server does not exist.");
				}
				else
					channel.sendMessage("Port must be a number!");
			}
			else
				channel.sendMessage("Incorrect syntax! Correct syntax is .rcon <port>");
		}
	}

	/**
	 * Sends a message to the channel with a list of servers from the user
	 * @param keywords String[] - the message
	 * @param channel
	 */
	private void processServers(String[] keywords, MessageChannel channel) {
		logMessage(LOGLEVEL_NORMAL, "Getting a list of servers.");
		if (keywords.length == 2) {
			List<Server> servers = getUserServers(keywords[1]);
			if (!servers.isEmpty()) {
				for (Server server : servers) {
					channel.sendMessage( server.port + ": \"" + server.servername + ((server.wads != null) ?
					"\" with wads " + Functions.implode(server.wads, ", ") : ""));
				}
			}
			else
				channel.sendMessage("User " + keywords[1] + " has no servers running.");
		}
		else if (keywords.length == 1) {
			channel.sendMessage(Functions.pluralize("There are " + servers.size() + " server{s}.", servers.size()));
		}
		else
			channel.sendMessage("Incorrect syntax! Correct usage is .servers or .servers <username>");
	}

	/**
	 * Allows external objects to send messages to the core channel
	 * @param msg The message to deploy
	 */
	public void sendMessageToCoreChannel(String msg) {
		final Optional<GuildChannel> guildChannel = channelByName(cfg_data.discord_channel);
		guildChannel.ifPresentOrElse(
				ch -> ch.asMessageChannel().sendMessage(msg),
				() -> System.out.println("Can't get core channel")
		);
	}

	/**
	 * Allows external objects to send messages to the log channel
	 * @param msg The message to deploy
	 */
	public void sendMessageToLogChannel(String msg) {
		final Optional<GuildChannel> guildChannel = channelByName(cfg_data.log_channel);
		guildChannel.ifPresentOrElse(
				ch -> ch.asMessageChannel().sendMessage(msg),
				() -> System.out.println("Can't get core channel")
		);
	}

	private Optional<GuildChannel> channelByName(String channel) {
		catnip.fetchGatewayInfo().blockingGet();
		return catnip.cache()
				.channels(this.guildId)
				.stream()
				.filter(ch -> channel.equals(ch.name()))
				.findAny();
	}

	public void sendDebugMessage(String message) {
		sendMessageToLogChannel("DEBUG " + message);
	}

    public void sendLogMessage(String message) {
		if (cfg_data.log_channel != null) {
			final Optional<GuildChannel> guildChannel = channelByName(cfg_data.log_channel);
			guildChannel.ifPresentOrElse(
					ch -> ch.asMessageChannel().sendMessage(message),
					() -> System.out.println("Can't get log channel")
			);
		}
	}
    
    public void sendLogInfoMessage(String message) {
    	sendLogMessage("[" + bold("I") + "]" + " " + message);
    }
    
    public void sendLogErrorMessage(String message) {
    	sendLogMessage("[" + bold("!") + "]" + " " + message);
    }
    
    public void sendLogAdminMessage(String message) {
    	sendLogMessage("[" + bold("A") + "]" + " " + message);
    }
    
    public void sendLogModeratorMessage(String message) {
    	sendLogMessage("[" + bold("M") + "]" + " " + message);
    }
    
    public void sendLogUserMessage(String message) {
    	sendLogMessage("[" + bold("U") + "]" + " " + message);
    }
    
    public void sendLogServerMessage(String message) {
    	sendLogMessage("[" + bold("S") + "]" + " " + message);
    }

	/**
	 * Contains the main methods that are run on start up for the bot
	 * The arguments should contain the path to the Bot.cfg file only
	 */
	public static void main(String[] args) {
		// We need only one argument to the config
		if (args.length != 1) {
			System.out.println("Incorrect arguments, please have only one arg to your ini path");
			return;
		}

		// Attempt to load the config
		ConfigData cfg_data;
		try {
			cfg_data = new ConfigData(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("Warning: ini file has a string where a number should be!");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			System.out.println("Warning: ini file IOException!");
			e.printStackTrace();
			return;
		}

		// Start the bot
		Bot b = new Bot(cfg_data);
		b.config_file = args[0];
	}

	public void sendMessage(MessageChannel channel, String msg) {
		channel.sendMessage(msg);
	}
}
