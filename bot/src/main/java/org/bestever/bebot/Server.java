// --------------------------------------------------------------------------
// Copyright (C) 2012-2013 Best-Ever
// Copyright (C) 2021 TarCV
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License version 3
// as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//
// --------------------------------------------------------------------------

package org.bestever.bebot;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bestever.bebot.Functions.getWadAbsolutePath;
import static org.bestever.bebot.Logger.LOGLEVEL_CRITICAL;
import static org.bestever.bebot.Logger.logMessage;

public class Server {

	/**
	 * Tempoary boolean to determine if recovering or not. Required for ServerProcess since it's a threaded process.
	 */
	public boolean recovering;

	/**
	 * Holds the temporary port
	 */
	public int temp_port;

	/**
	 * Protected servers cannot be killed by inactivity
	 */
	public boolean protected_server = false;

	/**
	 * If true, servers will not say "server stopped on port..."
	 */
	public boolean hide_stop_message = false;

	/**
	 * Holds the input stream
	 */
	public PrintWriter in;

	/**
	 * Contains whether or not the server should auto-restart when terminated
	 */
	public boolean auto_restart = false;

	/**
	 * Contains the thread of the server process
	 */
	public ServerProcess serverprocess;

	/**
	 * Contains the reference to the bot
	 */
	public MessageReceiver bot;

	/**
	 * Contains the ip:port it is run on
	 */
	public String address;

	/**
	 * Contains the port it is run on
	 */
	public int port;

	/**
	 * The time the server was started
	 */
	public long time_started;

	/**
	 * This is the generated password at the very start for server logs and the password
	 */
	public String server_id;

	/**
	 * This is the generated password at the very start for connect and join if enabled from config
	 */
	public String server_password;
	public String join_password;
	public String connect_password;

	/**
	 * Username of the person who sent the command to start it
	 */
	public String userName;

	/**
	 * This is the host's hostname on irc
	 */
	public String userId;

	/**
	 * Contains the entire ".host" command
	 */
	public Map<String, String> host_command;

	/**
	 * Contains the level of the user
	 */
	public AccountType user_level;

	/**
	 * Contains the hostname used, this will NOT contain " :: [BE] New York "
	 */
	public String servername;

	/**
	 * This is the iwad used
	 */
	public String iwad;

	/**
	 * This sets the map rotation list
	 */
	public final List<String> maps = new ArrayList<>();
	
	/**
	 * Contains the gamemode
	 */
	public OdamexGameModes gameMode = OdamexGameModes.COOP;

	enum OdamexGameModes {
		COOP("+sv_skill","4","+g_lives","0","+g_lives_jointimer","30","+g_rounds","0","+sv_forcerespawn","0","+sv_gametype","0","+sv_nomonsters","0"),
		CTF( "+sv_teamsinplay","2","+sv_timelimit","10","+sv_scorelimit","5","+g_lives","0","+g_rounds","0","+sv_forcerespawn","0","+sv_friendlyfire","0","+sv_gametype","3","+sv_nomonsters","1","+sv_skill","5"),
		DM( "+sv_timelimit","10","+sv_fraglimit","30","+g_lives","0","+g_rounds","0","+sv_forcerespawn","0","+sv_gametype","1","+sv_nomonsters","1","+sv_skill","5"),
		DUEL( "+sv_timelimit","10","+sv_fraglimit","30","+g_lives","0","+g_rounds","0","+g_winnerstays","1","+sv_forcerespawn","1","+sv_forcerespawntime","10","+sv_gametype","1","+sv_maxplayers","2","+sv_nomonsters","1","+sv_skill","5","+sv_warmup","1","+sv_warmup_autostart","0"),
		LMS( "+g_lives","1","+g_winlimit","5","+g_lives_jointimer","0","+g_rounds","1","+sv_forcerespawn","1","+sv_gametype","1","+sv_nomonsters","1","+sv_skill","5"),
		SURVIVAL( "+g_lives","3","+sv_skill","4","+g_lives_jointimer","30","+g_rounds","0","+sv_forcerespawn","1","+sv_gametype","0","+sv_nomonsters","0"),
		TDM( "+sv_timelimit","10","+sv_fraglimit","50","+g_lives","0","+g_rounds","0","+sv_forcerespawn","0","+sv_friendlyfire","0","+sv_gametype","2","+sv_nomonsters","1","+sv_skill","5"),
		TLMS( "+sv_teamsinplay","2","+g_lives","1","+g_winlimit","5","+g_lives_jointimer","0","+g_rounds","1","+sv_forcerespawn","1","+sv_friendlyfire","0","+sv_gametype","2","+sv_nomonsters","1","+sv_skill","5");

