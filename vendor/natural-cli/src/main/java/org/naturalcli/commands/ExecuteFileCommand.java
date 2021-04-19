/* 
 * ExecuteFile.java
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

import org.naturalcli.Command;
import org.naturalcli.InvalidSyntaxException;
import org.naturalcli.NaturalCLI;

/**
 * Implements a command that executes a command list inside a file.
 * 
 * @see ExecuteFileCommandExecutor
 * @author Ferran Busquets
 */
public class ExecuteFileCommand  extends Command {
	
	/**
	 * Constructor.
	 * 
	 * @param naturalCLI the naturalCLI processor to use.
	 */
	public ExecuteFileCommand(NaturalCLI naturalCLI) 
	{
		try {
			prepare("execute file <filename:string>", "Execute the commands on file.",
					new ExecuteFileCommandExecutor(naturalCLI)
			);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
	}	

}

