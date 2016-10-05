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
package com.srotya.tau.wraith.rules;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.Action;

/**
 * An wireframe for caller of RulesEngine so that the RulesEngine can notify of
 * activities to the caller in an event driven manner via these callbacks.
 * 
 * @author ambud_sharma
 */
public interface RulesEngineCaller<K, C> {

	/**
	 * Handle emission of an {@link Action} error which happens when an action
	 * can't be applied
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param actionErrorEvent
	 */
	public void emitActionErrorEvent(C eventCollector, K eventContainer, Event actionErrorEvent);

	/**
	 * Handle alert {@link Action}s
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param outputEvent
	 * @param ruleId
	 * @param actionId
	 * @param target
	 * @param mediaType
	 */
	public void emitRawAlert(C eventCollector, K eventContainer, Event outputEvent, Short ruleId, Short actionId, String target, String mediaType);
	
	/**
	 * Handle alert {@link Action}s
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param outputEvent
	 * @param ruleGroup 
	 * @param ruleId
	 * @param actionId
	 * @param ruleName
	 * @param templateId
	 * @param timestamp
	 */
	public void emitTemplatedAlert(C eventCollector, K eventContainer, Event outputEvent, String ruleGroup, Short ruleId, Short actionId, String ruleName, Short templateId, Long timestamp);
	
	/**
	 * Handle if rule doesn't match for an event
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param inputEvent
	 * @param rule
	 */
	public void handleRuleNoMatch(C eventCollector, K eventContainer, Event inputEvent, Rule rule);

	/**
	 * Report if the there's a match for ruleId. <br>
	 * <br>
	 * To be used for performance statistics.
	 * 
	 * @param ruleId
	 */
	public void reportRuleHit(String ruleGroup, short ruleId);

	/**
	 * Report time taken to execute the supplied rule id <br>
	 * <br>
	 * To be used for performance statistics.
	 * 
	 * @param ruleId
	 * @param executeTime
	 */
	public void reportRuleEfficiency(String ruleGroup, short ruleId, long executeTime);

	/**
	 * Report time taken to execute the condition for the supplied rule id <br>
	 * <br>
	 * To be used for performance statistics.
	 * 
	 * @param ruleId
	 * @param executeTime
	 */
	public void reportConditionEfficiency(String ruleGroup, short ruleId, long executeTime);

	/**
	 * Handle emission of an aggregation event
	 * 
	 * @param action
	 * @param eventCollector
	 * @param eventContainer
	 * @param originalEvent
	 * @param timestamp
	 * @param windowSize
	 * @param ruleActionId
	 * @param aggregationKey
	 * @param aggregationValue
	 */
	public void emitAggregationEvent(Class<? extends Action> action, C eventCollector, K eventContainer,
			Event originalEvent, Long timestamp, int windowSize, String ruleActionId, String aggregationKey, Object aggregationValue);
	
	/**
	 * @param eventCollector
	 * @param eventContainer
	 * @param track
	 * @param originalEvent
	 * @param timestamp
	 * @param windowSize
	 * @param ruleActionId
	 * @param aggregationKey
	 */
	public void emitStateTrackingEvent(C eventCollector, K eventContainer, Boolean track,
			Event originalEvent, Long timestamp, int windowSize, String ruleActionId, String aggregationKey);

	/**
	 * Build new event and emit that
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param originalEvent
	 * @param outputEvent
	 */
	public void emitNewEvent(C eventCollector, K eventContainer, Event originalEvent, Event outputEvent);

	/**
	 * Handle emission of a tagged {@link Event}
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param outputEvent
	 */
	public void emitTaggedEvent(C eventCollector, K eventContainer, Event outputEvent);
	
	/**
	 * Handle Omegas 
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param outputEvent
	 */
	public void emitOmegaActions(C eventCollector, K eventContainer, Event outputEvent);

	/**
	 * BETA Code
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param seriesName
	 * @param value
	 */
	public void emitAnomalyAction(C eventCollector, K eventContainer, String seriesName, Number value);
}
