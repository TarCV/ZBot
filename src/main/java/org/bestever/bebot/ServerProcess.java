// --------------------------------------------------------------------------
// Copyright (C) 2012-2013 Best-Ever
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bestever.bebot.Bot.bold;

/**
 * This class is specifically for running the server only and notifying the
 * bot when the server is closed, or when to be terminated; nothing more
 */
public class ServerProcess extends Thread {

	/**
	 * This contains the strings that will run in the process builder
	 */
	private ArrayList<String> serverRunCommands;

	/**
	 * A reference to the server
	 */
	private Server server;


	/**
	 * The process of the server
	 */
	private Process proc;

	/**
	 * Used in determining when the last activity of the server was in ms
	 */
	public long last_activity;

	/**
	 * Counts how many times the server has automatically restarted
	 */
	public short restarts = 0;

	/**
	 * Config data
	 */
	public ConfigData cfg_data;

	/**
	 * This should be called before starting run
	 * @param serverReference A reference to the server it is connected to (establishing a back/forth relationship to access its data)
	 */
	public ServerProcess(Server serverReference) {
		this.server = serverReference;
		processServerRunCommand();
	}

	/**
	 * Is used to indicate if the ServerProcess was initialized properly
	 * @return True if it was initialized properly, false if something went wrong
	 */
	public boolean isInitialized() {
		return this.server != null && this.serverRunCommands != null && proc != null;
	}

	/**
	 * This method can be invoked to signal the thread to kill itself and the
	 * process. It will also handle removing it from the linked list.
	 */
	public void terminateServer() {
		server.bot.removeServerFromLinkedList(this.server);
		if (getPid() != -1)  {
			try {
				String command = "kill -9 " + getPid();
				server.bot.sendDebugMessage("using " + command);
				Runtime.getRuntime().exec(command);
			} catch (Exception ex) {
				server.bot.sendDebugMessage("using proc.destroy()");
				proc.destroy();
			}
		} else {
			server.bot.sendDebugMessage("pid == -1; using proc.destroy()");
			proc.destroy();
		}
	}

	public long getPid() {
		return proc.pid();
	}

