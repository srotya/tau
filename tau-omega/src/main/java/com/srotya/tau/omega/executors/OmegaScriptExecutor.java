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

	public OmegaScriptExecutor(StoreFactory storeFactory) {
		this.storeFactory = storeFactory;
		this.loggerLookup = new HashMap<>();
		this.scriptLookup = new HashMap<>();
	}

	/**
	 * @param conf
	 * @throws Exception
	 */
	public void initialize(Map<String, String> conf) throws Exception {
		initializeScriptingEngines();
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
					filterAndUpdateRule(entry.getKey(), entry2.getValue(), false);
				}
			}
			store.disconnect();
		} catch (IOException e) {
			logger.severe("Failed to load rules from store, reason:" + e.getMessage());
			throw e;
		}
	}

	private void initializeScriptingEngines() throws ScriptException {
		// javascript engine
		nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
		nashornEngine.eval("var t=10");

		// jruby engine
		System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
		jrubyEngine = new ScriptEngineManager().getEngineByName("jruby");
		jrubyEngine.eval("def fact()\n\tn=10\nend");
	}

	/**
	 * @param ruleGroup
	 * @param ruleId
	 * @param actionId
	 * @param event
	 * @return
	 * @throws ScriptException
	 */
	public boolean executeScript(String ruleGroup, short ruleId, short actionId, Event event) throws ScriptException {
		ScriptAction action = scriptLookup.get(Utils.combineRuleActionId(ruleId, actionId));
		if (action != null) {
			Bindings bindings = null;
			OmegaLogger logger = loggerLookup.get(ruleGroup);
			ScriptEngine engine = null;
			boolean result = false;
			switch (action.getLanguage()) {
			case Javascript:
				engine = nashornEngine;
				break;
			case JRuby:
				engine = jrubyEngine;
				break;
			default: // language currently not supported
				break;
			/*
			 * case Jython: jythonEngine.set("event", event);
			 * jythonEngine.set("logger", loggerLookup.get(ruleGroup)); PyObject
			 * eval = jythonEngine.eval(action.getScript()); if (eval.asInt() ==
			 * 0) { result = true; } else { result = false; } break; default:
			 */
			}
			try {
				bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
				bindings.put("event", event);
				bindings.put("logger", logger);
				result = (boolean) engine.eval(action.getScript(), bindings);
			} catch (Exception e) {
				logger.log("ERROR:" + e.getMessage());
				result = false;
			}
			return result;
		}
		return false;
	}

	/**
	 * @param ruleGroup
	 * @param ruleJson
	 * @param delete
	 */
	public void updateRule(String ruleGroup, String ruleJson, boolean delete) {
		SimpleRule rule = RuleSerializer.deserializeJSONStringToRule(ruleJson);
		logger.fine("Processed rule update:" + rule);
		filterAndUpdateRule(ruleGroup, rule, delete);
	}

	/**
	 * @param ruleGroup
	 * @param rule
	 * @param delete
	 */
	private void filterAndUpdateRule(String ruleGroup, Rule rule, boolean delete) {
		for (Action action : rule.getActions()) {
			if (action instanceof ScriptAction) {
				if (!loggerLookup.containsKey(ruleGroup)) {
					loggerLookup.put(ruleGroup, new OmegaLogger(ruleGroup));
				}
				String ruleActionId = Utils.combineRuleActionId(rule.getRuleId(), action.getActionId());
				if (delete) {
					scriptLookup.remove(ruleActionId);
				} else {
					scriptLookup.put(ruleActionId, (ScriptAction) action);
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

	/**
	 * @return the nashornEngine
	 */
	protected ScriptEngine getNashornEngine() {
		return nashornEngine;
	}

	/**
	 * @return the jrubyEngine
	 */
	protected ScriptEngine getJrubyEngine() {
		return jrubyEngine;
	}

}