		final List<String> commands;

		OdamexGameModes(String... commands) {
			this.commands = Arrays.asList(commands);
		}
	}

	/**
	 * The name of the config file (like rofl.cfg), will contain ".cfg" on the end of the string
	 */
	public String config;

	/**
	 * Contains a list of all the wads used by the server separated by a space
	 */
	public final List<String> wads = new ArrayList<>();

	/**
	 *  Holds the skill of the game
	 */
	public OdamexSkill skill = OdamexSkill.DEFAULT;
	enum OdamexSkill {
		DEFAULT(-1),
		IM_TOO_YOUNG_TO_DIE(1),
		HEY_NOT_TOO_ROUGH(2),
		HURT_ME_PLENTY(3),
		ULTRA_VIOLENCE(4),
		NIGHTMARE(5);

		final int number;

		OdamexSkill(int number) {
			this.number = number;
		}
	}

	/**
	 * Contains flags for the server
	 */
	public final Set<OdamexFlags> addFlags = new HashSet<>();
	public final Set<OdamexFlags> removeFlags = new HashSet<>();

	enum OdamexFlags {
		DMFARSPAWN("dmfarspawn", "sv_dmfarspawn"),
		CALLVOTE_SCORELIMIT("callvote_scorelimit", "sv_callvote_scorelimit"),
		CALLVOTE_FRAGLIMIT("callvote_fraglimit", "sv_callvote_fraglimit"),
		CALLVOTE_RESTART("callvote_restart", "sv_callvote_restart"),
		CALLVOTE_RANDPICKUP("callvote_randpickup", "sv_callvote_randpickup"),
		CALLVOTE_RANDCAPS("callvote_randcaps", "sv_callvote_randcaps"),
		CALLVOTE_RANDMAP("callvote_randmap", "sv_callvote_randmap"),
		CALLVOTE_NEXTMAP("callvote_nextmap", "sv_callvote_nextmap"),
		CALLVOTE_MAP("callvote_map", "sv_callvote_map"),
		CALLVOTE_FORCESTART("callvote_forcestart", "sv_callvote_forcestart"),
		CALLVOTE_FORCESPEC("callvote_forcespec", "sv_callvote_forcespec"),
		CALLVOTE_KICK("callvote_kick", "sv_callvote_kick"),
		CALLVOTE_COINFLIP("callvote_coinflip", "sv_callvote_coinflip"),
		VOTE_SPECVOTE("vote_specvote", "sv_vote_specvote"),
		VOTE_SPECCALL("vote_speccall", "sv_vote_speccall"),
		VOTE_COUNTABS("vote_countabs", "sv_vote_countabs"),
		UNBLOCKPLAYERS("unblockplayers", "sv_unblockplayers"),
		FORCERESPAWN("forcerespawn", "sv_forcerespawn"),
		ALLOWSHOWSPAWNS("allowshowspawns", "sv_allowshowspawns"),
		ALLOWWIDESCREEN("allowwidescreen", "sv_allowwidescreen"),
		ALLOWPWO("allowpwo", "sv_allowpwo"),
		ALLOWREDSCREEN("allowredscreen", "sv_allowredscreen"),
		ALLOWMOVEBOB("allowmovebob", "sv_allowmovebob"),
		SHAREKEYS("sharekeys", "sv_sharekeys"),
		KEEPKEYS("keepkeys", "sv_keepkeys"),
		WEAPONSTAY("weaponstay", "sv_weaponstay"),
		NOMONSTERS("nomonsters", "sv_nomonsters"),
		MONSTERSRESPAWN("monstersrespawn", "sv_monstersrespawn"),
		RESPAWNSUPER("respawnsuper", "sv_respawnsuper"),
		ITEMSRESPAWN("itemsrespawn", "sv_itemsrespawn"),
		INFINITEAMMO("infiniteammo", "sv_infiniteammo"),
		FRAGEXITSWITCH("fragexitswitch", "sv_fragexitswitch"),
		ALLOWTARGETNAMES("allowtargetnames", "sv_allowtargetnames"),
		FREELOOK("freelook", "sv_freelook"),
		FORCEWATER("forcewater", "sv_forcewater"),
		DOUBLEAMMO("doubleammo", "sv_doubleammo"),
		ALLOWJUMP("allowjump", "sv_allowjump"),
		ALLOWEXIT("allowexit", "sv_allowexit"),
		FRIENDLYFIRE("friendlyfire", "sv_friendlyfire");

