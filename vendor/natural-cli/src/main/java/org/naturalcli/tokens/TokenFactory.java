/* 
 * TokenFactory.java
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
package org.naturalcli.tokens;

import org.naturalcli.InvalidTokenException;

/**
 * @author Ferran Busquets
 *
 */
public class TokenFactory {
    
   public static Token getToken(String text) throws InvalidTokenException
   {  
       Token t;
       // Parameter
       t = checkTokenDefinition(text, new ParameterTokenDefinition());
       if (t != null) return t;
       // VarArg
       t = checkTokenDefinition(text, new VarArgTokenDefinition());
       if (t != null) return t;
       // Work token
       return new WordToken(text);
   }

   /**
    * @param text
    * @param def
    * @throws InvalidTokenException
    */
   private static Token checkTokenDefinition(String text, ITokenDefinition def)
   throws InvalidTokenException 
   {
       String error;
       if (def.matches(text))
           return new ParameterToken(text);
       error = def.incompatibleMessage(text);
       if (error != null)
           throw new InvalidTokenException(error);
       return null;
   }
}
