/* 
 * IParameterType.java
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
 * A parameter type for all the commands.
 *
 * @author Ferran Busquets
 */
public interface IParameterType {

	/**
	 * Gets the parameter type name. 
	 * 
	 * @return the name of the parameter type
	 */
	public String getParameterTypeName();
	
	/**
	 * Checks if a parameter value is of this type 
	 * of parameter.
	 * 
	 * @param  the string to be validated as this parameter type
	 * @return <code>true</code> if the validation it's right;
	 *         <code>false</code> otherwise
	 */
	public boolean validateParameter(String value);

	/**
	 * Checks if a parameter value is of this type 
	 * of parameter and returns a detailed message
	 * if the validation fails.
	 * 
	 * @param  the string to be validated as this parameter type
	 * @return <code>null</code> if the validation it's right;
	 *         a detailed message if something it's wrong
	 */
	public String validationMessage(String value);

	/**
	 * Converts the string representing the parameter value to
	 * the corresponding type value.
	 *  
	 * @param strRepresentation the string representation of the value
	 * @return real object value
	 */
	public Object convertParameterValue(String strRepresentation);		
}
