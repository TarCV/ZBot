/* 
 * WordToken.java
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
public class WordToken extends Token {

    public WordToken(String text) throws InvalidTokenException {
        super(text);
    }

    /* (non-Javadoc)
     * @see org.naturalcli.tokens.Token#getDefinition()
     */
    @Override
    public ITokenDefinition getDefinition() {
        return new WordTokenDefinition();
    }

    /* (non-Javadoc)
     * @see org.naturalcli.tokens.Token#matches(java.lang.String)
     */
    @Override
    public boolean matches(String text) {
        return true;
    }

}
