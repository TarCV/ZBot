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


import java.io.*;

import org.naturalcli.ParseResult;
import org.naturalcli.ICommandExecutor;
import org.naturalcli.NaturalCLI;


/**
 * Executor for <code>ExecuteFileCommand</code>
 *  
 * @see ExecuteFileCommand
 * @author Ferran Busquets
 */
public class ExecuteFileCommandExecutor implements ICommandExecutor {

	static final String COMMENT = "#";
	
	private NaturalCLI naturalCLI;
	
	public ExecuteFileCommandExecutor(NaturalCLI naturalCLI)
	{
		this.naturalCLI = naturalCLI;
	}
		
	/* (non-Javadoc)
	 * @see org.naturalcli.ICommandExecutor#execute(java.lang.Object[])
	 */
	@Override
	public void execute(ParseResult parseResult)  {
		try {
			String file_name = parseResult.getParameterValue(0).toString();
	        BufferedReader in = new BufferedReader(new FileReader(file_name));
	        String command;
	        while ((command = in.readLine()) != null)
	        {
	        	if (command.startsWith(COMMENT))
	        		continue;
	    		this.naturalCLI.execute(command, 0);
	        }
	        in.close();
		} catch (Exception e) {
			throw new RuntimeException("Execution aborted", e);
		}
	}

}
