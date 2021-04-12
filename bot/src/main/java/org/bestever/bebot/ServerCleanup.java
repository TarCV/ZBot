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

import java.util.TimerTask;

/**
 * This class runs every x amount of time and is responsible for
 * killing any inactive servers as well as any servers running
 * for longer than 30 days
 */
public class ServerCleanup extends TimerTask {

	/**
	 * Holds the bot
	 */
	private Bot bot;

	/**
	 * Constructor
	 * @param bot Bot - the main bot object
	 */
	public ServerCleanup(Bot bot) {
		this.bot = bot;
	}

	/**
	 * Send a message to all servers
	 */
	public void run() {
		int killed = 0;
		for (Server s : bot.servers) {
			// Check if the server has been running for more than 3 days without activity
			if (System.currentTimeMillis() - s.serverprocess.last_activity > Server.DAY_MILLISECONDS * bot.cfg_data.cleanup_interval) {
				s.hide_stop_message = true;
				s.killServer();
				killed++;
			}
		}
		// Send a message to the channel if we've killed a server like this
		if (killed > 0) {
			bot.sendMessageToCoreChannel(Functions.pluralize("Killed " + killed + " inactive server{s} (inactive for " + bot.cfg_data.cleanup_interval + " days.", killed));
		}
	}
}
