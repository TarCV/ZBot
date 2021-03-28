package org.bestever.bebot;

/*
//		TSPG is banned from IPIntel, and also this somehow doesn't work properly anyway.
//		Most of the script is commented out
*/


import org.jibble.pircbot.Colors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by Sean on 19/10/2016.
 */
public class IPIntel implements Runnable {
	public final String ip;
	public final String name;
	public final Server source;

	private IPIntel(final String ip, final String name, final Server source) {
		this.ip = ip;
		this.name = name;
		this.source = source;
	}

	@Override
	public void run() {
		try {
			if (MySQL.checkKnownIP(ip)) {
				return;
			}
			final String contactEmail = source.bot.cfg_data.ipintel_contact;
			final double minResult = source.bot.cfg_data.ipintel_minimum;

			StringBuilder paramBuilder = new StringBuilder();
			paramBuilder.append("ip=" + ip);
			paramBuilder.append("&contact=" + URLEncoder.encode(contactEmail, "UTF-8"));

			if (minResult == 1.0) {
				paramBuilder.append("&flags=m");
			}
			
			URL url = new URL("http://check.getipintel.net/check.php?" + paramBuilder.toString());

			HttpURLConnection connection;

			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// TODO: parse response code too
			connection.getResponseCode();

			BufferedReader in = new BufferedReader(
			new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder data = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				data.append(inputLine);
			}
			in.close();

			String responseText = data.toString();
			final double response = Double.parseDouble(responseText);

			if (response < 0) {
				System.err.println("IPIntel returned " + response + "!");
				return;
			}
			MySQL.addKnownIP(ip);
			if (response >= minResult) {
				source.bot.sendLogErrorMessage(Colors.BOLD+name+Colors.BOLD+" with ip "+Colors.BOLD+ip+Colors.BOLD+" was kicked from " + Colors.BOLD+source.sender+Colors.BOLD + "'s server "+ Colors.BOLD+source.servername+Colors.BOLD +" on port "+Colors.BOLD+source.port+Colors.BOLD+" as they're suspected of being behind a proxy");
				source.in.println("addban " + ip + " 10minute " + "\"\\ciBanned from all " + source.bot.cfg_data.service_short + " servers on suspicion of using a proxy.\"");
				if (MySQL.addBan(ip, "Proxy (IPIntel)", source.bot.cfg_data.irc_name)) {
					source.bot.sendLogInfoMessage("Proxy IP " + Colors.BOLD+ip+Colors.BOLD + " was added to the banlist");
				}
				else { 
					source.bot.sendLogErrorMessage("Proxy IP " + Colors.BOLD+ip+Colors.BOLD + " could not be added to the banlist");
				}
			}
		} catch (Exception e) {
			source.bot.sendLogErrorMessage("IPIntel check has failed - disabling");
			source.bot.cfg_data.ipintel_enabled = false;
			e.printStackTrace();
		}
		return;
	}
			
	public static void query(String ip, String name, Server server) {
		Thread thread = new Thread(new IPIntel(ip, name, server));
		thread.setName("IPIntel-" + System.nanoTime());
		thread.start();
	}
}