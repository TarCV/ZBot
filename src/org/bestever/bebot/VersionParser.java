package org.bestever.bebot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.json.*; // I'll only use 2 classes anyway

/**
 * Parses version.json, allowing the bot to use multiple Zandronum version.
 * @author Sean Baggaley
 */
public class VersionParser {
	public HashMap<String, Version> versions;
	
	public Version defaultVersion = null;
	
	public VersionParser(String configPath) {
		versions = new HashMap<String, Version>();
		
		// Find where our versions.json is, first thing
		File cfgFile = new File(configPath);
		String cfgDir = cfgFile.getParent();
		File versionsFile = new File(cfgDir + (cfgDir.endsWith("/") ? "" : "/") + "versions.json");
		
		if (!versionsFile.exists()) {
			System.err.println("Could not find versions.json - make sure it's present and you renamed the example file!");
			System.exit(120);
		}
		
		// now it gets messy
		try {
			String json = "";
			Scanner vScanner = new Scanner(versionsFile);
			
			// read the file.
			while (vScanner.hasNextLine())
				json += vScanner.nextLine();
			
			vScanner.close();
			
			JSONArray array = new JSONArray(json);
			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				String name = object.getString("name");
				String path = object.getString("path");
				String desc = object.getString("description");
				boolean isDefault = object.getBoolean("default");
				Version v = new Version(name, path, isDefault, desc);
				versions.put(name, v);
				
				if (v.isDefault && defaultVersion != null)
					defaultVersion = v;
			}
			
		} catch (Exception e) {
			System.err.println("An exception has occured while parsing versions.json:");
			System.err.println();
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(121);
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
