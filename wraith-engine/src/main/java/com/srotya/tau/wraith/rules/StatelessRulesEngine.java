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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.srotya.tau.wraith.Configurable;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;
import com.srotya.tau.wraith.PerformantException;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.AggregationAction;
import com.srotya.tau.wraith.rules.validator.RuleValidator;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.StoreFactory;

/**
 * A simple single threaded rule engine implementation for Wraith. This
 * implementation can then be combined with a stream / batch processing
 * framework to create a working Real-time or passive Rules-Engine for
 * {@link Event}s.<br>
 * <br>
 * 
 * The rules engine connects to a configured {@link RulesStore} for initial rule
 * loading but any subsequent changes to rules are delivered as events, removing
 * need for any future external calls or I/O blocks.<br>
 * This a mechanism guarantees that all the {@link StatelessRulesEngine}
 * instances operate in sync of the actual rules states and that periodic data
 * store lookups to check changes is never needed.<br>
 * <br>
 * 
 * The data-structures used are not thread-safe therefore all methods are
 * expected to be called synchronously.
 * 
 * @author ambud_sharma
 */
public class StatelessRulesEngine<K, C> implements Configurable {

	private static final Logger logger = LoggerFactory.getLogger(StatelessRulesEngine.class);
	public static final String ALL_RULEGROUP = "all";
	private Map<String, Map<Short, Rule>> ruleGroupMap;
	private RulesEngineCaller<K, C> caller;
	private EventFactory eventFactory;
	private StoreFactory storeFactory;
	private int hashSize;

	public StatelessRulesEngine(RulesEngineCaller<K, C> caller, EventFactory eventFactory, StoreFactory storeFactory) {
		this.caller = caller;
		this.eventFactory = eventFactory;
		this.storeFactory = storeFactory;
	}

	/**
	 * Load {@link Rule}s into the engine on start
	 * 
	 * @param conf
	 * @throws Exception
	 */
	@Override
	public void initialize(Map<String, String> conf) throws Exception {
		hashSize = Integer.parseInt(conf.getOrDefault(Constants.RULE_HASH_INIT_SIZE, Constants.DEFAULT_RULE_HASH_SIZE));
		this.ruleGroupMap = new HashMap<>(hashSize);
		RulesStore store = null;
		try {
			store = storeFactory.getRulesStore(conf.get(Constants.RSTORE_TYPE), conf);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw e;
		}
		try {
			store.connect();
			this.ruleGroupMap.putAll(store.listGroupedRules());
			store.disconnect();
		} catch (IOException e) {
			logger.error("Failed to load rules from store, reason:" + e.getMessage());
			throw e;
		}
	}

	/**
	 * Updates rule and returns the old rule
	 * 
	 * @param ruleMap
	 * @param ruleJson
	 * @param delete
	 * @return oldRule
	 * @throws ValidationException
	 */
	public static Rule updateRuleMap(Map<Short, Rule> ruleMap, String ruleJson, boolean delete)
			throws ValidationException {
		SimpleRule rule = RuleSerializer.deserializeJSONStringToRule(ruleJson);
		try {
			RuleValidator.getInstance().validate(rule);
		} catch (ValidationException e) {
			// ignore rules that don't pass validation
			throw e;
		}
		if (!delete) {
			return ruleMap.put(rule.getRuleId(), rule);
		} else {
			return ruleMap.remove(rule.getRuleId());
		}
	}

	/**
	 * Rule updates are delivered synchronously by invoking this method.
	 * 
	 * @param ruleJson
	 */
	public void updateRule(String ruleGroup, String ruleJson, boolean delete) throws Exception {
		if (ruleGroup == null) {
			throw new PerformantException("Supplied rule group is null");
		}
		Map<Short, Rule> ruleMap = ruleGroupMap.get(ruleGroup);
		if (ruleMap == null) {
			ruleMap = new LinkedHashMap<>(hashSize);
			ruleGroupMap.put(ruleGroup, ruleMap);
		}
		updateRuleMap(ruleMap, ruleJson, delete);
	}

