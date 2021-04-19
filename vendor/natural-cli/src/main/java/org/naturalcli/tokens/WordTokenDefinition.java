/* 
 * WordTokenDefinition.java
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

/**
 * @author Ferran Busquets
 *
 */
public class WordTokenDefinition implements ITokenDefinition {

    /* (non-Javadoc)
     * @see org.naturalcli.tokens.ITokenDefinition#incompatibleMessage(java.lang.String)
     */
    @Override
    public String incompatibleMessage(String text) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.naturalcli.tokens.ITokenDefinition#isIncompatible(java.lang.String)
     */
    @Override
    public boolean isIncompatible(String text) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.naturalcli.tokens.ITokenDefinition#matches(java.lang.String)
     */
    @Override
    public boolean matches(String text) {
        return true;
    }

}
