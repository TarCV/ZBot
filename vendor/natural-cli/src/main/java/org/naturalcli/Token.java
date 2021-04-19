/* 
 * Token.java
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
 * <p>
 * The <code>Token</code> class implements a token for the 
 * command grammar.
 * </p>
 * 
 * @author Ferran Busquets
 */
public class Token {
	
	/** Beginning char for a parameter */
    static private final char CHAR_BEGIN_PARAM = '<';
    
    /** Ending char for a parameter */
    static private final char CHAR_END_PARAM = '>';

    /** Beginning char for an optional token*/
    static private final char CHAR_BEGIN_OPT = '[';

    /** Ending char for an optional token */
    static private final char CHAR_END_OPT= ']';
    
    /** Char separator for a parameter name and type */
    static private final char CHAR_NAME_TYPE = ':';

    /** String for variable arguments token */
    static private final String VAR_ARGS = "...";
    
    /** Texts giving sense to the token */
    private String text;
	
    /**
     * Constructor for the token
     * 
     * @param text the token text
     * @throws InvalidTokenException 
     * @throws InvalidTokenException 
     */
	public Token(String text) throws InvalidTokenException
	{
		this.setText(text);
	}
	
	
	/**
	 * Get the token text 
	 * 
	 * @return the token text 
	 */
	public String getText() {
		return text;
	}


	/**
	 * Set the token text and validate it
	 * 
	 * @param text the token text to set
	 * @throws InvalidTokenException 
	 * @throws InvalidTokenException 
	 */
	public void setText(String text) throws InvalidTokenException  {
		validate(text);
		this.text = text;
	}
	
	private void validate(String text) throws InvalidTokenException 
	{
		// Validate null
		if (text == null)
			throw new InvalidTokenException("Null token text");
		if (text.length() == 0)
			throw new InvalidTokenException("Empty token text");
		// Validate invalid optional parameters
		String bad_start = new String(new char[] { CHAR_BEGIN_PARAM, CHAR_BEGIN_OPT });
		String bad_end = new String(new char[] { CHAR_END_OPT, CHAR_END_PARAM });
		String opt_par_start = new String(new char[] { CHAR_BEGIN_OPT, CHAR_BEGIN_PARAM });
		String opt_par_end = new String(new char[] { CHAR_END_PARAM, CHAR_END_OPT,  });
		if (text.startsWith(bad_start) || text.endsWith(bad_end))
			throw new InvalidTokenException("Bad optional parameter token");
		if (text.startsWith(opt_par_start) && !text.endsWith(opt_par_end))
			throw new InvalidTokenException("Bad optional parameter token");
		if (!text.startsWith(opt_par_start) && text.endsWith(opt_par_end))
			throw new InvalidTokenException("Bad optional parameter token");
		// Validate the options format
		char first = text.charAt(0);
		char last = text.charAt(text.length()-1);
		if (first == CHAR_BEGIN_OPT && last != CHAR_END_OPT)
			throw new InvalidTokenException("Bad optional token");
		if (first != CHAR_BEGIN_OPT && last == CHAR_END_OPT)
			throw new InvalidTokenException("Bad optional token");
		// Validate the parameter format
		if (first == CHAR_BEGIN_PARAM && last != CHAR_END_PARAM)
			throw new InvalidTokenException("Bad parameter token");
		if (first != CHAR_BEGIN_PARAM && last == CHAR_END_PARAM)
			throw new InvalidTokenException("Bad parameter token");
	}

	/**
	 * Checks if it's an optional token
	 * 
	 * @return <code>true</code> if its optional, <code>false</code> otherwise
	 */
    public boolean isOptional()
    {
        boolean begin = text.charAt(0) == CHAR_BEGIN_OPT;
        boolean end = text.charAt(text.length()-1) == CHAR_END_OPT;
        return begin && end;
    }
	
	/**
	 * Checks if it's a parameter token
	 * 
	 * @return <code>true</code> if it's a parameter, <code>false</code> otherwise
	 */    
    public boolean isParameter()
    {    	
        boolean begin, end;
        boolean opt = this.isOptional();
        begin = text.charAt(opt?1:0) == CHAR_BEGIN_PARAM;
        end = text.charAt(text.length()-(opt?2:1)) == CHAR_END_PARAM;
        return begin && end;
    }

	/**
	 * Checks if it's an optional parameter token
	 * 
	 * @return <code>true</code> if its optional parameter, <code>false</code> otherwise
	 */
    public boolean isOptionalParameter()
    {
        return isParameter() && isOptional();
    }
	
	/**
	 * Checks if it's a mandatory parameter token
	 * 
	 * @return <code>true</code> if it's mandatory parameter, <code>false</code> otherwise
	 */
    public boolean isMandatoryParameter()
    {
        return isParameter() && !isOptional();
    }    
    
    /**
     * Helper method for {@link org.naturalcli.Token#getParameterName()} and
     * {@link org.naturalcli.Token#getParameterName()}
     * 
     * @param n1t2 the info requested. 1 for name and 2 for type
     * @return the parameter name, type name or <code>null</code>
     */
    private String getParameterInfo(int n1t2)
    {
    	if (n1t2 != 1 && n1t2 != 2)
    		throw new RuntimeException("Bad value for n1t2.");
    	if (!this.isParameter())
    		return null;
    	String word = this.getWord();
        int i = word.indexOf(CHAR_NAME_TYPE);
        if (i == -1)
            return word;
        return (n1t2 == 1) ? word.substring(0, i) : word.substring(i+1);
    }
        
	/**
	 * Gets the parameter name for the token
	 * 
	 * @return the parameter name, or <code>null</code> if it's not a parameter.
	 */        
    public String getParameterName()
    {
    	return this.getParameterInfo(1);
    }    

	/**
	 * Gets the parameter type name for the token
	 * 
	 * @return the parameter type name, or <code>null</code> if it's not a parameter.
	 */        
    public String getParameterTypeName()
    {
    	return this.getParameterInfo(2);
    }    
    
    /**
     * Determines if it's an identifier
     * 
     * @return <code>true</code> if it's an identifier, <code>false</code> otherwise
     */
    public boolean isIdentifier()
    {
    	return !this.isParameter();
    }

    /**
     * Determines if it's a variable arguments token
     * 
     * @return <code>true</code> if it's a variable arguments token, <code>false</code> otherwise
     */
    public boolean isVarArgs()
    {
    	return this.text.equals(VAR_ARGS);
    }    
    
    /**
     * Obtains the word that represents the token, it means
     * the token text without optional or parameter chars
     * 
     * @return the word that represents the token
     */
    public String getWord()
    {
    	String word = new String(text);
    	if (isOptionalParameter())
    		return text.substring(2, text.length()-2);
    	else if (isOptional() || isParameter())
    		return text.substring(1, text.length()-1);
    	return word;
    }
    
    /**
     * Checks the text given to see if it's like the token
     * 
     * @param text the text to match
     * @param pv the parameter validator
     * @return <code>true</code> if matches, <code>false</code> if not
     * @throws UnknownParameterType 
     * @throws UnknownParameterType
     */
    public boolean matches(String t, ParameterValidator pv) throws UnknownParameterType 
    {
    	if (this.isIdentifier())
    		return this.getWord().equals(t);
    	else if (this.isParameter())
    		return pv.validate(t, this.getParameterTypeName()) == null;
    	return false;
    }
    
}
