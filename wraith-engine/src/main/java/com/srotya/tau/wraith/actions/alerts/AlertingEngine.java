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
package com.srotya.tau.wraith.actions.alerts;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.rules.Rule;

/**
 * Blueprint on an Alerting Engine and what methods it must support
 * 
 * @author ambud_sharma
 */
public interface AlertingEngine {

	/**
	 * Fired when an {@link Event} matches a {@link Rule} and needs to fire an actual
	 * {@link Alert}
	 * @param event
	 * @param ruleId
	 * @param actionId
	 * @param target
	 * @param media
	 * @param timestamp
	 * @return
	 */
	public Alert materialize(Event event, short ruleId, short actionId, String target, String media, long timestamp);

	/**
	 * Update it's internal data structures when the rule gets updated
	 * @param ruleGroup
	 * @param ruleJson
	 * @param delete
	 */
	void updateRule(String ruleGroup, String ruleJson, boolean delete);

	/**
	 * Fired when an {@link Event} matches a {@link Rule} and needs to fire an actual
	 * {@link Alert}
	 * @param event
	 * @param ruleGroup
	 * @param ruleId
	 * @param actionId
	 * @param target
	 * @param media
	 * @param timestamp
	 * @return
	 */
	public Alert materialize(Event event, String ruleGroup, short ruleId, short actionId, String target, String media, long timestamp);
	
}