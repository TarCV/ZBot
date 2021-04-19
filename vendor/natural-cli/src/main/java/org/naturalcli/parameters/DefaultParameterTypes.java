/* 
 * DefaultParameters.java
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

import java.util.Set;
import java.util.HashSet;

import org.naturalcli.IParameterType;

/**
 * Helper class to obtain a set of default parameter types.
 * 
 * @author Ferran Busquets
 */
public class DefaultParameterTypes {
	
	/**
	 * Creates a set with a default parameter types.
	 * 
	 * @return a set with the default parameter types.
	 * @see EmailParamType
	 * @see IdentifierParamType
	 * @see IntegerParamType
	 * @see StringParamType
	 * @see FileParamType
	 */
	public static Set<IParameterType> createSet()
	{
		Set<IParameterType> s = new HashSet<IParameterType>();
        s.add(new BinaryParamType());
        s.add(new ByteParamType());
        s.add(new DoubleParamType());
		s.add(new EmailParamType());
        s.add(new FileParamType());
        s.add(new FloatParamType());
        s.add(new HexadecimalParamType());
		s.add(new IdentifierParamType());
		s.add(new IntegerParamType());
        s.add(new LongParamType());
        s.add(new OctalParamType());
        s.add(new ShortParamType());
		s.add(new StringParamType());
        s.add(new URLParamType());
        s.add(new WorkingURLParamType());
		return s;
	}
}
