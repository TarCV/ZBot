/* 
 * InvalidSyntaxDefinionException.java
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
 * Thrown when an invalid syntax definition is found.
 * 
 * @author Ferran Busquets
 * 
 */
@SuppressWarnings("serial")
public class InvalidSyntaxException extends Exception {

	/**
	 * 
	 */
	public InvalidSyntaxException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public InvalidSyntaxException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public InvalidSyntaxException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public InvalidSyntaxException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