		public final String argumentName;
		final String odamexName;

		OdamexFlags(String argumentName, String odamexName) {
			this.argumentName = argumentName;
			this.odamexName = odamexName;
		}
	}

	/**
	 * Contains the RCON Password
	 */
	public String rcon_password;

	/**
	 * The Version of Zandronum this server uses.
	 */
	public Version version;
	
	/**
	 * This is so we can tell if we crashed or not
	 */
	public boolean being_killed = false;
	public boolean being_killed_by_owner = false;

	/**
	 * This is the time of a day in milliseconds
	 */
	public static final long DAY_MILLISECONDS = 1000L * 60 * 60 * 24;
	public ServerManager manager;

	/**
	 * Default constructor for building a server
	 */
	private Server() {
		// Purposely empty
	}

	/**
	 * This will take ".host ...", parse it and pass it off safely to anything else
	 * that needs the information to create/run the servers and the mysql database.
	 * In addition, all servers will be passed onto a server queue that will use a
	 * thread which processes them one by one from the queue to prevent two servers
	 * attempting to use the same port at the same time
	 * @param hostname The hostname of the sender
	 * @param bot
	 * @param botReference The reference to the running bot
	 * @param cfg_data
	 * @param versionParser
	 * @param channel The channel it was sent from
	 * @param message The message sent
	 * @param userLevel
	 */
	public static void handleHostCommand(MessageReceiver botReference, ServerManager serverManager, ConfigData cfg_data, VersionParser versionParser, String userName, String userId, Map<String, String> message, AccountType userLevel, boolean autoRestart, int port, String id, boolean recovering) throws InputException {
		Server server = new Server();

		// Reference server to bot
		server.bot = botReference;
		server.manager = serverManager;

		// Check if autoRestart was enabled
		server.auto_restart = autoRestart;

		server.temp_port = port;

		// Input basic values
		server.userId = userId;
		server.userName = userName;
		server.host_command = message;
		server.user_level = userLevel;

		// The bot structure of using the executable has changed, we will set
		// it to default here at the very beginning to the normal exe, but it
		// can be changed later on in the code with a binary=... flag
		server.version = versionParser.defaultVersion;

		for (Map.Entry<String, String> entry : message.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			switch (key) {
				case "autorestart" -> {
					server.auto_restart = handleTrue(value);
				}
				case "version" -> {
					String wanted = value.toLowerCase();
					Version v = versionParser.getVersion(wanted);
					if (v != null) {
						server.version = v;
					} else {
						server.bot.onError("Invalid version.");
						return;
					}
				}
				case "config" -> {
					if (!server.checkConfig(cfg_data.bot_cfg_directory_path + Functions.cleanInputFile(value.toLowerCase()))) {
						throw new InputException("Config file '" + value + "' does not exist.");
					}
					server.config = Functions.cleanInputFile(value.toLowerCase());
				}
				case "gamemode" -> server.gameMode = getGamemode(value);
				case "hostname" -> server.servername = value;
				case "iwad" -> server.iwad = getIwad(Functions.cleanInputFile(value));
				case "maps" -> server.maps.addAll(Arrays.asList(value.split(",")));
				case "mapwad" -> {
					final String path = getWadAbsolutePath(value, cfg_data);
					final DoomFile doomFile;
					try {
						doomFile = new DoomFile(path);
					} catch (IOException e) {
						e.printStackTrace();
						throw new InputException("The passed wad is unknown");
					}

					final List<String> mapsFromWads = doomFile.levelNames;
					server.wads.add(value);
					server.maps.addAll(mapsFromWads);
				}
				case "port" -> {
					if (Functions.checkValidPort(value))
						server.temp_port = Integer.parseInt(value);
					else {
						throw new InputException("You did not input a valid port.");
					}
					if (server.checkPortExists(serverManager, server.temp_port)) {
						throw new InputException("Port " + server.temp_port + " is already in use.");
					}
				}
				case "skill" -> {
					server.skill = handleSkill(value);
				}
				case "wad", "file", "wads", "files" -> {
					List<String> wadArray = addWads(value);
					server.wads.addAll(wadArray);
					if (!MySQL.checkHashes(server.wads.toArray(new String[0])))
						throw new InputException("There were disallowed wads");
				}
				default -> {
					final OdamexFlags flag = getFlag(key);
					if (handleTrue(value)) {
						server.addFlags.add(flag);
					} else {
						server.removeFlags.add(flag);
					}
				}
			}
		}

		// Check if the wads exist
		if (server.wads != null) {
			for (int i = 0; i < server.wads.size(); i++) {
				if (server.wads.get(i).startsWith("iwad:")) {
					String tempWad = server.wads.get(i).split(":")[1];
					if (!Functions.fileExists(cfg_data.bot_iwad_directory_path + tempWad)) {
						throw new InputException("File (iwad) '" + tempWad + "' does not exist!");
					}
					// Replace iwad: since we don't need it
					else
						server.wads.set(i, tempWad);
				}
				else if (!Functions.fileExists(cfg_data.bot_wad_directory_path + server.wads.get(i))) {
					throw new InputException("File '" + server.wads.get(i) + "' does not exist!");
				}
			}
		}

		// Now that we've indexed the string, check to see if we have what we need to start a server
		if (server.iwad == null) {
			throw new InputException("You are missing an iwad, or have specified an incorrect iwad. You can add it by appending: iwad=your_iwad");
		}

		if (server.servername == null) {
			throw new InputException("You are missing the hostname, or your hostname syntax is wrong. You can add it by appending: hostname=\"Your Server Name\"");
		}

		// Check if the global server limit has been reached
		if (!recovering && Functions.getFirstAvailablePort(serverManager.getMinPort(), serverManager.getMaxPort()) == 0) {
			throw new InputException("Global server limit has been reached.");
		}

		// Generate the unique ID
		if (id != null) {
			server.server_id = id;
		} else {
			try {
				server.server_id = Functions.generateHash();
			} catch (NoSuchAlgorithmException e) {
				logMessage(LOGLEVEL_CRITICAL, "Error generating MD5 hash!");
				throw new InputException("Error generating MD5 hash. Please contact an administrator.");
			}
		}
		{
			final int mid = server.server_id.length() / 2;
			server.rcon_password = server.server_id.substring(0, mid);
			server.server_password = server.server_id.substring(mid);
		}

		// Assign and start a new thread
		server.recovering = recovering;
		server.serverprocess = new ServerProcess(server, cfg_data);
		server.serverprocess.start();
		MySQL.logServer(server.servername, server.server_id, server.userId);
	}

