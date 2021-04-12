package org.bestever.bebot;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Parses version.json, allowing the bot to use multiple Zandronum versions.
 * @author Sean Baggaley
 */
public class VersionParser {
	public HashMap<String, Version> versions;
	public ArrayList<Version> list;
	public Version defaultVersion = null;
	private final Bot bot;
	private final String configPath;
	
	public VersionParser(String configPath, Bot bot) {
		this.bot = bot;
		this.configPath = configPath;
		load();
	}
	
	public void load() {
		versions = new HashMap<String, Version>();
		list = new ArrayList<Version>();

		// Find where our versions.json is, first thing
		try {
			File versionsFile = new File(configPath);
			if (!versionsFile.exists()) {
				System.err.println("Could not find "+configPath+" - make sure it exists and it's a valid json file!");
			//	System.exit(120);
			}
			// now it gets messy
			try {
				String json = "";
				try (Scanner vScanner = new Scanner(versionsFile)) {

					// read the file.
					while (vScanner.hasNextLine())
						json += vScanner.nextLine();

				}

				JsonArray array;
				try (JsonReader reader = Json.createReader(new StringReader(json))) {
					array = reader.readArray();
				}
				for (int i = 0; i < array.size(); i++) {
					JsonObject object = array.getJsonObject(i);
					String name = object.getString("name");
					String path = object.getString("path");
					String desc = object.getString("description");
					String data = object.getString("data");
					boolean isDefault = object.getBoolean("default");
					Version v = new Version(name, path, isDefault, desc, data);
					versions.put(name, v);
					list.add(v);

					if (isDefault && defaultVersion != null)
						defaultVersion = v;
					
					System.out.println("Imported version "+name+" from "+path+" ("+desc+") Data: "+data+" Default: "+isDefault);
					System.out.println(v);
					if (bot.hasStarted) {
						bot.sendLogInfoMessage("Imported version "+name+" from "+path+" ("+desc+") Data: "+data+" Default: "+isDefault);
					}
				}


				if (defaultVersion == null) {
					defaultVersion = list.get(0);
					System.out.println("No default version specified, using first version read.");
				}

			} catch (Exception e) {
				System.err.println("An exception has occured while parsing versions.json:");
				System.err.println();
				System.err.println(e.getMessage());
				e.printStackTrace();
				if (bot.hasStarted) {
					bot.sendLogErrorMessage("An exception has occured while parsing versions.json");
				}
			//	System.exit(121);
			}
		}
		catch (Exception e) {
			System.err.println("Could not load "+configPath+"!");
			System.out.println(e);
		//	System.exit(120);
		}
	}
	
	/**
	 * Checks if a version is valid.
	 * @param name Name of the wanted version.
	 * @return a Version if name is valid, null otherwise.
	 */
	public Version getVersion(String name) {
		if (versions.containsKey(name))
			return versions.get(name);
		
		return null;
	}
}
