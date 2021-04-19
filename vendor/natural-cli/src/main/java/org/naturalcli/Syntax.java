/* 
 * CommandGrammar.java
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

import java.util.LinkedList;
import java.util.List;


/**
 * Implements a class with a syntax definition and parser.
 * 
 * @author Ferran Busquets
 *
 */
public class Syntax {

	/** Syntax definition */
	private String definition;
	
	/** List of tokens defining the grammar */
	private List<Token> grammar;
	
	/** Number of parameters */
	private int paramCount;

	/**
	 * Constructor for the Syntax class
	 * 
	 * @param definition the syntax definition
	 * @throws InvalidSyntaxException 
	 */
	public Syntax(String definition) throws InvalidSyntaxException
	{
		this.setDefinition(definition);
	}


	/**
	 * Gets the syntax definition
	 * 
	 * @return the definition
	 */
	public String getDefinition() {
		return definition;
	}
	
	/**
	 * Sets the definition for the syntax
	 * 
	 * @param definition the definition to set
	 * @throws InvalidSyntaxException 
	 */
	private void setDefinition(String definition) throws InvalidSyntaxException {
		if (definition == null || definition.length() == 0)
			throw new IllegalArgumentException("The definition cannot be null or empty string.");
		this.definition = definition;
		this.compile();
	}
		
	/**
	 * Creates the grammar for the command
	 * @throws InvalidSyntaxException 
	 * 
	 */
	private void compile() throws InvalidSyntaxException
	{
		grammar = new LinkedList<Token>();
		String[] tokens = definition.split(" "); 
		Token last_t = null;
   		for (int i = 0; i < tokens.length ; i++)
   		{
   			String s = tokens[i];
   			// Create the token
   			Token t;
			try {
				t = new Token(s);
			} catch (InvalidTokenException e) {
				throw new InvalidSyntaxException("Bad token.", e);
			}
			// Validating the variable argument token
			if (t.isVarArgs())
			{
				if (last_t == null || i != tokens.length-1)
					throw new InvalidSyntaxException("Variable arguments token only allowed at the end.");
				if (!last_t.isMandatoryParameter())
					throw new InvalidSyntaxException("Variable arguments have to follow a mandatory parameter.");
			}
			// Validating optional parameters
   			if (t.isParameter() && last_t != null && last_t.isOptional() && 
   			    t.getParameterTypeName().equals(last_t.getParameterTypeName()))
   			{
   				throw new InvalidSyntaxException("An optional parameter cannot be followed by a parameter of the same type.");
   			}
   			// Add the token
   			grammar.add(t);
   			// 
   			if (t.isParameter())
   				paramCount++;
   			//    			
   			last_t = t;
   		}
	} 
	
	/**
	 * Parse the tokens to see if match with the syntax
	 * 
	 * @param candidates the candidate tokens to match
	 * @param first the first item in the tokens list 
	 * @param pv the parameter validator
	 * @return <code>null</code> if the candidate does not match, 
	 *         or a <code>ParseData</code> object
	 * @throws UnknownParameterType
	 * @see ParseResult
	 */
	public ParseResult parse(String[] candidates, int first, ParameterValidator pv) throws UnknownParameterType
	{
		if (candidates == null)
			throw new IllegalArgumentException("The string array to parse cannot be null.");
		if (pv == null)
			throw new IllegalArgumentException("Parameter validator cannot be null.");
		if (first < 0)
			throw new IllegalArgumentException("First token index have to be 0 or greater.");
		List<Object> ParamValues = new LinkedList<Object>();
		boolean[] tokenGiven = new boolean[this.grammar.size()];
		int ip = 0; // index for paramValues
		int it = 0; // index for tokensGiven
		int ig = 0; // index for the grammar
		int ic = first; // index for the candidates
		boolean varargs = false;
		/* 
		 *                          increment       
		 * match opt param   ip  it  ig  ic
		 *   0    0    ?      -   -   -   -   return null
		 *   0    1    0      0   1   1   0    
		 *   0    1    1      1   1   1   0    
		 *   1    ?    0      0   1   1   1         
		 *   1    ?    1      1   1   1   1   
		 *   
		 */
		while (ig < grammar.size() && ic < candidates.length)
		{
			Token tgrammar = grammar.get(ig);
			// check if there are varargs
			varargs = tgrammar.isVarArgs();		
			if (varargs)
				break;
			boolean match = tgrammar.matches(candidates[ic], pv);
			boolean opt = tgrammar.isOptional();
			boolean param = tgrammar.isParameter();
			// Validate the token
			if (!match && !opt)
				return null;
			// Increment ip and add value to paramValues
			if (param) {
				if (opt && !match)
					ParamValues.add(null);
				else
					ParamValues.add(pv.getParameterType(tgrammar.getParameterTypeName()).convertParameterValue(candidates[ic]));
				ip++;
			}
			// Increment ic if matches
			if (match)
				ic++;
			// Increment it and ig and add value to tokenGiven
			tokenGiven[it++] = match;
			ig++;
		}
		// Process the variable arguments
		if (varargs)
		{
		    // Validate for the last token 
            if (ig != grammar.size()-1)
                throw new RuntimeException("Internal error: Varargs token have to be the last one.");         
		    // Get token
			tokenGiven[it] = true;
			Token token = grammar.get(ig-1); // Token before the varargs token
			ig++; 
			// Validate optional
			if (token.isOptional())
				throw new RuntimeException("Internal error: Parameter for variable arguments cannot be optional.");			
			// Go for values
			while(ic < candidates.length)
			{
				if (!token.matches(candidates[ic], pv))
					return null;
				ParamValues.add(pv.getParameterType(token.getParameterTypeName()).convertParameterValue(candidates[ic]));
				ic++;
			}
		}
		// Validate ParamValuesAux
		if (!varargs && ParamValues.size() > paramCount)
			throw new RuntimeException("Internal error: Invalid parameter count.");
		// If too many candidates, fail
		if (ic != candidates.length)
		    return null;
		// If all the grammar is parsed, it's ok
		if (ig == grammar.size())
			return new ParseResult(ParamValues.toArray(), tokenGiven);
		// If varargs but no arguments
		if (ig == grammar.size()-1 && grammar.get(grammar.size()-1).isVarArgs())
			return new ParseResult(ParamValues.toArray(), tokenGiven);
        // Validate that the following tokens on grammar are all optional
        while (ig < grammar.size())
        {
            if (grammar.get(ig).isOptional())
                ig++;
            else
                return null;
        }
        return new ParseResult(ParamValues.toArray(), tokenGiven);
	}

	/*
	 * Returns the definition for this Syntax
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
	    return this.getDefinition();
	}
}