	/**
	 * @param eventCollector
	 * @param eventContainer
	 * @param event
	 */
	public void evaluateRules(C eventCollector, K eventContainer, Event event) {
		evaluateEventAgainstGroupedRules(eventCollector, eventContainer, event);
	}

	/**
	 * Evaluates all loaded rules against this event, one rule at a time.<br>
	 * <br>
	 * Deactive rules are ignored, if an {@link Event} matches a {@link Rule}
	 * then the {@link Action} of that rule are applied 1 {@link Action} at a
	 * time.
	 * 
	 * @param event
	 */
	public void evaluateEventAgainstGroupedRules(C eventCollector, K eventContainer, Event event) {
		Object rg = event.getHeaders().get(Constants.FIELD_RULE_GROUP);
		Map<Short, Rule> rules;
		String ruleGroup;
		// Evaluate rules for this event type
		if (rg != null && !rg.toString().equalsIgnoreCase(ALL_RULEGROUP)) {
			ruleGroup = rg.toString();
			rules = ruleGroupMap.get(ruleGroup);
			iterateAndEvaluate(eventCollector, eventContainer, event, rules, ruleGroup);
		} else {
			event.getHeaders().put(Constants.FIELD_RULE_GROUP, ALL_RULEGROUP);
		}
		// Evaluate global rules
		ruleGroup = ALL_RULEGROUP;
		rules = ruleGroupMap.get(ruleGroup);
		iterateAndEvaluate(eventCollector, eventContainer, event, rules, ruleGroup);
	}

	/**
	 * @param eventCollector
	 * @param eventContainer
	 * @param event
	 * @param rules
	 * @param ruleGroup
	 */
	protected void iterateAndEvaluate(C eventCollector, K eventContainer, Event event, Map<Short, Rule> rules,
			String ruleGroup) {
		if (rules != null) {
			for (Short ruleId : rules.keySet()) {
				Rule rule = rules.get(ruleId);
				evaluateEventAgainstRule(ruleGroup, rule, eventCollector, eventContainer, event);
			}
		}
	}

	/**
	 * 
	 * @param rule
	 * @param eventCollector
	 * @param eventContainer
	 * @param event
	 */
	public void evaluateEventAgainstRule(String ruleGroup, Rule rule, C eventCollector, K eventContainer, Event event) {
		if (!rule.isActive()) {
			logger.debug("Rule:" + rule.getRuleId() + " is deactive");
			return;
		}
		long ruleStartTime = System.nanoTime();
		long conditionTime = System.nanoTime();
		boolean result = rule.getCondition().matches(event);
		conditionTime = System.nanoTime() - conditionTime;
		if (result) {
			caller.reportRuleHit(ruleGroup, rule.getRuleId());
			List<Action> actions = rule.getActions();
			for (Action action : actions) {
				applyRuleAction(eventCollector, eventContainer, event, ruleGroup, rule, action);
			}
		} else {
			caller.handleRuleNoMatch(eventCollector, eventContainer, event, rule);
		}
		caller.reportRuleEfficiency(ruleGroup, rule.getRuleId(), System.nanoTime() - ruleStartTime);
		caller.reportConditionEfficiency(ruleGroup, rule.getRuleId(), conditionTime);
	}

