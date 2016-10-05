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
package com.srotya.tau.dengine.bolts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * Rules store for unit tests
 * 
 * @author ambud_sharma
 */
public class TestStore implements RulesStore, TemplateStore {

	private Map<String, Map<Short, Rule>> ruleMap;
	private Map<Short, AlertTemplate> templates;

	public TestStore() {
		ruleMap = new HashMap<>();
		templates = new HashMap<>();
	}

	@Override
	public void initialize(Map<String, String> conf) {
		String rulesJson = conf.get(TestAlertingEngineBolt.RULES_CONTENT);
		if (rulesJson != null) {
			Rule[] ruleList = RuleSerializer.deserializeJSONStringToRules(rulesJson);
			System.out.println("Rule content:" + rulesJson + "\t" + ruleList.length);
			Map<Short, Rule> rules = new HashMap<>();
			ruleMap.put("all", rules);
			for (Rule rule : ruleList) {
				rules.put(rule.getRuleId(), rule);
			}
		}
		String templateJson = conf.get(TestAlertingEngineBolt.TEMPLATE_CONTENT);
		if (templateJson != null) {
			AlertTemplate[] alertTemplates = AlertTemplateSerializer.deserializeArray(templateJson);
			for (AlertTemplate template : alertTemplates) {
				System.out.println("Template content:" + template + "\t" + alertTemplates.length);
				templates.put(template.getTemplateId(), template);
			}
		}
	}

	@Override
	public void connect() throws IOException {
	}

	@Override
	public void disconnect() throws IOException {
	}

	@Override
	public Map<String, Map<Short, Rule>> listGroupedRules() throws IOException {
		return ruleMap;
	}

	@Override
	public Map<Short, AlertTemplate> getAllTemplates() throws IOException {
		return templates;
	}

}