	private static OdamexFlags getFlag(String key) throws InputException {
		final String flagName = key.substring(1);
		return Stream.of(OdamexFlags.values())
				.filter(flag -> flag.argumentName.equalsIgnoreCase(flagName))
				.findAny()
				.orElseThrow(() -> new InputException("Unknown flag " + flagName));
	}

	/**
	 * Servers stored in the database should be loaded upon invoking this
	 * function on bot startup
	 * This will automatically (assuming there isn't a MySQL error) begin to get
	 * the servers up and running and fill the objects with the appropriate
	 * information.
	 * @param bot The calling bot reference.
	 */
	/* UNUSED
	public void loadServers(Bot bot, ResultSet rs) {
		// If something goes wrong...
		if (rs == null) {
			logMessage(LOGLEVEL_CRITICAL, "Unable to load servers from MySQL!");
			return;
		}

		// Go through each server and initialize them accordingly
		int database_id = -1;
		try {
			Server server;
			while (rs.next()) {
				// The server should be marked as online, if it's not then skip it
				if (rs.getInt("online") == SERVER_ONLINE) {
					database_id = rs.getInt("id");
					server = new Server(); // Reference a new object each time we run through the servers
					server.bot = bot;
					server.buckshot = (rs.getInt("buckshot") == 1);
					server.compatflags = rs.getInt("compatflags");
					server.zacompatflags = rs.getInt("zacompatflags");
					server.config = rs.getString("config");
					server.dmflags = rs.getInt("dmflags");
					server.dmflags2 = rs.getInt("dmflags2");
					server.zadmflags = rs.getInt("zadmflags");
					server.enable_skulltag_data = (rs.getInt("enable_skulltag_data") == 1);
					server.gamemode = rs.getString("gamemode");
					server.host_command = rs.getString("host_command");
					server.instagib = (rs.getInt("instagib") == 1);
					server.irc_channel = rs.getString("irc_channel");
					server.irc_hostname = rs.getString("irc_hostname");
					server.irc_login = rs.getString("irc_login");
					server.iwad = rs.getString("iwad");
					server.mapwads = rs.getString("mapwads").replace(" ","").split(","); // Check this!
					// server.play_time = 0; // NOT IN THE DATABASE
					server.rcon_password = rs.getString("rcon_password");
					server.sender = rs.getString("username"); // ???
					server.server_id = rs.getString("unique_id"); // ???
					server.servername = rs.getString("servername");
					server.time_started = rs.getLong("time_started");
					server.user_level = 0; // ??? Get from Mysql
					//server.wads = rs.getString("wads").replace(" ","").split(","); // Check this!

					// Handle the server (pass it to the appropriate places before referencing a new object) (server.port and server.serverprocess)
					logMessage(LOGLEVEL_NORMAL, "Successfully processed server id " + database_id + "'s data.");
					server.serverprocess = new ServerProcess(server);
					server.serverprocess.start();
				}
			}
		} catch (SQLException e) {
			logMessage(LOGLEVEL_CRITICAL, "MySQL exception loading servers at " + database_id + "!");
			e.printStackTrace();
		}
	}
	*/
	/**
	 * Checks to see if the configuration file exists
	 * @param config String - config name
	 * @return true/false
	 */
	private boolean checkConfig(String config) {
		File f = new File(config);
		return f.exists();
	}