	/**
	 * Parse the server object and run the server based on the configuration
	 */
	private void processServerRunCommand() {

		// Create an arraylist with all our strings
		serverRunCommands = new ArrayList<String>();

		// This must always be first (we may also want a custom binary, so do that here as well)
		serverRunCommands.add(server.version.path);
		
		// Add fake parameters for who hosted the server
		addParameter("-owner", server.userId);

		// Check if we have a temporary port
		// This will try to host the server on the same port as before
		if (server.temp_port != 0)
			addParameter("-port", String.valueOf(server.temp_port));
		else
			addParameter("-port", Integer.toString(server.bot.getMinPort()));

		// Load the global configuration file
		addParameter("+exec", server.bot.cfg_data.bot_cfg_directory_path + "global.cfg");

		if (server.iwad != null)
			addParameter("-iwad", server.bot.cfg_data.bot_iwad_directory_path + server.iwad);

		if (server.enable_skulltag_data) {
			// Add the skulltag_* data files first since they need to be accessed by other wads
			int z = 0;
			for (String stwad : server.version.data.split(" "))
				server.wads.add(z++, stwad);
		}

		// Add the extra wads and clean duplicates
		server.wads.addAll(server.bot.cfg_data.bot_extra_wads);
		server.wads = Functions.removeDuplicateWads(server.wads);

		// Finally, add the wads
		if (server.wads.size() > 0) {
			for (String wad : server.wads) {
				if (Server.isIwad(wad))
					addParameter("-file", server.bot.cfg_data.bot_iwad_directory_path + wad);
				else if (wad != null && !wad.trim().isEmpty())
					addParameter("-file", server.bot.cfg_data.bot_wad_directory_path + wad);
			}
		}

		// Optional WADs
		if (server.optwads.size() > 0) {
			for (String wad : server.optwads) {
				addParameter("-optfile", server.bot.cfg_data.bot_wad_directory_path + wad);
			}
		}
		
		if (server.map != null)
			addParameter("+map", server.map);

		addParameter("+skill", String.valueOf((server.skill != -1) ? server.skill : 3));

		if (server.gamemode != null) {
			if (!server.gamemode.equals("cooperative") || !server.gamemode.equals("survival")) {
				addParameter("+cooperative", " 0");
			}
			addParameter("+" + server.gamemode, " 1");
		}

		if (server.dmflags > 0)
			addParameter("+dmflags", Integer.toString(server.dmflags));

		if (server.dmflags2 > 0)
			addParameter("+dmflags2", Integer.toString(server.dmflags2));

		if (server.zadmflags > 0)
			addParameter("+zadmflags", Integer.toString(server.zadmflags));

		if (server.compatflags > 0)
			addParameter("+compatflags", Integer.toString(server.compatflags));

		if (server.zacompatflags > 0)
			addParameter("+zacompatflags", Integer.toString(server.zacompatflags));

		if (server.instagib)
			addParameter("+instagib", "1");

		if (server.buckshot)
			addParameter("+buckshot", "1");

		if (server.config != null)
			addParameter("+exec", server.bot.cfg_data.bot_cfg_directory_path + server.config);

		if (server.servername != null)
			addParameter("+sv_hostname", server.bot.cfg_data.bot_hostname_base + " " + server.servername);
			
		// Add rcon/file based stuff
		addParameter("+sv_rconpassword", server.rcon_password);
		addParameter("+sv_password", server.server_password);
		addParameter("+sv_joinpassword", server.server_password);
		addParameter("+sv_banfile", server.bot.cfg_data.bot_banlistdir + server.userId + ".txt");
		addParameter("+sv_adminlistfile", server.bot.cfg_data.bot_adminlistdir + server.userId + ".txt");
		addParameter("+sv_banexemptionfile", server.bot.cfg_data.bot_whitelistdir + server.userId + ".txt");

		// Create a custom wadpage for us
		server.wads.addAll(server.optwads);
		server.wads = Functions.removeDuplicateWads(server.wads);
		String key = MySQL.createWadPage(Functions.implode(server.wads, ","));

		// Add the custom page to sv_website to avoid large wad list lookups
		addParameter("+sv_website", server.bot.cfg_data.website_link + "/wadpage?key=" + key);

		addParameter("-host", "");

		// Add the passwords to join and connect separately
		server.join_password = server.server_password;
		server.connect_password = server.server_password;
	}

	/**
	 * Adds a parameter to the server run command araylist
	 * @param parameter String - parameter
	 * @param argument String - argument
	 */
	public void addParameter(String parameter, String argument) {
		serverRunCommands.add(parameter);
		serverRunCommands.add(argument);
	}

