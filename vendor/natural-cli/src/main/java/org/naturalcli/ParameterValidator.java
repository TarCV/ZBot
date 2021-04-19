/*
 * ParameterTypes.java
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

import java.util.Collection;

import org.naturalcli.parameters.DefaultParameterTypes;



/**
 * This class checks parameters values against their types.
 *
 * @author Ferran Busquets
 *
 * @see IParameterType
 */
public class ParameterValidator {

	/** Parameter types for the validation */
	private Collection<IParameterType> parameterTypes;
	
	/** 
     * Creates a new instance of <code>ParameterValidator</code> with default parameter types
     */
    public ParameterValidator() 
    {   
    	this.parameterTypes = DefaultParameterTypes.createSet();
    }	
	
	/** 
     * Creates a new instance of <code>ParameterValidator</code>
     * 
     * @param parameterTypes the parameter types collection
     */
    public ParameterValidator(Collection<IParameterType> parameterTypes) 
    {   
    	this.parameterTypes = parameterTypes;
    }	


    /**
     * Validate a parameter value for a type
     * 
     * @param value the parameter value
     * @param type the parameter type name
     * @return <code>null</code> if validated, otherwise a error message
     * @throws UnknownParameterType raised if the parameter is not found
     */
    public String validate(String value, String type) throws UnknownParameterType
    {
    	IParameterType pt = getParameterType(type);
    	// If not found throw exception
    	if (pt == null)
            throw new UnknownParameterType(type);
    	// Validate the parameter
    	return pt.validationMessage(value);
    }
        
    /**
     * Gets the parameter type for the given type name
     *  
     * @param type the type name
     * @return the paramter type object
     */
    public IParameterType getParameterType(String type)
    {
    	IParameterType pt = null;
    	// Look for the parameter type
    	for (IParameterType s : this.parameterTypes)
    	{
    		if (s.getParameterTypeName().equals(type))
    		{
    			pt = s;
    			break;
    		}
    	}
    	return pt;
    }
}
