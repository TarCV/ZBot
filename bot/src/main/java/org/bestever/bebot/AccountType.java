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

import java.util.Arrays;

public enum AccountType {
	/**
	 * Not a ZBot user. A guest and means it was not found; also returns this if not logged in
	 */
	NONE(""),

	/**
	 * Normal users.
	 */
	REGISTERED("ZBot user"),
	
	/**
	 * Can access other people's RCon passwords.
	 */
	VIP("ZBot superuser"),
	
	/**
	 * A moderator who has access to some more commands.
	 */
	MODERATOR("ZBot moderator"),
	
	/**
	 * <code>MODERATOR</code> with more power.
	 */
	ADMIN("ZBot administrator");

	private final String title;

	AccountType(String title) {
		this.title = title;
	}

	public static AccountType fromString(String str) {
		return Arrays.stream(values())
				.filter(t -> t.title.equals(str))
				.findFirst()
				.orElse(AccountType.NONE);
	}

	/**
	 * To check for different masks, this method searches to see if you contain one of them.
	 * Usage of this function would be similar to: isAccountType(accountHere, AccountType.ADMIN, AccounType.TRUSTED);
	 * to check if they are either an admin or trusted user
	 * @param userRole        The role that the user have
	 * @param minimumLevel    Minimum level.
	 * @return True if one of the types is met, false if none are
	 */
	public static boolean isAccountTypeOf(AccountType userRole, AccountType minimumLevel) {
		return (userRole.ordinal() >= minimumLevel.ordinal());
	}
}