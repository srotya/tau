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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.ActionSerializer;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.ConditionSerializer;

/**
 * Gson type adapter for {@link Rule} to serialize and deserialize rules
 * 
 * @author ambud_sharma
 *
 */
public class RuleSerializer {

	public static final String PROP_PRETTY_JSON = "json.pretty";

	private RuleSerializer() {
	}

	/**
	 * Serialize {@link Rule}s to JSON string
	 * 
	 * @param rules
	 * @return rules as JSON
	 */
	public static String serializeRulesToJSONString(List<Rule> rules, boolean pretty) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.registerTypeAdapter(Action.class, new ActionSerializer());
		gsonBuilder.disableHtmlEscaping();
		if (pretty) {
			gsonBuilder.setPrettyPrinting();
		}
		Gson gson = gsonBuilder.create();
		return gson.toJson(rules);
	}

	/**
	 * Serialize {@link Rule} to JSON string
	 * 
	 * @param rule
	 * @return rule as JSON
	 */
	public static String serializeRuleToJSONString(Rule rule, boolean pretty) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.registerTypeAdapter(Action.class, new ActionSerializer());
		gsonBuilder.disableHtmlEscaping();
		if (pretty) {
			gsonBuilder.setPrettyPrinting();
		}
		Gson gson = gsonBuilder.create();
		return gson.toJson(rule);
	}

	/**
	 * Deserialize {@link Rule}s from JSON
	 * 
	 * @param jsonRule
	 * @return array of ruleObjects
	 */
	public static SimpleRule[] deserializeJSONStringToRules(String jsonRule) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.registerTypeAdapter(Action.class, new ActionSerializer());
		gsonBuilder.disableHtmlEscaping();
		Gson gson = gsonBuilder.create();
		SimpleRule[] rules = gson.fromJson(jsonRule, SimpleRule[].class);
		if(rules!=null) {
			for(SimpleRule rule:rules) {
				sortActionsById(rule);
			}
		}
		return rules;
	}

	/**
	 * Deserialize {@link Rule} from JSON
	 * 
	 * @param jsonRule
	 * @return ruleObject
	 */
	public static SimpleRule deserializeJSONStringToRule(String jsonRule) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.registerTypeAdapter(Action.class, new ActionSerializer());
		gsonBuilder.disableHtmlEscaping();
		Gson gson = gsonBuilder.create();
		SimpleRule rule = gson.fromJson(jsonRule, SimpleRule.class);
		sortActionsById(rule);
		return rule;
	}
	
	/**
	 * Autosort actions by their Id
	 * @param rule
	 */
	public static void sortActionsById(SimpleRule rule) {
		if(rule!=null && rule.getActions()!=null && rule.getActions().size()>0) {
			Collections.sort(rule.getActions(), new Comparator<Action>() {

				@Override
				public int compare(Action o1, Action o2) {
					return Integer.compare(o1.getActionId(), o2.getActionId());
				}
			});
			for (short i = 0; i < rule.getActions().size(); i++) {
				Action action = rule.getActions().get(i);
				action.setActionId(i);
			}
		}
	}

}