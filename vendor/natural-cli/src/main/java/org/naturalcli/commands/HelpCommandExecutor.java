/* 
 * HelpCommandExecutor.java
 *
 * Copyright (C) 2007 Ferran Busquets
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.naturalcli.commands;

import java.util.Set;

import org.naturalcli.Command;
import org.naturalcli.ParseResult;
import org.naturalcli.ICommandExecutor;


/**
 * Executor for <code>HelpCommand</code>
 *  
 * @see HelpCommandExecutor
 * @author Ferran Busquets
 */
public class HelpCommandExecutor implements ICommandExecutor {

	/** Commands set */
	private Set<Command> commands;

	/**
	 * Constructor.
	 * @param commands the set of commands for the help
	 */	
	public HelpCommandExecutor(Set<Command> commands)
	{
		this.commands = commands;
	}
		
	/* (non-Javadoc)
	 * @see org.naturalcli.ICommandExecutor#execute(java.lang.Object[])
	 */
	@Override
	public void execute(ParseResult parseResult)  {
		for (Command c : commands) 
		{
			if (c.isHidden())
				continue;
			System.out.println(c.getSyntax());
			System.out.println("\t" + c.getHelp());
			System.out.println();
		}
	}

}
