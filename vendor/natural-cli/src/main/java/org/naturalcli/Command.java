/*
 * Command.java
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

package org.naturalcli;


/**
 * Represents a command definition 
 * 
 * @author Ferran Busquets
 */
public class Command {

	/** Help message */
	private String help;

	/** Command executor */	
	private ICommandExecutor executor;
	
	/** Syntax definition  */
	private Syntax syntax;

	private final char CHAR_HIDDEN_COMMAND = '.';
	
	/**
	 * Constructs a new command.
	 * 
	 * @param syntax the syntax for the command.
	 * @param helpthe help help of the command.
	 * @param ce command executor.
	 * @throws InvalidSyntaxDefinionException.
	 */
	public Command(String syntax, String help, ICommandExecutor ce) throws InvalidSyntaxException{
		this.prepare(syntax, help, ce);
	}

	/**
	 * Default constructor only for inheritors.
	 */
	protected Command() {
	}
	
	/**
	 * Initialize the command.
	 * 
	 * @param syntax the syntax for the command.
	 * @param helpthe help help of the command.
	 * @param ce command executor.
	 * @throws InvalidSyntaxDefinionException.
	 */
	protected void prepare(String syntax, String help, ICommandExecutor ce) throws InvalidSyntaxException 
	{
		if (help == null || help.length() == 0)
			throw new IllegalArgumentException("Syntax cannot be empty.");
		if (ce == null)
			throw new IllegalArgumentException("Command executor cannot be null.");
		this.help = help;
		this.syntax = new Syntax(syntax);
		this.executor = ce;
	}
	
	/**
	 * Determine if this is a hidden command.
	 * 
	 * @return <code>true</code> if it's a hidden command, <code>false</code> if not.
	 */
	public boolean isHidden() {
		return getHelp().charAt(0) == CHAR_HIDDEN_COMMAND;
	}

	/**
	 * Returns a string with the syntax for the command.
	 * 
	 * @return A string with the syntax for the command.
	 */
	public Syntax getSyntax() {
		return syntax;
	}

	/**
	 * Returns the help for the commend.
	 * 
	 * @return The help for the command.
	 */
	public String getHelp() {
		return help;
	}

	/**
	 * Get the executor for the command.
	 * 
	 * @return the executor.
	 */
	public ICommandExecutor getExecutor() {
		return executor;
	}

}
