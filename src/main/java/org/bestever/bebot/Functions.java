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

import com.google.common.net.InetAddresses;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Functions {
	private Functions() { }

	/**
	 * Checks for a valid port number
	 * Must be an integer between Bot.MAX_PORT and Bot.MIN_PORT
	 * @return true if the port is valid
	 */
	public static boolean checkValidPort(String port) {
		if (isInteger(port)) {
			int numPort = Integer.parseInt(port);
			return numPort >= Bot.min_port && numPort < Bot.max_port;
		}
		else
			return false;
	}

	/**
	 * Checks a message and number and returns the pluralized version (if need be)
	 * @param message String - the phrase to be checked
	 * @param number Int - the number to be checked
	 * @return String - the pluralized? string
	 */
	public static String pluralize(String message, int number) {
		if (message.contains("{s}")) {
			if (number == 1) {
				return message.replace("{s}", "");
			}
			else {
				return message.replace("{s}", "s");
			}
		}
		// Passed a string withous {s}
		return message;
	}

	/**
	 * Removes duplicates from an arraylist by casting to a set
	 * @param l ArrayList - the list
	 * @return cleaned ArrayList
	 */
	public static List<String> removeDuplicateWads(List<String> l) {
		Set<String> setItems = new HashSet<>(l);
		return new ArrayList<>(setItems);
	}

	/**
	 * Generates an MD5 hash
	 * @return 32 character MD5 hex string
	 */
	public static String generateHash() throws NoSuchAlgorithmException {
		String seed = System.nanoTime() + "SOON";
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(seed.getBytes());
		return byteArrayToHex(md.digest());
	}

	/**
	 * Checks if an IP address is in the range of another IP address
	 * @param ip String - IP address of the user
	 * @param ipRange String - IP range (asterisks)
	 * @return true/false
	 */
	@SuppressWarnings("UnstableApiUsage")
	public static boolean inRange(String ip, String ipRange) {
		long startIP = new BigInteger(InetAddresses.forString(ipRange.replace("*","0")).getAddress()).intValue();
		long endIP = new BigInteger(InetAddresses.forString(ipRange.replace("*","255")).getAddress()).intValue();
		long sourceIP = new BigInteger(InetAddresses.forString(ip).getAddress()).intValue();
		return sourceIP >= startIP && sourceIP <= endIP;
	}

	/**
	 * Escapes quotes in a string
	 * @param input String - input
	 * @return String - escaped string
	 */
	public static String escapeQuotes(String input) {
		StringBuilder s = new StringBuilder();
		for (char c : input.toCharArray()) {
			if (c == '\"')
				s.append('\\');
			s.append(c);
		}
		return s.toString();
	}

	/**
	 * Given a byte array, returns a hexadecimal string
	 * @param bytes byte array
	 * @return 16bit hex string
	 */
	public static String byteArrayToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes)
			sb.append(String.format("%02x", b&0xff));
		return sb.toString();
	}

	/**
	 * Checks to see if a number is numeric
	 * In a recent update, now checks safely for nulls should such a thing happen
	 * @param maybeid The String to check (does parse double)
	 * @return True if it is a number, false if it's not
	 */
	public static boolean isInteger(String maybeid) {
		try {
			Integer.parseInt(maybeid);
		} catch (NumberFormatException | NullPointerException nfe)	{
			return false;
		}
		return true;
	}

	/**
	 * Checks to see if a given port is in use
	 * @param checkport The port to check
	 * @return True if it's available, false if not
	 */
	public static boolean checkIfPortAvailable(int checkport) {
		try (
				ServerSocket ss = new ServerSocket(checkport);
				DatagramSocket ds = new DatagramSocket(checkport)
		) {
			ss.setReuseAddress(true);
			ds.setReuseAddress(true);
			return true;
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * Checks for an available port from minport up to (but NOT including) maxport
	 * @param minport The minimum port to check
	 * @param maxport The maximum port that is one above what you would check (ex: 20200 would be the same as checking up to 20199)
	 * @return The first available port, or 0 if no port is available
	 */
	public static int getFirstAvailablePort(int minport, int maxport) {
		for (int p = minport; p < maxport; p++) {
			if (checkIfPortAvailable(p))
				return p;
		}
		return 0;
	}

	/**
	 * Function that takes a time in seconds
	 * and converts it to a string with days, hours, minutes
	 * and seconds.
	 * @param milliseconds in long format
	 * @return A String in a readable format
	 */
	public static String calculateTime(long milliseconds) {
		long seconds = milliseconds / 1000;
		int day = (int)TimeUnit.SECONDS.toDays(seconds);
		long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
		long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
		long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) *60);
		return day + " days " + hours + " hours " + minute + " minutes and " + second + " second(s).";
	}

	/**
	 * Check if a file exists
	 * @param file String path to file
	 * @return true if exists, false if not
	 */
	public static boolean fileExists(String file) {
		File f = new File(file);
		return f.exists();
	}

	/**
	 * Returns a cleaned string for file inputs
	 * @param input String - the string to clean
	 * @return cleaned string
	 */
	public static String cleanInputFile(String input) {
		return input.replace("/", "").trim();
	}

	static String absolutePath(String path) {
        return new File(path).getAbsolutePath();
    }

    static void createDirectoryAndFile(File file) throws IOException {
        final Path path = file.toPath();
        Files.createDirectories(path.getParent());
        Files.createFile(path);
    }
}
