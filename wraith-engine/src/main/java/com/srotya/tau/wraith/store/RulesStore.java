/**
 * Copyright 2016 Symantec Corporation.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”); 
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
 */
package com.srotya.tau.wraith.store;

import java.io.IOException;
import java.util.Map;

import com.srotya.tau.wraith.rules.Rule;

/**
 * Blueprints for a persistent storage media for {@link Rule}s
 * 
 * @author ambud_sharma
 */
public interface RulesStore extends Store {
	
	/**
	 * List all {@link Rule} grouped by their rule groups and mapped with their ruleIds
	 * @return map of rule group and map of ruleIds and {@link Rule} objects
	 * @throws IOException
	 */
	public Map<String, Map<Short, Rule>> listGroupedRules() throws IOException;
	
}