	/**
	 * This method should be executed when the data is set up to initialize the
	 * server. It will be bound to this thread. Upon server termination this
	 * thread will also end. <br>
	 * Note that this method takes care of adding it to the linked list, so you
	 * don't have to.
	 */
	@Override
	public void run() {
		server.bot.sendDebugMessage("Attempting to start server.");
//		try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace(); }
		String portNumber = ""; // This will hold the port number
		String ipAddress = "";
		File logFile, banlist, whitelist, adminlist;
		String strLine, dateNow;
		server.time_started = System.currentTimeMillis();
		last_activity = System.currentTimeMillis(); // Last activity should be when we start
		BufferedReader br = null;
		BufferedWriter bw = null;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		try {
			// Ensure we have the files created
			banlist = new File(server.bot.cfg_data.bot_banlistdir, server.userId + ".txt");
			if (!banlist.exists())
				createDirectoryAndFile(banlist);
			whitelist = new File(server.bot.cfg_data.bot_whitelistdir, server.userId + ".txt");
			if (!whitelist.exists())
				createDirectoryAndFile(whitelist);
			adminlist = new File(server.bot.cfg_data.bot_adminlistdir, server.userId + ".txt");
			if (!adminlist.exists())
				createDirectoryAndFile(adminlist);

			server.bot.sendDebugMessage("Building process");
			// Set up the server
			ProcessBuilder pb = new ProcessBuilder(serverRunCommands.toArray(new String[0]));
			// Redirect stderr to stdout
			pb.redirectErrorStream(true);
			// Set our working directory
			pb.directory(new File(server.bot.cfg_data.doom_executable_path));
			proc = pb.start();
			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			// Set up the input (with autoflush)
			server.in = new PrintWriter(proc.getOutputStream(), true);

			// Set up file/IO
			logFile = new File(server.bot.cfg_data.bot_logfiledir, server.server_id + ".txt");
			if (!logFile.exists()) {
				createDirectoryAndFile(logFile);
			}
			bw = new BufferedWriter(new FileWriter(new File(server.bot.cfg_data.bot_logfiledir, server.server_id + ".txt"), true));
			

			bw.write("----------------------------------------------------------------\n");
			bw.write(" Server started at " + formatter.format(Calendar.getInstance().getTime()) + "\n");
			bw.write(" Owner: " + server.sender + "\n");
			bw.write(" Service: " + server.bot.cfg_data.service_name + "\n");
			bw.write(" Node: " + server.bot.cfg_data.node_name + "\n");
			bw.write(" Hostline: " + serverRunCommands.toString().replaceAll("^\\[","").replaceAll("\\]$","").replace(", "," ") + "\n");
			bw.write("----------------------------------------------------------------\n");
			bw.flush();
				

			// Check if global RCON variable is set, or if the user has access to the RCON portion
			// If either criteria is met, the user will be messaged the RCON password
			// NOTE: As of now, BE users can still check the RCON password by accessing the control panel on the website.
			// We'll fix this later by changing the RCON from the unique_id to a random MD5 hash
			if (!this.server.recovering) {
				server.bot.sendLogUserMessage(bold(server.sender) + " starts server '" + server.servername + "' with PID " + bold(""+getPid()) + " and UUID " + bold(server.server_id) + " - Log File: " + server.bot.cfg_data.static_link + "/logs/" + server.server_id + ".txt");
			}
			
			// Process server while it outputs text
			while ((strLine = br.readLine()) != null) {
				String[] keywords = strLine.split(" ");
				// Make sure to get the port [Server using alternate port 10666.]
				if (strLine.startsWith("Server using alternate port ")) {
					portNumber = strLine.replace("Server using alternate port ", "").replace(".", "").trim();
					if (Functions.isNumeric(portNumber)) {
						server.port = Integer.parseInt(portNumber);
					} else
						server.bot.sendMessage(server.channel, "Warning: port parsing error when setting up server [1]; contact an administrator.");

				// If the port is used [NETWORK_Construct: Couldn't bind to 10666. Binding to 10667 instead...]
				} else if (strLine.startsWith("NETWORK_Construct: Couldn't bind to ")) {
					portNumber = strLine.replace("NETWORK_Construct: Couldn't bind to " + portNumber + ". Binding to ", "").replace(" instead...", "").trim();
					if (Functions.isNumeric(portNumber)) {
						server.port = Integer.parseInt(portNumber);
					} else
						server.bot.sendMessage(server.channel, "Warning: port parsing error when setting up server [2]; contact an administrator.");
				}
				if (strLine.startsWith("IP address ")) {
					server.address = strLine.replace("IP address ", "").trim();
				}
				// If we see this, the server started
				if (strLine.equalsIgnoreCase("UDP Initialized.")) {
					server.bot.sendDebugMessage("Found \"UDP Initialized.\" in server output. Assuming the server started.");
					try {
						server.bot.servers.add(server);
					} catch (Exception e) {
						if (this.server.recovering)
							System.out.println(server.sender+"'s server '" + server.servername + "' with UUID " + server.server_id + " was unable to be added to the linked list - Killing!");
						else {
							server.bot.sendMessage(server.channel, "Server '" + server.servername + "' was unable to be added to the server list and has been killed. Please contact an Administrator.");
							server.bot.sendLogErrorMessage(bold(server.sender) + "'s server with UUID "+bold(server.server_id)+" was unable to be added to the server list - Killing!");
						}
						e.printStackTrace();
						server.serverprocess.terminateServer();
						server.hide_stop_message = true;
						server.in.close();
						return;
					}
					try {
						server.bot.vSHashmap.get(server.version.name).add(server);
					} catch (Exception e) {
						if (this.server.recovering)
							System.out.println(server.sender+"'s server '" + server.servername + "' with UUID " + server.server_id + " was unable to be added to the versions list");
						else {
							server.bot.sendLogErrorMessage(bold(server.sender) + "'s server with UUID "+bold(server.server_id)+" was unable to be added to the versions list");
						}
						e.printStackTrace();
					}
					if (!this.server.recovering) {
//						if (!MySQL.serverInRecovery(server.server_id))
//							MySQL.addServerToRecovery(server);
						server.bot.sendLogServerMessage(bold(server.sender) + "'s server '" + server.servername + "' has been assigned port " + bold(""+server.port));
						server.bot.sendMessage(server.channel, "Server '" + server.servername + "' started successfully on port " + server.port + "! zds://" + server.address + "/za");
						server.bot.sendMessage(server.channel, "Server '" + server.servername + "' started successfully on port " + server.port + "! - To kill your server, in the channel " + server.bot.cfg_data.irc_channel + ", type .kill " + server.port);
						server.bot.sendMessage(server.channel, "Your unique server ID is: " + server.server_id + ". You can view your logfile at " + server.bot.cfg_data.static_link + "/logs/" + server.server_id + ".txt");
						server.bot.sendMessage(server.channel, "Your RCON password is "+server.rcon_password+". Your server's connect and join passwords are "+server.server_password+" if you have either of those enabled.");
					}
					else {
						System.out.println("Server '" + server.servername + "' with UUID " + server.server_id + " started successfully on port " + server.port + "!");
					}
					this.server.recovering = false;
				}

				// Check for globally banned players
				String name = null;
				String ip = null;
				Boolean skipipintel = false;
				
				// Reading "Player (IP) has connected." (3.0+)
				// This only works in 3.0 as that version prints the IP of who connected within the connect string
				Pattern pattern1 = Pattern.compile("(?:\\n|^)(?!CHAT )(.*?) \\(([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:[0-9]{1,5}\\) has connected\\.$");
				Matcher matcher1 = pattern1.matcher(strLine);
				if (matcher1.find())
				{
					name = matcher1.group(1);
					ip = matcher1.group(2);
				}
				/*
				// Reading "Player has connected." (2.1.2-)
				Pattern pattern2 = Pattern.compile("(?:\\n|^)(?!CHAT )(.*?) has connected\\.$");
				Matcher matcher2 = pattern2.matcher(strLine);
				if (matcher2.find())
				{
					server.in.flush(); server.in.println("getip " + Functions.escapeQuotes(matcher2.group(1).replace(" ","\\ "))+";\n");
				}

				// Reading GetIP Player
				// This method is actually unreliable in vanilla 2.1.2 as multiple people can have the same name :/
				// A custom binary that disallows duplicate names or shows the IP in the "has connected." line like in 3.0
				// Once 3.0 is released as stable, this method will be removed.
				Pattern pattern3 = Pattern.compile("(?:\\n|^)(?!CHAT )(.*?)'s IP is: ([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:[0-9]{1,5}$");
				Matcher matcher3 = pattern3.matcher(strLine);
				if (matcher3.find())
				{
					name = matcher3.group(1);
					ip = matcher3.group(2);
				}
				*/
				// Reading PlayerInfo
				Pattern pattern4 = Pattern.compile("(?:\\n|^)[0-9]{1,2}\\. (.*?) - IP ([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})\\:[0-9]{1,5}");
				Matcher matcher4 = pattern4.matcher(strLine);
				if (matcher4.find())
				{
					name = matcher4.group(1);
					ip = matcher4.group(2);
					skipipintel = true;
				}
				
				// If we have an IP and a name, check if they are behind a proxy or are banned
				if (name != null && ip != null) {
					if (!MySQL.checkWhitelisted(ip)) {
						if (server.bot.cfg_data.ipintel_enabled && !skipipintel)
						{
							IPIntel.query(ip, name, server);
						}
						String decIP = MySQL.checkBanned(ip);
						String reason = MySQL.getBannedReason(decIP);
						if (decIP != null)
						{
							server.bot.sendLogErrorMessage(bold(name)+" with ip "+bold(ip)+" was kicked from " + bold(server.sender) + "'s server on port "+bold(server.port)+" as they're globally banned with reason: " + bold(reason));
							server.in.flush(); server.in.println("addban " + ip + " perm " + "\"\\ciBanned from all " + server.bot.cfg_data.service_short + " servers: " + reason + "\";\n");
						}
					}
				}
				
				
				// Check for Hostname changes
				Pattern pattern5 = Pattern.compile("(?:\\n|^)-> sv_hostname\\s+(.+?)\\s*(?:\\(RCON|$)");
				Matcher matcher5 = pattern5.matcher(strLine);
				if (matcher5.find()) {
					String old = server.servername;
					server.servername = matcher5.group(1).replaceAll("^\"","").replaceAll("\"$","");
					if (!Pattern.compile("(?:\\n|^)\\s*$").matcher(server.servername).find()) {
						if (!old.equals(server.servername)) {
							if (!AccountType.isAccountTypeOf(server.user_level, AccountType.VIP)) {
								server.in.flush();
								server.in.println("sv_hostname \"" + server.bot.cfg_data.bot_hostname_base + " " + Functions.escapeQuotes(server.servername) + "\";\n");
							}
							server.bot.sendMessage(server.channel, "Hostname for '%s' on port %d has been changed to: '%s'".formatted(old, server.port, server.servername));
							server.bot.sendLogServerMessage("Hostname for " + bold(server.sender) + "'s server on port "+bold(server.port)+" has been changed to: "+bold(server.servername));
						}
					}
				}

				// Check for RCON password changes
				Pattern pattern6 = Pattern.compile("(?:\\n|^)-> sv_rconpassword\\s+(.+?)\\s*(?:\\(RCON|$)");
				Matcher matcher6 = pattern6.matcher(strLine);
				if (matcher6.find()) {
					String old = server.rcon_password;
					server.rcon_password = matcher6.group(1);
					if (!Pattern.compile("(?:\\n|^)\\s*$").matcher(server.rcon_password).find()) {
						if (server.rcon_password.length() < 5) {
							server.bot.sendMessage(server.channel, "RCON Password change for '%s' on port %d failed: Must be 5 or more characters!".formatted(server.servername, server.port));
						}
						else if (!old.equals(server.rcon_password)) {
							server.bot.sendMessage(server.channel, "RCON Password for '%s' on port %d has been changed to: '%s'".formatted(server.servername, server.port, server.rcon_password));
						}
					}
				}

				// Check for Join password changes
				Pattern pattern7 = Pattern.compile("(?:\\n|^)-> sv_joinpassword\\s+(.*?)\\s*(?:\\(RCON|$)");
				Matcher matcher7 = pattern7.matcher(strLine);
				if (matcher7.find()) {
					String old = server.join_password;
					server.join_password = matcher7.group(1);
					if (!Pattern.compile("(?:\\n|^)\\s*$").matcher(server.join_password).find()) {
						if (server.join_password.length() < 5) {
							server.bot.sendMessage(server.channel, "Join Password change for '" + server.servername + "' on port " + server.port + " failed: Must be 5 or more characters!");
						}
						else if (!old.equals(server.join_password)) {
							server.bot.sendMessage(server.channel, "Join Password for '" + server.servername + "' on port " + server.port + " has been changed to: '" + server.join_password + "'");
						}
					}
				}

				// Check for Connect password changes
				Pattern pattern8 = Pattern.compile("(?:\\n|^)-> sv_password\\s+(.*?)\\s*(?:\\(RCON|$)");
				Matcher matcher8 = pattern8.matcher(strLine);
				if (matcher8.find()) {
					String old = server.connect_password;
					server.connect_password = matcher8.group(1);
					if (!Pattern.compile("(?:\\n|^)\\s*$").matcher(server.connect_password).find()) {
						if (server.connect_password.length() < 5) {
							server.bot.sendMessage(server.channel, "Connect Password change for '" + server.servername + "' on port " + server.port + " failed: Must be 5 or more characters!");
						}
						else if (!old.equals(server.connect_password)) {
							server.bot.sendMessage(server.channel, "Connect Password for '" + server.servername + "' on port " + server.port + " has been changed to: '" + server.connect_password + "'");
						}
					}
				}

				// If we have a player joining or leaving, mark this server as active
				if (strLine.endsWith("joined the game."))
					last_activity = System.currentTimeMillis();

				dateNow = formatter.format(Calendar.getInstance().getTime());
				bw.write(dateNow + " " + strLine + "\n");
				bw.flush();
			}
			server.bot.sendDebugMessage("Server possibly stopped.");
			// Handle cleanup
			dateNow = formatter.format(Calendar.getInstance().getTime());
			long end = System.currentTimeMillis();
			long uptime = end - server.time_started;
			bw.write(dateNow + " Server stopped! Uptime was " + Functions.calculateTime(uptime));
			server.in.close();

			// Notify the main channel if enabled
			if (!server.hide_stop_message) {
				if (server.port != 0) {
					if (server.being_killed) {
						if (MySQL.serverInRecovery(server.server_id))
							MySQL.removeServerFromRecovery(server.server_id);
						if (server.being_killed_by_owner) {
							server.bot.sendMessage(server.channel, "Server '" + server.servername + "' on port " + server.port + " stopped! Server ran for " + Functions.calculateTime(uptime));
						}
						else {
							server.bot.sendMessage(server.channel, server.sender + "'s server '" + server.servername + "' on port " + server.port + " stopped! Server ran for " + Functions.calculateTime(uptime));
							server.bot.sendMessage(server.channel, "Server '" + server.servername + "' on port " + server.port + " stopped! Server ran for " + Functions.calculateTime(uptime));
						}
						// Don't log as we already log kill commands
					} else {
						server.bot.sendMessage(server.channel, server.sender + "'s server '" + server.servername + "' on port " + server.port + " crashed! Server ran for " + Functions.calculateTime(uptime));
						server.bot.sendMessage(server.channel, "Server '" + server.servername + "' on port " + server.port + " crashed! Server ran for " + Functions.calculateTime(uptime));
						server.bot.sendLogErrorMessage(bold(server.sender)+"'s server on port " + bold(server.port) + " " + bold("CRASHED") + "!!! - A log is available at " + bold(server.bot.cfg_data.static_link + "/logs/" + server.server_id + ".txt"));
					}
				}
				else if (!this.server.recovering) {
					server.bot.sendMessage(server.channel, "Server '" + server.servername + "' was unable to start.");
					server.bot.sendMessage(server.channel, "Server '" + server.servername + "' was unable to start. This is most likely due to a wad error, incorrect load order, missing required wads or requires a later game version. See your log file for more details.");
					server.bot.sendMessage(server.channel, "You can view your logfile at " + server.bot.cfg_data.static_link + "/logs/" + server.server_id + ".txt");
					server.bot.sendLogServerMessage(bold(server.sender)+"'s server '" + server.servername + "' was unable to start. See the log file for more details.");
				}
			}

			// Remove from the Linked List
			server.bot.removeServerFromLinkedList(this.server);
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			server.bot.sendMessage(server.channel, "ERROR");
			server.bot.sendDebugMessage("EXCEPTION - " + e.getMessage());
			for (StackTraceElement element : trace)
				server.bot.sendDebugMessage("TRACE - " + element.toString());
			e.printStackTrace();
			server.bot.removeServerFromLinkedList(this.server);
		} finally {
			try {
				if (bw != null)
					bw.close();
			} catch (Exception e) {
				StackTraceElement[] trace = e.getStackTrace();
				for (StackTraceElement element : trace)
					server.bot.sendDebugMessage("TRACE - " + element.toString());
				e.printStackTrace();
			}
			try {
				if (br != null)
					br.close();
			} catch (Exception e) {
				StackTraceElement[] trace = e.getStackTrace();
				for (StackTraceElement element : trace)
					server.bot.sendDebugMessage("TRACE - " + element.toString());
				e.printStackTrace();
			}
		}
	}

	private static void createDirectoryAndFile(File file) throws IOException {
		final Path path = file.toPath();
		Files.createDirectories(path.getParent());
		Files.createFile(path);
	}
}
