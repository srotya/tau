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
package com.srotya.tau.ui;

/**
 * Refactored query names
 * 
 * @author ambud_sharma
 */
public class Queries {
	
	public static final String RULES_STATS = "Rules.stats";
	public static final String RULES_ENABLE_DISABLE_RULE = "Rules.enableDisableRule";
	public static final String RULES_LATEST_RULE_ID = "Rules.latestRuleId";
	public static final String RULES_DELETE_BY_ID = "Rules.deleteById";
	public static final String RULES_FIND_ALL_BY_TENANT_NAME = "Rules.findAllByTenantName";
	public static final String RULES_FIND_ALL_BY_TENANT_IDS = "Rules.findAllByTenantIds";
	public static final String RULES_FIND_BY_ID = "Rules.findById";
	public static final String RULES_FIND_BY_ID_AND_TENANT = "Rules.findByIdAndTenant";
	public static final String RULES_FIND_ALL_BY_TENANT_ID = "Rules.findAllByTenantId";
	public static final String RULES_FIND_ALL = "Rules.findAll";
	public static final String TENANT_FIND_BY_NAMES = "Tenant.findByNames";
	public static final String TENANT_FIND_BY_NAME = "Tenant.findByName";
	public static final String TENANT_FIND_BY_IDS = "Tenant.findByIds";
	public static final String TENANT_FIND_BY_ID = "Tenant.findById";
	public static final String TENANT_FIND_ALL = "Tenant.findAll";
	public static final String TENANT_DELETE_BY_ID = "Tenant.deleteById";

	
	private Queries() {
	}

}
