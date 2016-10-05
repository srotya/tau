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
package com.srotya.tau.api;

/**
 * Refactored query names
 * 
 * @author ambud_sharma
 */
public class Queries {
	
	public static final String RULES_STATS = "Rules.stats";
	public static final String RULES_LATEST_RULE_ID = "Rules.latestRuleId";
	public static final String RULES_DELETE_BY_ID = "Rules.deleteById";
	public static final String RULES_FIND_ALL_BY_RULE_GROUP_NAME = "Rules.findAllByRuleGroupName";
	public static final String RULES_FIND_ALL_BY_RULE_GROUP_IDS = "Rules.findAllByRuleGroupIds";
	public static final String RULES_FIND_BY_ID = "Rules.findById";
	public static final String RULES_FIND_BY_ID_AND_RULE_GROUP = "Rules.findByIdAndRuleGroup";
	public static final String RULES_FIND_ALL_BY_RULE_GROUP_ID = "Rules.findAllByRuleGroupId";
	public static final String RULES_FIND_ALL = "Rules.findAll";
	public static final String RULE_GROUP_FIND_BY_NAMES = "RuleGroup.findByNames";
	public static final String RULE_GROUP_FIND_BY_NAME = "RuleGroup.findByName";
	public static final String RULE_GROUP_FIND_BY_IDS = "RuleGroup.findByIds";
	public static final String RULE_GROUP_FIND_BY_ID = "RuleGroup.findById";
	public static final String RULE_GROUP_FIND_ALL = "RuleGroup.findAll";
	public static final String RULE_GROUP_DELETE_BY_ID = "RuleGroup.deleteById";
	public static final String RULE_GROUP_FILTERED = "RuleGroup.filterByRuleGroups";
	
	public static final String TEMPLATE_FIND_BY_RULE_GROUP_ID = "Template.findByRuleGroupId";
	public static final String TEMPLATE_FIND_ALL = "Template.findAll";
	public static final String TEMPLATE_DELETE_BY_ID = "Template.deleteById";
	public static final String TEMPLATE_FIND_BY_ID = "Template.findById";
	public static final String RULES_BY_TEMPLATE_ID_BY_RULE_GROUP = "Rules.findByTemplateIdByRuleGroup";
	
	public static final String API_KEYS_BY_RULE_GROUP = "ApiKey.findByRuleGroup";
	public static final String API_KEY_BY_ID = "ApiKey.findById";
	
	private Queries() {
	}

}