	/**
	 * Checks if a server exists on the port
	 * @param b Bot - the bot object
	 * @param port int - the port
	 * @return true if taken, false if not
	 */
	private boolean checkPortExists(ServerManager b, int port) {
		if (b.getServer(port) == null)
			return false;
		else
			return true;
	}

	/**
	 * Returns the skill of the game
	 * @param skill String - skill level
	 * @return int - skill level
	 */
	private static OdamexSkill handleSkill(String skill) throws InputException {
		if (!Functions.isInteger(skill) || Integer.parseInt(skill) > 4 || Integer.parseInt(skill) < 0) {
			throw new InputException("Incorrect skill number (should be 0-4)");
		}
		else
			return OdamexSkill.values()[Integer.parseInt(skill)];
	}

	/**
	 * Returns an array of wads from a String
	 * @param wads comma-seperated list of wads
	 * @return array of wads
	 */
	private static List<String> addWads(String wads) {
		return Stream.of(wads.split(","))
				.map(wad -> wad.trim().toLowerCase())
				.filter(wad -> !wad.isEmpty())
				.collect(Collectors.toList());
	}

	/**
	 * Checks to see if a wad is an IWAD
	 * @param wad String - name of the wad
	 * @return True if IWAD / False if not
	 */
	public static boolean isIwad(String wad) {
		switch (wad.toLowerCase()) {
			case "doom2.wad":
			case "doom.wad":
			case "tnt.wad":
			case "heretic.wad":
			case "hexen.wad":
			case "hexdd.wad":
			case "strife1.wad":
			case "doom1.wad":
			case "harmony.wad":
			case "harm1.wad":
			case "hacx.wad":
			case "chex3.wad":
			case "megagame.wad":
			case "freedm.wad":
			case "nerve.wad":
			case "fakeiwad.wad":
			case "rott_tc_full.pk3":
			case "doom_complete.pk3":
			case "action2.wad":
                        case "freedoom1.wad":
                        case "freedoom2.wad":
				return true;
			default:
				return false;
		}
	}