	/**
	 * Apply a give {@link Rule} {@link Action} on a {@link Event}
	 * 
	 * @param eventCollector
	 * @param eventContainer
	 * @param event
	 * @param ruleGroup
	 * @param rule
	 * @param action
	 */
	protected void applyRuleAction(C eventCollector, K eventContainer, Event event, String ruleGroup, Rule rule,
			Action action) {
		short ruleId = rule.getRuleId();
		Event outputEvent = action.actOnEvent(event);
		if (outputEvent == null) {
			Event actionErrorEvent = eventFactory.buildEvent();
			Map<String, Object> errorHeaders = actionErrorEvent.getHeaders();
			errorHeaders.put(Constants.HEADER_EVENT_TYPE, Constants.ERROR_EVENT_TYPE);
			errorHeaders.put(Constants.HEADER_EVENT_ERROR_TYPE, Constants.ACTION_FAIL);
			errorHeaders.put(Constants.HEADER_EVENT_ERROR_FIELD, ruleId);
			errorHeaders.put(Constants.HEADER_EVENT_ERROR_VALUE, action.getActionId());
			actionErrorEvent.setBody(Utils.eventToBytes(event));
			caller.emitActionErrorEvent(eventCollector, eventContainer, actionErrorEvent);
			return;
		}

		long timestamp = (long) outputEvent.getHeaders().get(Constants.FIELD_TIMESTAMP);

		switch (action.getActionType()) {
		case RAW_ALERT:
			caller.emitRawAlert(eventCollector, eventContainer, outputEvent, ruleId, action.getActionId(),
					outputEvent.getHeaders().get(Constants.FIELD_ALERT_TARGET).toString(),
					outputEvent.getHeaders().get(Constants.FIELD_ALERT_MEDIA).toString());
			break;
		case TEMPLATED_ALERT:
			caller.emitTemplatedAlert(eventCollector, eventContainer, outputEvent, ruleGroup, ruleId,
					action.getActionId(), rule.getName(),
					(short) outputEvent.getHeaders().get(Constants.FIELD_ALERT_TEMPLATE_ID), timestamp);
			break;
		case AGGREGATION:
			// find the correct stream id based on the aggregation action class
			String ruleActionId = Utils.combineRuleActionId(ruleId, action.getActionId());
			caller.emitAggregationEvent(action.getClass(), eventCollector, eventContainer, event, timestamp,
					((AggregationAction) action).getAggregationWindow(), ruleActionId,
					outputEvent.getHeaders().get(Constants.FIELD_AGGREGATION_KEY).toString(),
					outputEvent.getHeaders().get(Constants.FIELD_AGGREGATION_VALUE));
			event.getHeaders().remove(Constants.FIELD_AGGREGATION_KEY);
			event.getHeaders().remove(Constants.FIELD_AGGREGATION_VALUE);
			break;
		case STATE:
			// find the correct stream id based on the aggregation action class
			String stateRuleActionId = Utils.combineRuleActionId(ruleId, action.getActionId());
			caller.emitStateTrackingEvent(eventCollector, eventContainer,
					(Boolean) event.getHeaders().get(Constants.FIELD_STATE_TRACK), event, timestamp,
					((AggregationAction) action).getAggregationWindow(), stateRuleActionId,
					outputEvent.getHeaders().get(Constants.FIELD_AGGREGATION_KEY).toString());
			event.getHeaders().remove(Constants.FIELD_AGGREGATION_KEY);
			event.getHeaders().remove(Constants.FIELD_STATE_TRACK);
			break;
		case NEW:
			outputEvent.getHeaders().put(Constants.FIELD_RULE_ID, ruleId);
			caller.emitNewEvent(eventCollector, eventContainer, event, outputEvent);
			break;
		case TAG:
			caller.emitTaggedEvent(eventCollector, eventContainer, outputEvent);
			break;
		case OMEGA:
			caller.emitOmegaActions(eventCollector, eventContainer, ruleGroup, timestamp, ruleId, action.getActionId(),
					outputEvent);
			break;
		case ANOMD:
			caller.emitAnomalyAction(eventCollector, eventContainer,
					outputEvent.getHeaders().get(Constants.FIELD_ANOMALY_SERIES).toString(),
					(Number) outputEvent.getHeaders().get(Constants.FIELD_ANOMALY_VALUE));
			break;
		default:
			break;
		}
	}

	/**
	 * @return the ruleGroupMap
	 */
	public Map<String, Map<Short, Rule>> getRuleGroupMap() {
		return ruleGroupMap;
	}

	/**
	 * @return the hashSize
	 */
	public int getHashSize() {
		return hashSize;
	}
}