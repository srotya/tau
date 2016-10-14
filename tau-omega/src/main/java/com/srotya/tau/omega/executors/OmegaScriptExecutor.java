/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.omega.executors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.srotya.tau.omega.OmegaLogger;
import com.srotya.tau.omega.ScriptAction;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.omega.OmegaAction;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.StoreFactory;

/**
 * @author ambudsharma
 */
public class OmegaScriptExecutor {

	private static final Logger logger = Logger.getLogger(OmegaScriptExecutor.class.getName());
	private ScriptEngine nashornEngine;
	private Map<String, ScriptAction> scriptLookup;
	private Map<String, OmegaLogger> loggerLookup;
	private StoreFactory storeFactory;
	private ScriptEngine jrubyEngine;
	private ScriptEngine jythonEngine;

	public OmegaScriptExecutor(StoreFactory storeFactory) {
		this.storeFactory = storeFactory;
		this.loggerLookup = new HashMap<>();
	}

	/**
	 * @param conf
	 * @throws Exception
	 */
	public void initialize(Map<String, String> conf) throws Exception {
		nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
		jrubyEngine = new ScriptEngineManager().getEngineByName("jruby");
		jythonEngine = new ScriptEngineManager().getEngineByName("jruby");
		RulesStore store;
		try {
			store = storeFactory.getRulesStore(conf.get(Constants.RSTORE_TYPE), conf);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw e;
		}
		try {
			store.connect();
			Map<String, Map<Short, Rule>> rules = store.listGroupedRules();
			for (Entry<String, Map<Short, Rule>> entry : rules.entrySet()) {
				for (Entry<Short, Rule> entry2 : entry.getValue().entrySet()) {
					filterAndUpdateRule(entry.getKey(), entry2.getValue());
				}
			}
			store.disconnect();
		} catch (IOException e) {
			logger.severe("Failed to load rules from store, reason:" + e.getMessage());
			throw e;
		}
	}

	/**
	 * @param ruleGroup
	 * @param ruleId
	 * @param actionId
	 * @param inputEvent
	 * @return
	 * @throws ScriptException
	 */
	public boolean executeScript(String ruleGroup, short ruleId, short actionId, Event inputEvent)
			throws ScriptException {
		ScriptAction action = scriptLookup.get(Utils.combineRuleActionId(ruleId, actionId));
		if (action != null) {
			Bindings bindings = nashornEngine.getBindings(ScriptContext.ENGINE_SCOPE);
			bindings.put("event", inputEvent);
			bindings.put("logger", loggerLookup.get(ruleGroup));
			boolean result = false;
			switch (action.getLanguage()) {
			case Javascript:
				result = (boolean) nashornEngine.eval(action.getScript());
				break;
			case JRuby:
				result = (boolean) jrubyEngine.eval(action.getScript());
				break;
			case Jython:
				result = (boolean) jythonEngine.eval(action.getScript());
				break;
			default:
			}
			return result;
		}
		return false;
	}

	/**
	 * @param ruleGroup
	 * @param ruleJson
	 */
	public void updateRule(String ruleGroup, String ruleJson) {
		SimpleRule rule = RuleSerializer.deserializeJSONStringToRule(ruleJson);
		filterAndUpdateRule(ruleGroup, rule);
	}

	/**
	 * @param ruleGroup
	 * @param rule
	 */
	private void filterAndUpdateRule(String ruleGroup, Rule rule) {
		for (Action action : rule.getActions()) {
			if (action instanceof OmegaAction) {
				if (!loggerLookup.containsKey(ruleGroup)) {
					loggerLookup.put(ruleGroup, new OmegaLogger(ruleGroup));
				}
			}
		}
	}

	/**
	 * @return the scriptLookup
	 */
	public Map<String, ScriptAction> getScriptLookup() {
		return scriptLookup;
	}

	/**
	 * @return the loggerLookup
	 */
	public Map<String, OmegaLogger> getLoggerLookup() {
		return loggerLookup;
	}

	/**
	 * @return the storeFactory
	 */
	public StoreFactory getStoreFactory() {
		return storeFactory;
	}

}
