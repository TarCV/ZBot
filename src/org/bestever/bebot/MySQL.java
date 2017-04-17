// --------------------------------------------------------------------------
// Copyright (C) 2012-2013 Best-Ever
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

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.Colors;

import static org.bestever.bebot.Logger.*;

/**
 * MySQL Class for handling all of the database inserts/fetching
 */
public class MySQL {

	/**
	 * Holds the Bot
	 */
	private static Bot bot;

	/**
	 * Holds the MySQL hostname
	 */
	private static String mysql_host;

	/**
	 * Holds the MySQL username
	 */
	private static String mysql_user;

	/**
	 * Holds the MySQL password
	 */
	private static String mysql_pass;

	/**
	 * Holds the MySQL port
	 */
	private static int mysql_port;

	/**
	 * Holds the MySQL database
	 */
	public static String mysql_db;

	/**
	 * A constant in the database to indicate a server is considered online
	 */
	public static final int SERVER_ONLINE = 1;

	/**
	 * Constructor for the MySQL Object
	 * @param bot instance of the bot
	 * @param host MySQL hostname
	 * @param user MySQL username
	 * @param pass MySQL Password
	 * @param port MySQL Port
	 * @param db MySQL Database
	 */
	public static void setMySQL(Bot bot, String host, String user, String pass, int port, String db) {
		MySQL.bot = bot;
		MySQL.mysql_host = host;
		MySQL.mysql_user = user;
		MySQL.mysql_pass = pass;
		MySQL.mysql_port = port;
		MySQL.mysql_db = db;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			logMessage(LOGLEVEL_CRITICAL, "Could not initialize MySQL Driver!");
			System.exit(-1);
		}
	}

	/**
	 * Experimental function that allows for dynamic mysql query execution. Instead of writing
	 * a new method for each query, we will be able to call this method with the statement and
	 * parameters. As per normal preparedStatement procedures, the query will need to include ?
	 * in place of variables. The ? are processed in sequential order.
	 * This method does not support insertions/deletions/alterations, use executeUpdate() for that.
	 * @param query String - the query, with variables replaces with ?
	 * @param arguments Object... - an array of objects, one for each variable (?)
	 * @return ArrayList with a hasmap key => value pair for each row.
	 */
	public static ArrayList executeQuery(String query, Object... arguments) {
		ArrayList<HashMap<String, Object>> rows = new ArrayList<>();
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			// Go through each argument and check what type they are
			// We will then bind the value to the prepared statement
			if (arguments.length > 0) {
				for (int i = 0; i < arguments.length; i++) {
					if (arguments[i] instanceof String) {
						pst.setString(i+1, String.valueOf(arguments[i]));
					}
					else if (arguments[i] instanceof Integer || arguments[i] instanceof Short || arguments[i] instanceof Byte) {
						pst.setInt(i+1, (int) arguments[i]);
					}
				}
			}
			ResultSet r = pst.executeQuery();
			ResultSetMetaData md = r.getMetaData();
			int columns = md.getColumnCount();
			while (r.next()) {
				HashMap row = new HashMap(columns);
				for (int j = 1; j <= columns; j++) {
					// Add each column as the key, and field as the value, to the hashmap
					row.put(md.getColumnName(j), r.getObject(j));
				}
				// Add the hashmap to the arraylist
				rows.add(row);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "There was a MySQL error in executeQuery. Statement: " + query + " Arguments:");
			for (Object argument : arguments) {
				logMessage(LOGLEVEL_IMPORTANT, String.valueOf(argument));
			}
		}
		return rows;
	}

	/**
	 * Returns the connection
	 */
	private static Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:mysql://" + mysql_host + ":"+mysql_port+"/" + mysql_db, mysql_user, mysql_pass);
	}

	/**
	 * Create a custom wadpage for our wads
	 * @param wads String[] - the wads to add
	 */
	public static String createWadPage(String wads) {
		String query = "INSERT INTO `" + mysql_db + "`.`wad_pages` (`key`, `wad_string`) VALUES (?, ?)";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			try {
				String hash = Functions.generateHash();
				pst.setString(1, hash);
				pst.setString(2, wads);
				pst.executeUpdate();
				return hash;
			} catch (NoSuchAlgorithmException e) { }
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not add wad page. (SQL Error)");
		}
		return null;
	}

	/**
	 * Checks any number of wads against the wad blacklist
	 * @param fileName String... - name of the file(s)
	 * @return true if is blacklisted, false if not
	 */
	public static boolean checkHashes(String... fileName) {
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT `wadname`,`md5` FROM `").append(mysql_db).append("`.`wads` WHERE `wadname` IN (");
		int i = 0;
		for (; i < fileName.length; i++) {
			if (i == fileName.length - 1)
				sb.append("?");
			else
				sb.append("?, ");
		}
		sb.append(")");
		String query = sb.toString();
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)){
			for (int j = 1; j <= i; j++) {
				pst.setString(j, fileName[j-1]);
			}
			ResultSet checkHashes = pst.executeQuery();
			Statement stm = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet blacklistedHashes = stm.executeQuery("SELECT `name`,`reason`,`md5` FROM `" + mysql_db + "`.`blacklist`;");
			while (checkHashes.next()) {
				blacklistedHashes.beforeFirst();
				while (blacklistedHashes.next())
					if (blacklistedHashes.getString("md5").equalsIgnoreCase(checkHashes.getString("md5"))) {
						bot.sendMessage(bot.cfg_data.irc_channel, "Wad " + checkHashes.getString("wadname") +
								" matches blacklist " + blacklistedHashes.getString("name") + " with reason: \"" + blacklistedHashes.getString("reason") + "\" (hash: " + blacklistedHashes.getString("md5") + ")");
						return false;
					}
			}
			stm.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not get hashes of file (SQL Error)");
			return false;
		}
		return true;
	}

	/**
	 * Gets a ban reason for the specified IP
	 * @param ip String - IP address
	 * @return String - the ban reason
	 */
	public static String getBannedReason(String ip) throws UnknownHostException {
		String query = "SELECT * FROM `" + mysql_db + "`.`banlist` WHERE `ip` = ?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, ip);
			ResultSet r = pst.executeQuery();
			if (r.next()) {
				if (r.getString("reason") != null) {
					return r.getString("reason");
				}
			}
		}  catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not get ban reason.");
		}
		return "None Specified";
	}

	/**
	 * Checks if an IP address is banned
	 * @param ip String - ip address
	 * @return true/false
	 */
	public static String checkBanned(String ip) throws UnknownHostException {
		String query = "SELECT * FROM `" + mysql_db + "`.`banlist` WHERE (`expire` > ? OR `expire` = 0)";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setLong(1, new java.util.Date().getTime());
			ResultSet r = pst.executeQuery();
			while (r.next()) {
				String decIP = r.getString("ip");
				if (decIP.contains("*")) {
					if (Functions.inRange(ip, decIP)) {
						if (!checkWhitelisted(ip)) {
							return decIP;
						}
					}
				}
				else if (decIP.equals(ip)) {
					if (!checkWhitelisted(ip)) {
						return decIP;
					}
				}
			}
		}  catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not check banlist");
		}
		return null;
	}
	
	public static boolean checkWhitelisted(String ip) throws UnknownHostException {
		String query = "SELECT * FROM `" + mysql_db + "`.`whitelist`";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			ResultSet r = pst.executeQuery();
			while (r.next()) {
				String decIP = r.getString("ip");
				if (decIP.contains("*")) {
					if (Functions.inRange(ip, decIP)) {
						return true;
					}
				}
				else if (decIP.equals(ip)) {
					return true;
				}
			}
		}  catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not check whitelist");
		}
		return false;
	}
	
	public static boolean checkKnownIP(String ip) throws UnknownHostException {
		String query = "SELECT * FROM `" + mysql_db + "`.`checked_ips` WHERE `ip` = ?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, ip);
			ResultSet r = pst.executeQuery();
			if (r.next()) {
				return true;
			}
		}  catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not check known IP list");
		}
		return false;
	}

	/**
	 * Gets the maximum number of servers the user is allowed to host
	 * @param hostname String - the user's hostname
	 * @return server_limit Int - maximum server limit of the user
	 */
	public static int getMaxSlots(String hostname) {
		if (Functions.checkLoggedIn(hostname)) {
			String query = "SELECT `server_limit` FROM " + mysql_db + ".`login` WHERE `username` = ?";
			try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
				pst.setString(1, Functions.getUserName(hostname));
				ResultSet r = pst.executeQuery();
				if (r.next())
					return r.getInt("server_limit");
				else
					return 0;
			} catch (SQLException e) {
				logMessage(LOGLEVEL_IMPORTANT, "SQL_ERROR in 'getMaxSlots()'");
				e.printStackTrace();
			}
		}
		return AccountType.GUEST; // Return 0, which is a guest and means it was not found; also returns this if not logged in
	}

	/**
	 * Queries the database and returns the level of the user
	 * @param hostname of the user
	 * @return level for success, 0 for fail, -1 for non-existent username
	 */
	public static int getLevel(String hostname){
		if (Functions.checkLoggedIn(hostname)) {
			String query = "SELECT `level` FROM " + mysql_db + ".`login` WHERE `username` = ?";
			try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
				pst.setString(1, Functions.getUserName(hostname));
				ResultSet r = pst.executeQuery();
				if (r.next())
					return r.getInt("level");
				else
					return -1;
			} catch (SQLException e) {
				logMessage(LOGLEVEL_IMPORTANT, "SQL_ERROR in 'getLevel()'");
				e.printStackTrace();
			}
		}
		return AccountType.GUEST; // Return 0, which is a guest and means it was not found; also returns this if not logged in
	}

	/**
	 * Inserts an account into the database
	 * (assuming the user is logged in to IRC)
	 * @param hostname hostname of the user
	 * @param password password of the user
	 */
	public static void registerAccount(String hostname, String password, String sender) {
		logMessage(LOGLEVEL_NORMAL, "Handling account registration from " + sender + ".");
		// Query to check if the username already exists
		String checkQuery = "SELECT `username` FROM " + mysql_db + ".`login` WHERE `username` = ?";
		// Query to add entry to database
		String executeQuery = "INSERT INTO " + mysql_db + ".`login` ( `username`, `password`, `level`, `activated`, `server_limit`, `remember_token` ) VALUES ( ?, ?, 1, 1, ?, null )";
		try (Connection con = getConnection(); PreparedStatement cs = con.prepareStatement(checkQuery); PreparedStatement xs = con.prepareStatement(executeQuery)) {
			// Query and check if see if the username exists
			cs.setString(1, Functions.getUserName(hostname));
			ResultSet r = cs.executeQuery();
			// The username already exists!
			if (r.next())
				bot.sendMessage(sender, "Account already exists!");
			else {
				// Prepare, bind & execute
				xs.setString(1, Functions.getUserName(hostname));
				// Hash the PW with BCrypt
				xs.setString(2, BCrypt.hashpw(password, BCrypt.gensalt(14)));
				// Set default server limit
				xs.setInt(3, bot.cfg_data.defaultlimit);
				if (xs.executeUpdate() >= 1) {
					bot.sendMessage(sender, "Account created! Your username is " + Functions.getUserName(hostname) + " and your password is " + password);
					bot.sendLogUserMessage(Colors.BOLD + sender + Colors.BOLD + " just registered with username " + Functions.getUserName(hostname) + "!");
				}
				else {
					bot.sendMessage(sender, "There was an error registering your account.");
					bot.sendLogErrorMessage(Colors.BOLD + sender + Colors.BOLD + " tried to register but there was a SQL error!");
				}
			}
		} catch (SQLException e) {
			logMessage(LOGLEVEL_IMPORTANT, "ERROR: SQL_ERROR in 'registerAccount()'");
			e.printStackTrace();
			bot.sendMessage(sender, "There was an error registering your account.");
		}
	}

	/**
	 * Changes the password of a logged in user
	 * (assuming the user is logged into IRC)
	 * @param hostname the user's hostname
	 * @param password the user's password
	 */
	public static void changePassword(String hostname, String password, String sender) {
		logMessage(LOGLEVEL_NORMAL, "Password change request from " + sender + ".");
		// Query to check if the username already exists
		String checkQuery = "SELECT `username` FROM " + mysql_db + ".`login` WHERE `username` = ?";

		// Query to update password
		String executeQuery = "UPDATE " + mysql_db + ".`login` SET `password` = ? WHERE `username` = ?";
		try (Connection con = getConnection(); PreparedStatement cs = con.prepareStatement(checkQuery); PreparedStatement xs = con.prepareStatement(executeQuery)) {
			// Query and check if see if the username exists
			cs.setString(1, Functions.getUserName(hostname));
			ResultSet r = cs.executeQuery();

			// The username doesn't exist!
			if (!r.next())
				bot.sendMessage(sender, "Username does not exist.");

			else {
				// Prepare, bind & execute
				xs.setString(1, BCrypt.hashpw(password, BCrypt.gensalt(14)));
				xs.setString(2, r.getString("username"));
				if (xs.executeUpdate() >= 1)
					bot.sendMessage(sender, "Successfully changed your password!");
				else
					bot.sendMessage(sender, "There was an error changing your password (executeUpdate error). Try again or contact an administrator with this message.");
			}
		} catch (SQLException e) {
			System.out.println("ERROR: SQL_ERROR in 'changePassword()'");
			logMessage(LOGLEVEL_IMPORTANT, "SQL_ERROR in 'changePassword()'");
			e.printStackTrace();
			bot.sendMessage(sender, "There was an error changing your password account (thrown SQLException). Try again or contact an administrator with this message.");
		}
	}

	/**
	 * Loads server saved with the .save command
	 * @param hostname String - their hostname
	 * @param words String[] - their message
	 * @param level Int - their user level
	 * @param channel String - the channel
	 * @param sender String - sender's name
	 */
	public static void loadSlot(String hostname, String[] words, int level, String channel, String sender) {
		if (words.length == 2) {
			if (Functions.isNumeric(words[1])) {
				int slot = Integer.parseInt(words[1]);
				if (slot > 10 || slot < 1) {
					bot.sendMessage(channel, "Slot must be between 1 and 10.");
					return;
				}
				try (Connection con = getConnection()) {
					String query = "SELECT `serverstring` FROM " + mysql_db + ".`save` WHERE `slot` = ? && `username` = ?";
					PreparedStatement pst = con.prepareStatement(query);
					pst.setInt(1, slot);
					pst.setString(2, Functions.getUserName(hostname));
					ResultSet r = pst.executeQuery();
					if (r.next()) {
						String hostCommand = r.getString("serverstring");
						bot.processHost(level, channel, sender, hostname, hostCommand, false, bot.getMinPort());
					}
					else
						 bot.sendMessage(channel, "You do not have anything saved to that slot!");
				}
				catch (SQLException e) {
					Logger.logMessage(LOGLEVEL_IMPORTANT, "SQL Error in 'loadSlot()'");
					e.printStackTrace();
				}
			}
		}
		else
			bot.sendMessage(channel, "Incorrect syntax! Correct syntax is .load 1 to 10");
	}

	/**
	 * Logs a server to the database
	 * @param servername String - the name of the server
	 * @param unique_id String - the server's unique ID
	 * @param username String - username of server host
	 */
	public static void logServer(String servername, String unique_id, String username) {
		String query = "INSERT INTO `" + mysql_db + "`.`serverlog` (`unique_id`, `servername`, `username`, `date`) VALUES (?, ?, ?, NOW())";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, unique_id);
			pst.setString(2, servername);
			pst.setString(3, username);
			pst.executeUpdate();
			pst.close();
		}
		catch (SQLException e) {
			Logger.logMessage(LOGLEVEL_IMPORTANT, "SQL Exception in logServer()");
			e.printStackTrace();
		}
	}

	/**
	 * Shows a server host string saved with the .save command
	 * @param hostname String - the user's hostname
	 * @param words String[] - array of words of message
	 */
	public static void showSlot(String hostname, String[] words, String channel) {
		if (words.length == 2) {
			if (Functions.isNumeric(words[1])) {
				int slot = Integer.parseInt(words[1]);
				if (slot > 0 && slot < 11) {
					String query = "SELECT `serverstring`,`slot` FROM `" + mysql_db + "`.`save` WHERE `slot` = ? && `username` = ?";
					try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
						pst.setInt(1, slot);
						pst.setString(2, Functions.getUserName(hostname));
						ResultSet rs = pst.executeQuery();
						if (rs.next())
							bot.sendMessage(channel, "In slot " + rs.getString("slot") + ": " + rs.getString("serverstring"));
						else
							bot.sendMessage(channel, "You do not have anything saved to that slot!");
					}
					catch (SQLException e) {
						Logger.logMessage(LOGLEVEL_IMPORTANT, "SQL Error in showSlot()");
						e.printStackTrace();
					}
				}
				else
					bot.sendMessage(channel, "Slot must be between 1 and 10!");
			}
			else
				bot.sendMessage(channel, "Slot must be a number.");
		}
		else
			bot.sendMessage(channel, "Incorrect syntax! Correct usage is .slot <slot>");
	}

	/**
	 * Returns a username based on the hostname stored in the database. This is useful for people with custom hostmasks.
	 * @param hostname String - the user's hostname (or hostmask)
	 * @return String - username
	 */
	public static String getUsername(String hostname) {		
		String query = "SELECT `username` FROM " + mysql_db + ".`hostmasks` WHERE `hostmask` = ?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, hostname);
			ResultSet r = pst.executeQuery();
			if (r.next())
				return r.getString("username");
			else
				return "None";
		}
		catch (SQLException e) {
			Logger.logMessage(LOGLEVEL_IMPORTANT, "SQL Error in getUsername()");
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Adds a ban to the banlist
	 * @param ip String - ip of the person to ban
	 * @param reason String - the reason to show they are banned for
	 */
	public static boolean addBan(String ip, String reason, String sender) {
		String date = String.valueOf(System.currentTimeMillis() / 1000L);
		String query = "INSERT INTO `" + mysql_db + "`.`banlist` (ip, reason, banner, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `reason` = ?, `banner` = ?, `date` = ?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, ip);
			pst.setString(2, reason);
			pst.setString(3, sender);
			pst.setString(4, date);
			pst.setString(5, reason);
			pst.setString(6, sender);
			pst.setString(7, date);
			if (pst.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not add ban to banlist");
		}
		return false;
	}
	
	/**
	 * Adds an IP to the known IPs list so they don't need to get checked by IPIntel multiple times
	 * @param ip String - ip to be added
	 */
	public static boolean addKnownIP(String ip) {
		String query = "INSERT INTO `" + mysql_db + "`.`known_ips` (ip) VALUES (?)";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, ip);
			if (pst.executeUpdate() == 1) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not add ban to known IP list");
		}
		return false;
	}

	public static boolean addServerToRecovery(Server server) { return true;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "INSERT INTO `" + mysql_db + "`.`server_recovery` (`uid`, `hostcmd`, `port`, `owner`, `owner_nick`, `owner_hostname`, `node`) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, server.server_id);
			pst.setString(2, server.host_command);
			pst.setInt(3, 10666);
			pst.setString(4, Functions.getUserName(server.irc_hostname));
			pst.setString(5, server.sender);
			pst.setString(6, server.irc_hostname);
			pst.setString(7, bot.cfg_data.irc_name);
			if (pst.executeUpdate() >= 1) {
				bot.sendDebugMessage("Added server to recovery");
				return true;
			}
			else
				bot.sendLogErrorMessage("Failed to add server to recovery: " + server.server_id);
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not add server to recovery");
		}
		return false;
		*/
	}
	
	public static boolean removeServerFromRecovery(String server_id) { return true;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "DELETE FROM `" + mysql_db + "`.`server_recovery` WHERE `uid`=? AND `node`=?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, server_id);
			pst.setString(2, bot.cfg_data.irc_name);
			if (pst.executeUpdate() >= 1) {
				bot.sendDebugMessage("Removed server from recovery");
				return true;
			}
			else
				bot.sendLogErrorMessage("Failed to remove server from recovery: " + server_id);
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not remove server from recovery");
		}
		return false;
		*/
	}
	
	public static boolean serverInRecovery(String server_id) { return false;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "SELECT * FROM `" + mysql_db + "`.`server_recovery` WHERE `uid`=? AND `node`=?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, server_id);
			pst.setString(2, bot.cfg_data.irc_name);
			ResultSet r = pst.executeQuery();
			if (r.next()) {
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not check if server is in recovery");
		}
		return false;
		*/
	}
	
	public static boolean clearRecovery() { return true;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "DELETE FROM `" + mysql_db + "`.`server_recovery` WHERE `node`=?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, bot.cfg_data.irc_name);
			if (pst.executeUpdate() >= 1) {
				bot.sendDebugMessage("Cleared server recovery");
				return true;
			}
			else
				bot.sendLogErrorMessage("Failed to clear recovery");
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not clear recovery");
		}
		return false;
		*/
	}
	
	public static boolean shouldRecover() { return false;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "SELECT * FROM `" + mysql_db + "`.`server_recovery` WHERE `node`=?";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, bot.cfg_data.irc_name);
			ResultSet set = pst.executeQuery();
			if (set.next()) {
				bot.sendDebugMessage("Found servers to recover");
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Could not recover servers");
		}
		return false;
		*/
	}
	
	public static void doRecovery() { return;
		/* [DA] Recovery is broken, for some reason NPEs happen with no trace to debug. For now, it's disabled.
		String query = "SELECT * FROM `" + mysql_db + "`.`server_recovery` WHERE `node`=? ORDER BY owner ASC";
		try (Connection con = getConnection(); PreparedStatement pst = con.prepareStatement(query)) {
			pst.setString(1, bot.cfg_data.irc_name);
			ResultSet set = pst.executeQuery();
			bot.recovering = true;
			int recovered = 0;
			int failed = 0;
			while (set.next()) {
				String id = set.getString("uid");
				String cmd = set.getString("hostcmd");
				int port = set.getInt("port");
				String owner = set.getString("owner");
				String ownerNick = set.getString("owner_nick");
				String ownerHostname = set.getString("owner_hostname");
				Server s = Server.handleHostCommand(bot, bot.servers, bot.cfg_data.irc_channel, ownerNick, owner, cmd, MySQL.getLevel(owner), false, port, id, true);
				String hostname = cmd;
				Pattern regex = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");
				Matcher m = regex.matcher(cmd);
				while (m.find()) {
					switch (m.group(1).toLowerCase()) {
						case "hostname":
							hostname = m.group(2);
							break;
					}
				}
				if (s != null) {
					recovered++;
				}
				else {
					failed++;
				}
			}
			bot.recovering = false;
			bot.sendMessage(bot.cfg_data.irc_channel, "Recovered " + recovered + " server(s).");
			if (failed > 0) { bot.sendMessage(bot.cfg_data.irc_channel, "Failed to recover " + failed + " server(s)."); }
			return;
		} catch (SQLException e) {
			e.printStackTrace();
			logMessage(LOGLEVEL_IMPORTANT, "Error: Could not recover servers");
		}
		bot.recovering = false;
		return;
		*/
	}
}