package org.bestever.bebot;

/*
//		TSPG is banned from IPIntel, and also this somehow doesn't work properly anyway.
//		Most of the script is commented out
*/


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import static org.bestever.bebot.Bot.bold;

/**
 * Created by Sean on 19/10/2016.
 */
public class IPIntel implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(IPIntel.class);

	public final String ip;
	public final String name;
	public final ConfigData configData;
	private final Server server;

	private IPIntel(final String ip, final String name, final ConfigData configData, Server server) {
		this.ip = ip;
		this.name = name;
		this.configData = configData;
		this.server = server;
	}

	@Override
	public void run() {
		try {
			if (MySQL.checkKnownIP(ip)) {
				return;
			}
			final String contactEmail = configData.ipintel_contact;
			final double minResult = configData.ipintel_minimum;

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
				logger.error("IPIntel returned " + response + "!");
				return;
			}
			MySQL.addKnownIP(ip);
			if (response >= minResult) {
				logger.error(bold(name)+" with ip "+bold(ip)+" was kicked from " + bold(server.userName) + "'s server "+ bold(server.servername) +" on port "+bold(server.port)+" as they're suspected of being behind a proxy");
				server.executeCommand("addban " + ip + " 10minute " + "\"\\ciBanned from all " + configData.service_short + " servers on suspicion of using a proxy.\"");
				if (MySQL.addBan(ip, "Proxy (IPIntel)", "<bot>")) {
					logger.info("Proxy IP " + bold(ip) + " was added to the banlist");
				}
				else {
					logger.error("Proxy IP " + bold(ip) + " could not be added to the banlist");
				}
			}
		} catch (Exception e) {
			logger.error("IPIntel check has failed - disabling");
			configData.ipintel_enabled = false;
			e.printStackTrace();
		}
		return;
	}
			
	public static void query(String ip, String name, ConfigData configData, Server server) {
		Thread thread = new Thread(new IPIntel(ip, name, configData, server));
		thread.setName("IPIntel-" + System.nanoTime());
		thread.start();
	}
}