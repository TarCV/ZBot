/* 
 * WorkingURLParamType.java
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
package org.naturalcli.parameters;

import java.io.*;
import java.net.*;

import org.naturalcli.IParameterType;

/**
 * The class implements an URL which is validated parameter type. 
 * 
 * @see java.net.URL
 * @author Ferran Busquets 
 */
public class WorkingURLParamType implements IParameterType {

	/* (non-Javadoc)
	 * @see org.naturalcli.IParameterType#getParameterTypeName()
	 */
	@Override
	public String getParameterTypeName() {
		return "working_url";
	}

	/* (non-Javadoc)
	 * @see org.naturalcli.IParameterType#validateParameter(java.lang.String)
	 */
	@Override
	public boolean validateParameter(String value) {
		try {
			URLConnection con = new URL(value).openConnection();
			Reader rd = new InputStreamReader(con.getInputStream());
            rd.read();
			return true;
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
            return false;
        }
	}

	/* (non-Javadoc)
	 * @see org.naturalcli.IParameterType#validationMessage(java.lang.String)
	 */
	@Override
	public String validationMessage(String value) {
		return this.validateParameter(value) ? null : "Not working URL.";
	}
	
	/* (non-Javadoc)
	 * @see org.naturalcli.IParameterType#convertParameterValue(java.lang.String)
	 */
	@Override
	public Object convertParameterValue(String strRepresentation) {
		try {
			return new URL(strRepresentation);
		} catch (MalformedURLException e) {
			return null;
		}
	}


}
