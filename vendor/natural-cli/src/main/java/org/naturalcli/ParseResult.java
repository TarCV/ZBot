/* 
 * CommandParseData.java
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
 * Encapsulate the data that restuls of a command parse. 
 * It means the parameters values and the list of tokens found.
 * 
 * @author Ferran Busquets
 */
public class ParseResult {
	
	/** Parameters values **/
	private Object[] parameterValues;
	
	/** Tokens given **/
	private boolean[] tokensGiven;

	/**
	 * Constructor
	 * 
	 * @param paramValues the ordered parameter values array. For optional
	 *                    parameters not provided will be <code>null</code>
	 * @param tokensGiven the ordered array with all the tokens saying if
	 *                    each token is given or not. For non-optional tokens
     *                    the value will be always <code>true</code>.
	 */
	public ParseResult(Object[] parameterValues, boolean[] tokensGiven)
	{
		if (parameterValues == null)
			throw new IllegalArgumentException("Parameter values array cannot be null.");
		if (tokensGiven == null)
			throw new IllegalArgumentException("Tokens found array cannot be null.");
		this.parameterValues = parameterValues;
		this.tokensGiven = tokensGiven;
	}

	/**
	 * Get the parameter value in the given index.
	 * 
	 * @param parameterIndex the parameter index. This index is relative
	 *                       only for the parameters.
	 * @return the parameter value.
	 */
	public Object getParameterValue(int parameterIndex)
	{
		return this.parameterValues[parameterIndex];
	}

	/**
	 * Get a copy of the parameter values
	 * 
	 * @return object array with the prameter values 
	 */
	public Object[] getParameterValues()
	{
		return this.parameterValues.clone();
	}	
	
	/**
	 * Get the number of all possible parameters
	 *  
	 * @return number of parameters
	 */
	public int getParameterCount()
	{
		return this.parameterValues.length;
	}
		
	/**
	 * Get if the token is given or not.
	 * 
	 * @param tokenIndex the token index. 
	 * 
	 * @return <code>true</code> if the token is given or <code>false</code>
	 *         if the token is not given (only for optional parameters).
	 */
	public boolean isTokenGiven(int tokenIndex)
	{
		return this.tokensGiven[tokenIndex];
	}
	
	/**
	 * Get a copy of the tokens given.
	 * 
	 * @return boolean array with the tokens given.
	 */
	public boolean[] getTokensGiven()
	{
		return this.tokensGiven.clone();
	}
	
}
