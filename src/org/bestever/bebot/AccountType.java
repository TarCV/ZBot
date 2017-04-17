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

public class AccountType {
	
	/**
	 * Below are the bitmask permissions for userroups
	 **/
	
	/**
	 * Banned Users
	 */
	public static final int BANNED = 0;
	
	/**
	 * Guests are users who are not logged into BestBot or NickServ.
	 */
	public static final int GUEST = -1;
	
	/**
	 * Normal users.
	 */
	public static final int REGISTERED = 1;
	
	/**
	 * Can access other people's RCon passwords.
	 */
	public static final int RCON = 2;
	
	/**
	 * A moderator who has access to some more commands.
	 */
	public static final int MODERATOR = 3;
	
	/**
	 * <code>MODERATOR</code> with more power.
	 */
	public static final int ADMIN = 4;
	
	/**
	 * <code>ADMIN</code> but has permission to use .shell and .terminate
	 */
	public static final int OPERATOR = 5;
	
	/**
	 * To check for different masks, this method searches to see if you contain one of them.
	 * Usage of this function would be similar to: isAccountType(accountHere, AccountType.ADMIN, AccounType.TRUSTED);
	 * to check if they are either an admin or trusted user
	 * @param accountType The bitmask to check of the account
	 * @param minimumLevel Minimum level.
	 * @return True if one of the types is met, false if none are
	 */
	public static boolean isAccountTypeOf(int accountType, int minimumLevel) {
		return (accountType >= minimumLevel);
	}
}