	/**
	 * Checks for the iwad based on the input
	 * @param string The keyword with the iwad (ex: iwad=doom2.wad)
	 * @return A string of the wad (lowercase), or null if there's no supported iwad name
	 */
	private static String getIwad(String string) {
		// Check if in array, and if so return that value
		switch (string.toLowerCase()) {
			case "doom2":
			case "doom2.wad":
				return "doom2.wad";
			case "doom":
			case "doom.wad":
				return "doom.wad";
			case "tnt":
			case "tnt.wad":
				return "tnt.wad";
			case "plutonia":
			case "plutonia.wad":
				return "plutonia.wad";
			case "heretic":
			case "heretic.wad":
				return "heretic.wad";
			case "hexen":
			case "hexen.wad":
				return "hexen.wad";
			case "strife1":
			case "strife1.wad":
				return "strife1.wad";
			case "sharewaredoom":
			case "doom1":
			case "doom1.wad":
				return "doom1.wad";
			case "harmony":
			case "harm1":
			case "harmony.wad":
			case "harm1.wad":
				return "harm1.wad";
			case "hacx":
			case "hacx.wad":
				return "hacx.wad";
			case "chex3":
			case "chex3.wad":
				return "chex3.wad";
			case "megaman":
			case "megagame":
			case "megagame.wad":
				return "megagame.wad";
			case "freedm":
			case "freedm.wad":
				return "freedm.wad";
			case "freedoom":
                        case "freedoom.wad":
                        case "freedoom2":
                        case "freedoom2.wad":
				return "freedoom2.wad";
                        case "freedoom1":
                        case "freedoom1.wad":
				return "freedoom1.wad";
			case "nerve":
			case "nerve.wad":
				return "nerve.wad";
			case "fakeiwad":
			case "fakeiwad.wad":
				return "fakeiwad.wad";
			case "rott":
			case "rotttc":
			case "rotttcfull":
			case "rott_tc":
			case "rott_tc_full":
			case "rott_tc_full.pk3":
				return "rott_tc_full.pk3";
			case "doom_complete":
			case "doom_complete.pk3":
				return "doom_complete.pk3";
			case "action2":
			case "action2.wad":
				return "action2.wad";
		}
		// If there's no match...
		return null;
	}

	/**
	 * Takes input to parse the gamemode
	 * @param string The keyword to check with the = sign (ex: gamemode=...)
	 * @return A string of the gamemode, throw if there was no such gamemode
	 */
	private static OdamexGameModes getGamemode(String string) throws InputException {
		// Find out if the string we're given matches a game mode
		switch (string.toLowerCase())
		{
			case "deathmatch":
			case "dm":
			case "ffa":
				return OdamexGameModes.DM;
			case "ctf":
			case "capturetheflag":
				return OdamexGameModes.CTF;
			case "tdm":
			case "teamdm":
			case "tdeathmatch":
			case "teamdeathmatch":
				return OdamexGameModes.TDM;
			case "lms":
			case "lastmanstanding":
				return OdamexGameModes.LMS;
			case "tlms":
			case "teamlms":
			case "teamlastmanstanding":
				return OdamexGameModes.TLMS;
			case "duel":
				return OdamexGameModes.DUEL;
			case "domination":
			case "coop":
			case "co-op":
			case "cooperative":
				return OdamexGameModes.COOP;
			case "survival":
				return OdamexGameModes.SURVIVAL;
		}

		throw new InputException("Unknown or supported game mode");
	}

	/**
	 * Method that contains aliases for on/off properties
	 * @param string The keyword to check
	 * @return True if to use it, false if not
	 */
	private static boolean handleTrue(String string) {
		return switch (string.toLowerCase()) {
			case "on", "true", "yes", "enable", "1" -> true;
			default -> false;
		};
	}

	/**
	 * Will return generic things from a server that a user may want to request, this method
	 * does not return anything that contains sensitive information (which can be done with reflection)
	 * @param fieldToGet A String indicating what field to get
	 * @return A String containing the data
	 */
	public String getField(String fieldToGet) {
		switch (fieldToGet.toLowerCase()) {
			case "autorestart":
				return "autorestart: " + this.auto_restart;
			case "config":
			case "cfg":
			case "configuration":
				return "config: " + nullToNone(this.config);
			case "gamemode":
			case "gametype":
				return "gamemode " + this.gameMode;
			case ".host":
			case "host":
			case "hostcommand":
			case "host_command":
				return "hostcommand: " + this.host_command;
			case "iwad":
				return "iwad: " + this.iwad;
			case "name":
			case "server_name":
			case "hostname":
			case "servername":
				return "hostname: " + this.servername;
			case "skill":
				return "skill: " + this.skill;
			case "wad":
			case "wads":
				return "wads: " + String.join(", ", this.wads);
			default:
				break;
		}
		return "Error: Not a supported keyword";
	}

	/**
	 * Checks for null values and returns and more user friendly message
	 * @param input String input
	 * @return String result
	 */
	public String nullToNone(String input) {
		if (input == null)
			return "None";
		else return input;
	}

	/**
	 * This will kill the server
	 */
	public void killServer() {
		if (this.serverprocess != null && this.serverprocess.isInitialized())
			this.serverprocess.terminateServer();
	}

	public void executeCommand(String command) {
		in.println(command);
	}
}