// --------------------------------------------------------------------------
// Copyright (C) 2012-2013 Best-Ever & The Sentinel's Playground
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

import static org.bestever.bebot.Logger.*;
import static org.bestever.bebot.AccountType.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.*;

import org.bestever.serverquery.QueryManager;
import org.bestever.serverquery.ServerQueryRequest;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.PircBot;

/**
 * This is where the bot methods are run and handling of channel/PM input are processed
 */
public class Bot extends PircBot {

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

	// Debugging purposes only
	public static Bot staticBot;

	private Boolean debugMode = false;

	public VersionParser versionParser;

	/**
	 * Set the bot up with the constructor
	 */
	public Bot(ConfigData cfgfile) {
		staticBot = this; // DEBUG ONLY

		// Point our config data to what we created back in RunMe.java
		cfg_data = cfgfile;

		// Set up the logger
		Logger.setLogFile(cfg_data.bot_logfile);

		// Parse versions.json
		versionParser = new VersionParser(cfgfile.filepath);

		// Set up the bot and join the channel
		logMessage(LOGLEVEL_IMPORTANT, "Initializing BestBot v" + cfg_data.irc_version);
		setVerbose(cfg_data.bot_verbose);
		setName(cfg_data.irc_name);
		setLogin(cfg_data.irc_user);
		setVersion(cfg_data.irc_version);
		try {
			connect(cfg_data.irc_network, cfg_data.irc_port, cfg_data.irc_pass);
		} catch (IOException | IrcException e) {
			logMessage(LOGLEVEL_CRITICAL, "Exception occurred while connecting to the network, terminating bot!");
			disconnect();
			System.exit(0);
			e.printStackTrace();
		}

		// Set initial ports
		this.min_port = cfg_data.bot_min_port;
		this.max_port = cfg_data.bot_max_port;

		// Set up the notice timer (if set)
		if (cfg_data.bot_notice != null) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new NoticeTimer(this), 1000, cfg_data.bot_notice_interval * 1000);
		}

		// Set up the server arrays
		this.servers = new LinkedList<>();
		this.vSHashmap = new HashMap<String, LinkedList<Server>>();
		for (Version v : versionParser.list)
			this.vSHashmap.put(v.name, new LinkedList<Server>());

		// Set up MySQL
		 MySQL.setMySQL(this, cfg_data.mysql_host, cfg_data.mysql_user, cfg_data.mysql_pass, cfg_data.mysql_port, cfg_data.mysql_db);

		// Get the time the bot was started
		this.time_started = System.currentTimeMillis();

		// Begin a server query thread that will run
		queryManager = new QueryManager(this);
		queryManager.run();
	}

	/**
	 * Called when the bot connects to the irc server
	 */
	public void onConnect() {
		this.joinChannel(cfg_data.irc_channel);
		this.joinChannel(cfg_data.log_channel);
		
		sendLogInfoMessage("Bot started.");
	}

	/**
	 * Attemps to rejoin the channel
	 */
	public void rejoinChannel() {
		this.joinChannel(cfg_data.irc_channel);
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
	 */
	public void addExtraWad(String wad, String sender) {
		if (!Functions.fileExists(cfg_data.bot_wad_directory_path + wad)) {
			sendMessage(sender, "Cannot add " + wad + " as it does not exist.");
			return;
		}
		for (String listWad : cfg_data.bot_extra_wads) {
			if (listWad.equalsIgnoreCase(wad)) {
				sendMessage(sender, "Cannot add " + listWad + " as it already exists in the wad startup list.");
				return;
			}
		}
		cfg_data.bot_extra_wads.add(wad);
		sendMessage(sender, "Added " + wad + " to the wad startup list.");
		sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " adds " + Colors.BOLD + wad + Colors.BOLD + " to the startup WAD list");
	}

	/**
	 * Removes a wad from the wad startup list
	 * @param wad String - name of the wad
	 */
	public void deleteExtraWad(String wad, String sender) {
		for (String listWad : cfg_data.bot_extra_wads) {
			if (listWad.equalsIgnoreCase(wad)) {
				cfg_data.bot_extra_wads.remove(wad);
				sendMessage(sender, "Wad " + wad + " was removed from the wad startup list.");
				sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " removes " + Colors.BOLD + wad + Colors.BOLD + " from the startup WAD list");
				return;
			}
		}
		sendMessage(sender, "Wad " + wad + " was not found in the wad startup list.");
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
	 * @param username their IRC username
	 * @return a list of server objects
	 */
	public List<Server> getUserServers(String username) {
		logMessage(LOGLEVEL_DEBUG, "Getting all servers from " + username + ".");
		if (servers == null || servers.isEmpty())
			return null;
		Server desiredServer;
		ListIterator<Server> it = servers.listIterator();
		List<Server> serverList = new ArrayList<>();
		while (it.hasNext()) {
			desiredServer = it.next();
			if (Functions.getUserName(desiredServer.irc_hostname).equalsIgnoreCase(username)) {
				serverList.add(desiredServer);
			}
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
	private void killServer(String portString, String sender) {
		logMessage(LOGLEVEL_NORMAL, "Killing server on port " + portString + ".");
		// Ensure it is a valid port
		if (!Functions.isNumeric(portString)) {
			sendMessage(cfg_data.irc_channel, "Invalid port number (" + portString + "), not terminating server.");
			return;
		}

		// Since our port is numeric, parse it
		int port = Integer.parseInt(portString);

		// Handle users sending in a small value (thus saving time
		if (port < min_port) {
			sendMessage(cfg_data.irc_channel, "Invalid port number (ports start at " + min_port + "), not terminating server.");
			return;
		}

		// See if the port is in our linked list, if so signify for it to die
		Server targetServer = getServer(port);
		if (targetServer != null) {
			targetServer.auto_restart = false;
			String owner = targetServer.sender;
			sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " requests kill of " + Colors.BOLD + owner + Colors.BOLD + "'s server on port "+ Colors.BOLD + portString + Colors.BOLD);
			targetServer.killServer();
		}
		else
			sendMessage(cfg_data.irc_channel, "Could not find a server with the port " + port + "!");
	}

	/**
	 * Toggles the auto-restart feature on or off
	 * @param level int - the user's level (bitmask)
	 * @param keywords String[] - array of words in message sent
	 */
	private void toggleAutoRestart(int level, String[] keywords) {
		if (isAccountTypeOf(level, MODERATOR)) {
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					Server s = getServer(Integer.parseInt(keywords[1]));
					if (s.auto_restart) {
						s.auto_restart = false;
						sendMessage(cfg_data.irc_channel, "Autorestart disabled on server.");
					}
					else {
						s.auto_restart = true;
						sendMessage(cfg_data.irc_channel, "Autorestart set up on server.");
					}
				}
			}
			else
				sendMessage(cfg_data.irc_channel, "Correct usage is .autorestart <port>");
		}
		else
			sendMessage(cfg_data.irc_channel, "You do not have permission to use this command.");
	}

	/**
	 * Toggles the protected server state on or off (protected servers are immune to killinactive)
	 * @param level int - the user's level (bitmask)
	 * @param keywords String[] - array of words in message sent
	 */
	private void protectServer(int level, String[] keywords) {
		if (isAccountTypeOf(level, MODERATOR)) {
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					Server s = getServer(Integer.parseInt(keywords[1]));
					if (s.protected_server) {
						s.protected_server = false;
						sendMessage(cfg_data.irc_channel, "Kill protection disabled.");
					}
					else {
						s.protected_server = true;
						sendMessage(cfg_data.irc_channel, "Kill protection enabled.");
					}
				}
			}
			else
				sendMessage(cfg_data.irc_channel, "Correct usage is .protect <port>");
		}
		else
			sendMessage(cfg_data.irc_channel, "You do not have permission to use this command.");
	}

	/**
	 * Sends a message to all servers
	 * @param level int - the user's level
	 * @param keywords String[] - array of words in message sent
	 */
	private void globalBroadcast(int level, String[] keywords, String sender) {
		if (isAccountTypeOf(level, MODERATOR)) {
			if (keywords.length > 1) {
				if (servers != null) {
					String message = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
					for (Server s : servers) {
						s.in.println("echo <--> GLOBAL SERVICE ANNOUNCEMENT <-->");
						s.in.println("say \\cf--------------\\cc");
						s.in.println("say GLOBAL ANNOUNCEMENT: " + Functions.escapeQuotes(message) + "");
						s.in.println("say \\cf--------------\\cc;");
						s.in.println("echo <--> GLOBAL SERVICE ANNOUNCEMENT <-->");
					}
					sendMessage(cfg_data.irc_channel, "Global broadcast sent.");
					sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " sends global announcement: " + message);
				}
				else {
					sendMessage(cfg_data.irc_channel, "There are no servers running at the moment.");
				}
			}
		}
		else {
			sendMessage(cfg_data.irc_channel, "You do not have the required privileges to send a broadcast.");
		}
	}

	/**
	 * Sends a command to specified server
	 * @param level int - the user's level
	 * @param keywords String[] - message
	 * @param recipient String - who to return the message to (since this can be accessed via PM as well as channel)
	 */
	private void sendCommand(int level, String[] keywords, String hostname, String recipient, String sender) {
		if (isAccountTypeOf(level, REGISTERED)) {
			if (keywords.length > 2) {
				if (Functions.isNumeric(keywords[1])) {
					int port = Integer.parseInt(keywords[1]);
					String message = Functions.implode(Arrays.copyOfRange(keywords, 2, keywords.length), " ");
					Server s = getServer(port);
					if (s != null) {
						boolean isHoster = Functions.getUserName(s.irc_hostname).equals(Functions.getUserName(hostname));
						if (isHoster || isAccountTypeOf(level, MODERATOR)) {
							s.in.println(message);
							if (keywords[2].equalsIgnoreCase("sv_rconpassword") && keywords.length > 2) {
								s.rcon_password = keywords[3];
							}
							sendMessage(recipient, "Command successfully sent.");
							
							String logSender = (isHoster ? "their own" : Colors.BOLD + Functions.getUserName(s.irc_channel) + Colors.BOLD + "'s");
							String logStr = Colors.BOLD + sender + Colors.BOLD + " sends command to " + logSender + " server on port " + port + ": " + message;
							
							if (isHoster) {
								sendLogUserMessage(logStr);
							} else {
								sendLogAdminMessage(logStr);
							}
						}
						else
							sendMessage(recipient, "You do not own this server.");
					}
					else
						sendMessage(recipient, "Server does not exist.");
				}
				else
					sendMessage(recipient, "Port must be a number!");
			}
			else
				sendMessage(recipient, "Incorrect syntax! Correct syntax is .send <port> <command>");
		}
	}

	/**
	 * Have the bot handle message events
	 */
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		// Perform these only if the message starts with a period (to save processing time on trivial chat)
		if (message.startsWith(".")) {
			// Generate an array of keywords from the message
			String[] keywords = message.split(" ");

			// Use soon!
			// String username = Functions.getUserName(hostname);

			// Support custom hostnames
			if (!Functions.checkLoggedIn(hostname))
				if (!MySQL.getUsername(hostname).equals("None"))
					hostname = MySQL.getUsername(hostname);

			// Perform function based on input (note: login is handled by the MySQL function/class); also mostly in alphabetical order for convenience
			int userLevel = MySQL.getLevel(hostname);
			switch (keywords[0].toLowerCase()) {
				case ".versions":
					for (Version v : versionParser.list)
						sendMessage(cfg_data.irc_channel, String.format("%s %s - %s", v.name, (v.isDefault ? "(default)" : ""), v.description));
					break;
				case ".autorestart":
					toggleAutoRestart(userLevel, keywords);
					break;
				case ".broadcast":
					globalBroadcast(userLevel, keywords, sender);
					break;
				case ".commands":
					sendMessage(cfg_data.irc_channel, "Allowed commands: " + processCommands(userLevel));
					break;
// [DA] Get CPU usage info from top
                case ".cpu":
					sendMessage(cfg_data.irc_channel, String.valueOf(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
					try {
						Runtime r = Runtime.getRuntime();
						Process p = r.exec(new String[]{"/bin/sh", "-c", "cat /proc/stat | awk '/^cpu /{usage=(($2+$4)*100/($2+$4+$5))} END { printf \"Usage: \" } END { printf \"%.2f\",usage } END { printf \"%\" }' &&  uptime | awk -F'[a-z]: ' '{ print \" | Load Average: \" $2 }'"});
						BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							sendMessage(cfg_data.irc_channel, inputLine);
						}
						in.close();
					}
					catch (IOException e) {
						System.out.println(e);
					}
					break;
					// [DA] Get Memory stats too. Both RAM and SWAP will be printed
					case ".mem":
						try {
							Runtime r = Runtime.getRuntime();
							Process p = r.exec(new String[]{"/bin/sh", "-c", "free -m | awk '/^Mem:/{rtotal=$2;rused=$3;rfree=$4+$7} END { print \"RAM: Free: \" rfree \"MB | Used: \" rused \"MB | Total: \" rtotal \"MB\" }' && free -m | awk '/^Swap:/{stotal=$2;sused=$3;sfree=$4} END { print \"SWAP: Free: \" sfree \"MB | Used: \" sused \"MB | Total: \" stotal \"MB\" }'"});
							BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
							String inputLine;
							while ((inputLine = in.readLine()) != null) {
								sendMessage(cfg_data.irc_channel, inputLine);
							}
							in.close();
						}
						catch (IOException e) {
						System.out.println(e);
						}
						break;
				case ".file":
					processFile(keywords, channel);
					break;
				case ".get":
					processGet(userLevel, keywords);
					break;
				case ".help":
					if (cfg_data.bot_help != null)
						sendMessage(cfg_data.irc_channel, cfg_data.bot_help);
					else
						sendMessage(cfg_data.irc_channel, "There is no helpfile.");
					break;
				case ".host":
					processHost(userLevel, channel, sender, hostname, message, false, getMinPort());
					break;
				case ".kill":
					processKill(userLevel, keywords, hostname, sender);
					break;
				case ".killall":
					processKillAll(userLevel);
					break;
				case ".killversion":
					processKillVersion(userLevel, keywords, sender);
					break;
				case ".killmine":
					processKillMine(userLevel, hostname, sender);
					break;
				case ".killinactive":
					processKillInactive(userLevel, keywords);
					break;
				case ".liststartwads":
					sendMessage(cfg_data.irc_channel, "These wads are automatically loaded when a server is started: " + Functions.implode(cfg_data.bot_extra_wads, ", "));
					break;
				case ".load":
					MySQL.loadSlot(hostname, keywords, userLevel, channel, sender);
					break;
				case ".notice":
					setNotice(keywords, userLevel, sender);
					break;
				case ".off":
					processOff(userLevel, sender);
					break;
				case ".on":
					processOn(userLevel, sender);
					break;
				case ".owner":
					processOwner(userLevel, keywords);
					break;
				case ".protect":
					protectServer(userLevel, keywords);
					break;
				case ".query":
					handleQuery(userLevel, keywords);
					break;
				case ".rcon":
					if (isAccountTypeOf(userLevel, REGISTERED))
						sendMessage(cfg_data.irc_channel, "Please PM the bot for the rcon.");
					break;
				case ".reloadconfig":
					if (isAccountTypeOf(userLevel, ADMIN)) {
						reloadConfigFile();
						sendMessage(cfg_data.irc_channel, "Configuration file has been successfully reloaded.");
					}
					break;
				case ".reloadversions":
					if (isAccountTypeOf(userLevel, ADMIN)) {
						versionParser.load();
						sendMessage(cfg_data.irc_channel, "Versions file has been successfully reloaded.");
					}
					break;
				case ".reauth":
					if (isAccountTypeOf(userLevel, MODERATOR)) {
						changeNick(cfg_data.irc_name);
                        sendRawLine("NICKSERV IDENTIFY " + cfg_data.irc_pass);
					}
					break;
				case ".save":
					MySQL.saveSlot(hostname, keywords);
					break;
				case ".send":
					if (isAccountTypeOf(userLevel, ADMIN))
						sendCommand(userLevel, keywords, hostname, cfg_data.irc_channel, sender);
					break;
				case ".servers":
					processServers(keywords);
					break;
				case ".slot":
					MySQL.showSlot(hostname, keywords);
					break;
				case ".uptime":
					if (keywords.length == 1)
						sendMessage(cfg_data.irc_channel, "I have been running for " + Functions.calculateTime(System.currentTimeMillis() - time_started));
					else
						calculateUptime(keywords[1]);
					break;
				case ".whoami":
					sendMessage(cfg_data.irc_channel, getLoggedIn(hostname, userLevel));
					break;
				case ".debug":
					if (isAccountTypeOf(userLevel, MODERATOR)) {
						debugMode = !debugMode;
						sendMessage(cfg_data.irc_channel, "Debug mode is now " + (debugMode ? "en" : "dis") + "abled.");
					}
					break;
				default:
					break;
			}
		}
	}

	/**
	 * Broadcasts the uptime of a specific server
	 * @param port String - port numero
	 */
	public void calculateUptime(String port) {
		if (Functions.isNumeric(port)) {
			int portValue = Integer.valueOf(port);
			Server s = getServer(portValue);
			if (s != null) {
				if (portValue >= this.min_port && portValue < this.max_port)
					sendMessage(cfg_data.irc_channel, s.port + " has been running for " + Functions.calculateTime(System.currentTimeMillis() - s.time_started));
				else
					sendMessage(cfg_data.irc_channel, "Port must be between " + this.min_port + " and " + this.max_port);
			}
			else
				sendMessage(cfg_data.irc_channel, "There is no server running on port " + port);
		}
		else
			sendMessage(cfg_data.irc_channel, "Port must be a number (ex: .uptime 15000)");
	}

	/**
	 * Sets the notice (global announcement to all servers)
	 * @param keywords String[] - array of words (message)
	 * @param userLevel int - bitmask level
	 */
	public void setNotice (String[] keywords, int userLevel, String sender) {
		if (keywords.length == 1) {
			sendMessage(cfg_data.irc_channel, "Notice is: " + cfg_data.bot_notice);
			return;
		}
		if (isAccountTypeOf(userLevel, ADMIN)) {
			cfg_data.bot_notice = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
			sendMessage(cfg_data.irc_channel, "New notice has been set.");
			sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " sets periodic global notice to " + Colors.BOLD + cfg_data.bot_notice + Colors.BOLD);
		}
		else
			sendMessage(cfg_data.irc_channel, "You do not have permission to set the notice.");
	}

	/**
	 * Purges an IP address from all banlists
	 * @param ip String - the target's IP address
	 */
	private void purgeBans(String ip) {
		List<Server> tempList = new LinkedList<>(servers);
		for (Server s : tempList) {
			s.in.println("delban " + ip);
		}
		sendMessage(cfg_data.irc_channel, "Purged " + ip + " from all banlists.");
	}

	/**
	 * Returns the login of the invoking user
	 * @param hostname String - the user's hostname
	 * @param level int - bitmask level of the user
	 * @return whether the user is logged in or not
	 */
	private String getLoggedIn(String hostname, int level) {
		if (isAccountTypeOf(level, REGISTERED))
			return "You are logged in as " + Functions.getUserName(hostname);
		else
			return "You are not logged in or do not have an account with TSPG. Please visit http://www.allfearthesentinel.net/ for instructions on how to register";
	}

	/**
	 * This displays commands available for the user
	 * @param userLevel The level based on AccountType enumeration
	 */
	private String processCommands(int userLevel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processComamnds().");
		if (isAccountTypeOf(userLevel, OPERATOR))
			return ".addban .addstartwad .autorestart .banwad .broadcast .commands .cpu .delban .delstartwad .file .get .help" +
			" .host .kill .killall .killmine .killinactive .liststartwads .load " +
			".notice .off .on .owner .protect .purgebans .query .rcon .reauth .reloadversions .save .send .servers .shell .slot .terminate .unbanwad " +
			".uptime .whoami";
		else if (isAccountTypeOf(userLevel, ADMIN))
			return ".addban .addstartwad .autorestart .banwad .broadcast .commands .cpu .delban .delstartwad .file .get .help" +
					" .host .kill .killall .killmine .killinactive .liststartwads .load " +
					".notice .off .on .owner .protect .purgebans .query .rcon .reauth .reloadversions .save .send .servers .slot .unbanwad .uptime .whoami";
		else if (isAccountTypeOf(userLevel, MODERATOR))
			return ".addban .addstartwad .autorestart .banwad .broadcast .commands .cpu .delban .delstartwad .file .get .help .host" +
					" .kill .killmine .killinactive .liststartwads .load " +
					".notice .owner .protect .purgebans .query .rcon .reauth .save .send .servers .slot .unbanwad .uptime .whoami";
		else if (isAccountTypeOf(userLevel, REGISTERED))
			return ".commands .cpu .file .get .help .host .kill .killmine .load .owner .query .rcon .save .servers .slot .uptime .whoami";
		else
			return "[Not logged in, guests have limited access] .commands .cpu .file .help .servers .uptime .whoami";
	}

	/**
	 * Sends a message to the channel, from the bot
	 * @param keywords String[] - message, split by spaces
	 * @param sender String - name of the sender
	 */
	private void messageChannel(String[] keywords, String sender) {
		if (keywords.length < 2)
			sendMessage(sender, "Incorrect syntax! Correct usage is .msg your_message");
		else {
			String message = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
			sendMessage(cfg_data.irc_channel, message);
			sendLogAdminMessage(Colors.BOLD+sender+Colors.BOLD + " sends " + Colors.BOLD+message+Colors.BOLD + " to " + cfg_data.irc_channel + " via bot");
		}
	}

	/**
	 * This checks to see if the file exists in the wad directory (it is lower-cased)
	 * @param keywords The keywords sent (should be a length of two)
	 * @param channel The channel to respond to
	 */
	private void processFile(String[] keywords, String channel) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processFile().");
		if (keywords.length == 2) {
			File file = new File(cfg_data.bot_wad_directory_path + Functions.cleanInputFile(keywords[1].toLowerCase()));
			if (file.exists())
				sendMessage(channel, "File '" + keywords[1].toLowerCase() + "' exists on the server.");
			else
				sendMessage(channel, "Not found!");
		} else
			sendMessage(channel, "Incorrect syntax, use: .file <filename.wad>");
	}

	/**
	 * Gets a field requested by the user
	 * @param userLevel The user's bitmask level
	 * @param keywords The field the user wants
	 */
	private void processGet(int userLevel, String[] keywords) {
		logMessage(LOGLEVEL_TRIVIAL, "Displaying processGet().");
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			if (keywords.length != 3) {
				sendMessage(cfg_data.irc_channel, "Proper syntax: .get <port> <property>");
				return;
			}
			if (!Functions.isNumeric(keywords[1])) {
				sendMessage(cfg_data.irc_channel, "Port is not a valid number");
				return;
			}
			Server tempServer = getServer(Integer.parseInt(keywords[1]));
			if (tempServer == null) {
				sendMessage(cfg_data.irc_channel, "There is no server running on this port.");
				return;
			}
			sendMessage(cfg_data.irc_channel, tempServer.getField(keywords[2]));
		}
	}

	/**
	 * Passes the host command off to a static method to create the server
	 * @param userLevel The user's bitmask level
	 * @param channel IRC data associated with the sender
	 * @param hostname IRC data associated with the sender
	 * @param message The entire message to be processed
	 */
	public void processHost(int userLevel, String channel, String sender, String hostname, String message, boolean autoRestart, int port) {
		logMessage(LOGLEVEL_NORMAL, "Processing the host command for " + Functions.getUserName(hostname) + " with the message \"" + message + "\".");
		if (botEnabled || isAccountTypeOf(userLevel, ADMIN)) {
			if (isAccountTypeOf(userLevel, REGISTERED)) {
				autoRestart = (message.contains("autorestart=true") || message.contains("autorestart=on"));
				int slots = MySQL.getMaxSlots(hostname);
				int userServers;
				if (getUserServers(Functions.getUserName(hostname)) == null) userServers = 0;
				else userServers = getUserServers(Functions.getUserName(hostname)).size();
				if (slots > userServers)
					Server.handleHostCommand(this, servers, channel, sender, hostname, message, userLevel, autoRestart, port);
				else
					sendMessage(cfg_data.irc_channel, "You have reached your server limit (" + slots + ")");
			}
			else
				sendMessage(cfg_data.irc_channel, "You must register with TSPG and be logged in to IRC to use the bot to host!");
		}
		else
			sendMessage(cfg_data.irc_channel, "The bot is currently disabled from hosting for the time being. Sorry for any inconvenience!");
	}

	/**
	 * Attempts to kill a server based on the port
	 * @param userLevel The user's bitmask level
	 * @param keywords The keywords to be processed
	 * @param hostname hostname from the sender
	 */
	private void processKill(int userLevel, String[] keywords, String hostname, String sender) {
		logMessage(LOGLEVEL_NORMAL, "Processing kill.");
		// Ensure proper syntax
		if (keywords.length != 2) {
			sendMessage(cfg_data.irc_channel, "Proper syntax: .kill <port>");
			return;
		}

		// Safety net
		if (servers == null) {
			sendMessage(cfg_data.irc_channel, "Critical error: Linkedlist is null, contact an administrator.");
			return;
		}

		// If server list is empty
		if (servers.isEmpty()) {
			sendMessage(cfg_data.irc_channel, "There are currently no servers running!");
			return;
		}

		// Registered can only kill their own servers
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			if (Functions.isNumeric(keywords[1])) {
				Server server = getServer(Integer.parseInt(keywords[1]));
				if (server != null) {
					if (Functions.getUserName(server.irc_hostname).equalsIgnoreCase(Functions.getUserName(hostname)) || isAccountTypeOf(userLevel, MODERATOR))
						if (server.serverprocess != null) {
							server.being_killed = true;
							server.auto_restart = false;
							server.serverprocess.terminateServer();
						}
						else
							sendMessage(cfg_data.irc_channel, "Error: Server process is null, contact an administrator.");
					else
						sendMessage(cfg_data.irc_channel, "Error: You do not own this server!");
				}
				else
					sendMessage(cfg_data.irc_channel, "Error: There is no server running on this port.");
			} else
				sendMessage(cfg_data.irc_channel, "Improper port number.");
		// Admins/mods can kill anything
		} else if (isAccountTypeOf(userLevel, MODERATOR)) {
			killServer(keywords[1], sender); // Can pass string, will process it in the method safely if something goes wrong
		}
	}

	/**
	 * When requested it will kill every server in the linked list
	 * @param userLevel The user level of the person requesting
	 */
	private void processKillAll(int userLevel) {
		logMessage(LOGLEVEL_IMPORTANT, "Processing killall.");
		if (isAccountTypeOf(userLevel, ADMIN)) {
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
				sendMessage(cfg_data.irc_channel, Functions.pluralize("Killed a total of " + serverCount + " server{s}.", serverCount));
			} else
				sendMessage(cfg_data.irc_channel, "There are no servers running.");
		}
		else
			sendMessage(cfg_data.irc_channel, "You do not have permission to use this command.");
	}

	private void processKillVersion(int userlevel, String[] keywords, String sender) {
		if (isAccountTypeOf(userlevel, ADMIN)) {
			if (keywords.length != 2) {
				sendMessage(cfg_data.irc_channel, "Invalid amount of arguments. Syntax: .killversion <version>");
				return;
			}

			String version = keywords[1];

			if (!vSHashmap.containsKey(version)) {
				sendMessage(cfg_data.irc_channel, "Unknown version " + version);
				return;
			}

			List<Server> tempList = new LinkedList<Server>(vSHashmap.get(version));
			if (tempList.size() < 1) {
				sendMessage(cfg_data.irc_channel, "No servers to kill.");
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

			String plural = (killed == 1 ? "" : "s");
			sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " kills all " + killed + Colors.BOLD + keywords[1] + Colors.BOLD + " servers");
			sendMessage(cfg_data.irc_channel, "Killed a total of " + killed + " servers.");
		}
	}
	/**
	 * This will look through the list and kill all the servers that the hostname owns
	 * @param userLevel The level of the user
	 * @param hostname The hostname of the person invoking this command
	 */
	private void processKillMine(int userLevel, String hostname, String sender) {
		logMessage(LOGLEVEL_TRIVIAL, "Processing killmine.");
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			List<Server> servers = getUserServers(Functions.getUserName(hostname));
			if (servers != null) {
				ArrayList<String> ports = new ArrayList<>();
				for (Server s : servers) {
					s.auto_restart = false;
					s.being_killed = true;
					s.hide_stop_message = true;
					s.killServer();
					ports.add(String.valueOf(s.port));
				}
				if (ports.size() > 0) {
					sendMessage(cfg_data.irc_channel, Functions.pluralize("Killed " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") +")", ports.size()));
					String plural = (ports.size() == 1 ? "" : "s");
					sendLogUserMessage(Colors.BOLD + sender + Colors.BOLD + " kills their " + ports.size() + " server" + plural);
				}
				else {
					sendMessage(cfg_data.irc_channel, "You do not have any servers running.");
				}
			} else {
				sendMessage(cfg_data.irc_channel, "There are no servers running.");
			}
		}
	}

	/**
	 * This will kill inactive servers based on the days specified in the second parameter
	 * @param userLevel The user's bitmask level
	 * @param keywords The field the user wants
	 */
	private void processKillInactive(int userLevel, String[] keywords) {
		logMessage(LOGLEVEL_NORMAL, "Processing a kill of inactive servers.");
		if (isAccountTypeOf(userLevel, MODERATOR)) {
			if (keywords.length < 2) {
				sendMessage(cfg_data.irc_channel, "Proper syntax: .killinactive <days since> (ex: use .killinactive 3 to kill servers that haven't seen anyone for 3 days)");
				return;
			}
			if (Functions.isNumeric(keywords[1])) {
				ArrayList<String> ports = new ArrayList<>();
				int numOfDays = Integer.parseInt(keywords[1]);
				if (numOfDays > 0) {
					if (servers == null || servers.isEmpty()) {
						sendMessage(cfg_data.irc_channel, "No servers to kill.");
						return;
					}
					sendMessage(cfg_data.irc_channel, "Killing servers with " + numOfDays + "+ days of inactivity.");
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
						sendMessage(cfg_data.irc_channel, "No servers were killed.");
					}
					else {
						sendMessage(cfg_data.irc_channel, Functions.pluralize("Killed " + ports.size() + " server{s} (" + Functions.implode(ports, ", ") + ")", ports.size()));
					}
				} else {
					sendMessage(cfg_data.irc_channel, "Using zero or less for .killinactive is not allowed.");
				}
			} else {
				sendMessage(cfg_data.irc_channel, "Unexpected parameter for method.");
			}
		}
	}

	/**
	 * Admins can turn off hosting with this
	 * @param userLevel The user's bitmask level
	 */
	private void processOff(int userLevel, String sender) {
		logMessage(LOGLEVEL_IMPORTANT, "An admin has disabled hosting.");
		if (botEnabled) {
			if (isAccountTypeOf(userLevel, ADMIN)) {
				botEnabled = false;
				sendMessage(cfg_data.irc_channel, "Bot disabled.");
				sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " disables hosting");
			}
		}
	}

	/**
	 * Admins can re-enable hosting with this
	 * @param userLevel The user's bitmask level
	 */
	private void processOn(int userLevel, String sender) {
		logMessage(LOGLEVEL_IMPORTANT, "An admin has re-enabled hosting.");
		if (!botEnabled) {
			if (isAccountTypeOf(userLevel, ADMIN)) {
				botEnabled = true;
				sendMessage(cfg_data.irc_channel, "Bot enabled.");
				sendLogAdminMessage(Colors.BOLD + sender + Colors.BOLD + " enables hosting");
			}
		}
	}

	/**
	 * This checks for who owns the server on the specified port
	 * @param userLevel The level of the user requesting the data
	 * @param keywords The keywords to pass
	 */
	private void processOwner(int userLevel, String[] keywords) {
		logMessage(LOGLEVEL_DEBUG, "Processing an owner.");
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					Server s = getServer(Integer.parseInt(keywords[1]));
					if (s != null)
						sendMessage(cfg_data.irc_channel, "The owner of port " + keywords[1] + " is: " + s.sender + "[" + Functions.getUserName(s.irc_hostname) + "].");
					else
						sendMessage(cfg_data.irc_channel, "There is no server running on " + keywords[1] + ".");
				} else
					sendMessage(cfg_data.irc_channel, "Invalid port number.");
			} else
				sendMessage(cfg_data.irc_channel, "Improper syntax, use: .owner <port>");
	}

	/**
	 * Will attempt to query a server and generate a line of text
	 * @param userLevel The level of the user
	 * @param keywords The keywords sent
	 */
	private void handleQuery(int userLevel, String[] keywords) {
		if (isAccountTypeOf(userLevel, REGISTERED)) {
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
	}

	/**
	 * Handles RCON stuff
	 * @param userLevel int - the user's level (permissions)
	 * @param keywords String[] - message split by spaces
	 * @param sender String - the nickname of the sender
	 * @param hostname String - the hostname of the sender
	 */
	private void processRcon(int userLevel, String[] keywords, String sender, String hostname) {
		logMessage(LOGLEVEL_NORMAL, "Processing a request for rcon (from " + sender + ").");
		if (isAccountTypeOf(userLevel, REGISTERED)) {
			if (keywords.length == 2) {
				if (Functions.isNumeric(keywords[1])) {
					int port = Integer.parseInt(keywords[1]);
					Server s = getServer(port);
					if (s != null) {
						boolean isHoster = Functions.getUserName(s.irc_hostname).equals(Functions.getUserName(hostname));
						if (isHoster || isAccountTypeOf(userLevel, MODERATOR)) {
							String logSender = (isHoster ? "their own" : Colors.BOLD + s.sender + Colors.BOLD + "'s");
							String logStr = Colors.BOLD + sender + Colors.BOLD + " requests RCON info for " + logSender + " server on port " + port;
							
							sendMessage(sender, "RCON: " + s.rcon_password);
							sendMessage(sender, "ID: " + s.server_id);
							sendMessage(sender, "LOG: http://static.allfearthesentinel.net/logs/" + s.server_id + ".txt");
							
							if (isHoster) {
								sendLogUserMessage(logStr);
							} else {
								sendLogAdminMessage(logStr);
							}
						}
						else
							sendMessage(sender, "You do not own this server.");
					}
					else
						sendMessage(sender, "Server does not exist.");
				}
				else
					sendMessage(sender, "Port must be a number!");
			}
			else
				sendMessage(sender, "Incorrect syntax! Correct syntax is .rcon <port>");
		}
	}

	/**
	 * Sends a message to the channel with a list of servers from the user
	 * @param keywords String[] - the message
	 */
	private void processServers(String[] keywords) {
		logMessage(LOGLEVEL_NORMAL, "Getting a list of servers.");
		if (keywords.length == 2) {
			List<Server> servers = getUserServers(Functions.getUserName(keywords[1]));
			if (servers != null && servers.size() > 0) {
				for (Server server : servers) {
					sendMessage(cfg_data.irc_channel,  server.port + ": " + server.servername + ((server.wads != null) ?
					" with wads " + Functions.implode(server.wads, ", ") : ""));
				}
			}
			else
				sendMessage(cfg_data.irc_channel, "User " + Functions.getUserName(keywords[1]) + " has no servers running.");
		}
		else if (keywords.length == 1) {
			sendMessage(cfg_data.irc_channel, Functions.pluralize("There are " + servers.size() + " server{s}.", servers.size()));
		}
		else
			sendMessage(cfg_data.irc_channel, "Incorrect syntax! Correct usage is .servers or .servers <username>");
	}

	/**
	 * Have the bot handle private message events
	 * @param sender The IRC data of the sender
	 * @param login The IRC data of the sender
	 * @param hostname The IRC data of the sender
	 * @param message The message transmitted
	 */
	public void onPrivateMessage(String sender, String login, String hostname, String message) {

		// Check for custom hostmasks
		if (!Functions.checkLoggedIn(hostname))
			if (!MySQL.getUsername(hostname).equals("None"))
				hostname = MySQL.getUsername(hostname);

		// As of now, you can only perform commands if you are logged in, so we don't need an else here
		if (Functions.checkLoggedIn(hostname)) {
			// Generate an array of keywords from the message (similar to onMessage)
			String[] keywords = message.split(" ");
			int userLevel = MySQL.getLevel(hostname);
			switch (keywords[0].toLowerCase()) {
				case ".addban":
					if (isAccountTypeOf(userLevel, MODERATOR) && keywords.length > 1) {
						MySQL.addBan(message.split(" ")[1], Functions.implode(Arrays.copyOfRange(message.split(" "), 2, message.split(" ").length), " "), sender);
					}
					break;
				case ".addstartwad":
					if (isAccountTypeOf(userLevel, MODERATOR) && keywords.length > 1)
						addExtraWad(Functions.implode(Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length), " "), sender);
					break;
				case ".delstartwad":
					if (isAccountTypeOf(userLevel, MODERATOR) && keywords.length > 1)
						deleteExtraWad(Functions.implode(Arrays.copyOfRange(message.split(" "), 1, message.split(" ").length), " "), sender);
					break;
				case ".rcon":
					processRcon(userLevel, keywords, sender, hostname);
					break;
				case "changepass":
				case "changepassword":
				case "changepw":
					if (keywords.length == 2)
						MySQL.changePassword(hostname, keywords[1], sender);
					else
						sendMessage(sender, "Incorrect syntax! Usage is: /msg " + cfg_data.irc_name + " changepw <new_password>");
					break;
				case ".banwad":
					if (isAccountTypeOf(userLevel, MODERATOR)) {
						MySQL.addWadToBlacklist(Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "), sender);
					}
					break;
				case ".unbanwad":
					if (isAccountTypeOf(userLevel, MODERATOR)) {
						MySQL.removeWadFromBlacklist(Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "), sender);
					}
					break;
				case ".delban":
					if (isAccountTypeOf(userLevel, MODERATOR) && keywords.length > 1)
						MySQL.delBan(message.split(" ")[1], sender);
					break;
				case ".msg":
					if (isAccountTypeOf(userLevel, MODERATOR))
						messageChannel(keywords, sender);
					break;
				case ".action":
					if (isAccountTypeOf(userLevel, MODERATOR))
						this.sendAction(cfg_data.irc_channel, Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "));
					break;
				case ".purgebans":
					if (isAccountTypeOf(userLevel, MODERATOR))
						purgeBans(keywords[1]);
					break;
				case ".raw":
					if (isAccountTypeOf(userLevel, ADMIN)) {
						String line = Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " ");
						sendRawLine(line);
						sendLogAdminMessage(Colors.BOLD+sender+Colors.BOLD + " sends raw line " + Colors.BOLD+line);
					}
				case ".rejoin":
					if (isAccountTypeOf(userLevel, ADMIN))
						rejoinChannel();
					break;
				case "register":
					if (keywords.length == 2)
						MySQL.registerAccount(hostname, keywords[1], sender);
					else
						sendMessage(sender, "Incorrect syntax! Usage is: /msg " + cfg_data.irc_name + " register <password>");
					break;
				case ".send":
					if (isAccountTypeOf(userLevel, MODERATOR))
						sendCommand(userLevel, keywords, hostname, sender, sender);
					break;
				/*
				// Commented out as it's apparently a security risk,
				// even though only people with ssh access should have access to it anyway.
				// That's what OPERATOR's for...
				case ".shell":
					if (isAccountTypeOf(userLevel, OPERATOR)) {
						if (keywords.length == 1) {
							sendMessage(sender, "Usage: .shell <command>");
							sendMessage(sender, "This command can cause delays with the bot if you use a command that takes a while to execute.");
							sendMessage(sender, "Please, use this command wisely.");
						} else {
							StringBuffer stringBuffer = new StringBuffer();
							Process process;
							Boolean gotException = false;

							try {
								process = Runtime.getRuntime().exec(Functions.implode(Arrays.copyOfRange(keywords, 1, keywords.length), " "));
								process.waitFor();

							    BufferedReader reader =
							         new BufferedReader(new InputStreamReader(process.getInputStream()));

							    String line = "";
							    while ((line = reader.readLine())!= null)
							    	stringBuffer.append(line + "\n");
							} catch (Exception e) {
								gotException = true;
								e.printStackTrace();
								sendMessage(sender, "An exception occured while executing the command.");
							}

							if (!gotException) {
								String[] lines = stringBuffer.toString().split("\n");

								sendMessage(sender, "Output is " + lines.length + " lines long.");

								for (int n = 0; n < lines.length; n++) {
									String s = lines[n];
									s.replace("\n", "");
									sendMessage(sender, n + ": " + s);
								}
							}
						}
					} else
						sendMessage(sender, "You are not permitted to use this command.");

					break;
				*/
				case ".terminate":
					if (isAccountTypeOf(userLevel, OPERATOR)) {
						if (System.currentTimeMillis() / 1000 > (terminateTimestamp / 1000) + 5) {
							terminateConfirmationTimes = 0;
							terminateTimestamp = System.currentTimeMillis();
						}

						if (System.currentTimeMillis() / 1000 <= (terminateTimestamp / 1000) + 5) {
							if (terminateConfirmationTimes == 0) {
								sendMessage(sender, "WARNING: This command WILL terminate the bot.");
								sendMessage(sender, "It will to be restarted by someone who has direct");
								sendMessage(sender, "access to the server's terminal.");
								sendMessage(sender, " - If you're REALLY sure you want to kill the bot, type .terminate again -");
								terminateConfirmationTimes += 1;
							}

							else if (terminateConfirmationTimes == 1) {
								sendMessage(sender, "The bot will close NOW!");
								messageChannel(("- - The bot is closing now (terminate command issued by " + sender + ") -").split(" "), sender);
								quitServer("Killed by " + sender);
								System.exit(1);
							}
						}

					} else
						sendMessage(sender, "You are not permitted to use this command.");

					break;
				default:
					break;
			}
		} else {
			sendMessage(sender, "Your account is not logged in properly to the IRC network. Please log in and re-query.");
		}
	}

	/**
	 * Have the bot handle kicks, this is useful for rejoining when kicked
	 * @param channel String - the channel
	 * @param kickerNick String - the name of the kicker
	 * @param kickerLogin String - the login of the kicker
	 * @param kickerHostname String - the hostname of the kicker
	 * @param recipientNick String - the name of the kicked user
	 * @param reason String - the reason for being kicked
	 */
	public void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
		// Rejoin the channel if kicked
		if (recipientNick.equalsIgnoreCase(getNick()) && channel.equalsIgnoreCase(cfg_data.irc_channel)) {
			this.joinChannel(cfg_data.irc_channel);
		}
	}

	/**
	 * Handles channel joins
	 * @param channel String - the name of the channel
	 * @param sender String - the user who joined
	 * @param login String - the login of the user who joined
	 * @param hostname String - the hostname of the user who joined
	 */
	public void onJoin(String channel, String sender, String login, String hostname) {
		// Process joins
	}

	/**
	 * Allows external objects to send messages to the core channel
	 * @param msg The message to deploy
	 */
	public void sendMessageToChannel(String msg) {
		sendMessage(cfg_data.irc_channel, msg);
	}

	public void sendDebugMessage(String message) {
		if (debugMode)
			sendMessage(cfg_data.irc_channel, "DEBUG " + message);
	}

    public void sendLogMessage(String message) {
		if (cfg_data.log_channel != null)
			sendMessage(cfg_data.irc_channel, message);
	}
    
    public void sendLogInfoMessage(String message) {
    	sendLogMessage(Colors.GREEN + "[" + Colors.BOLD + "II" + Colors.BOLD + "]" + Colors.NORMAL + " " + message);
    }
    
    public void sendLogErrorMessage(String message) {
    	sendLogMessage(Colors.RED + "[" + Colors.BOLD + "!!" + Colors.BOLD + "]" + Colors.NORMAL + " " + message);
    }
    
    public void sendLogAdminMessage(String message) {
    	sendLogMessage(Colors.YELLOW + "[" + Colors.BOLD + "AD" + Colors.BOLD + "]" + Colors.NORMAL + " " + message);
    }
    
    public void sendLogUserMessage(String message) {
    	sendLogMessage(Colors.CYAN + "[" + Colors.BOLD + "UU" + Colors.BOLD + "]" + Colors.NORMAL + " " + message);
    }

	/**
	 * Called when the bot is disconnected
	 * Ideally, attempt to reconnect
	 */
	public void onDisconnect() {
		while (!isConnected()) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Logger.logMessage(LOGLEVEL_CRITICAL, "Could not sleep.");
			}
			try {
				reconnect();
			} catch (Exception e) {
				System.out.println("Couldn't reconnect... trying again in 30 seconds.");
				Logger.logMessage(LOGLEVEL_IMPORTANT, "Could not reconnect. Attempting to reconnect in 30 seconds.");
			}
		}
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
}
