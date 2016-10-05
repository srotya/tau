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
package com.srotya.tau.wraith.silo.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.validator.AlertTemplateValidator;
import com.srotya.tau.wraith.rules.validator.RuleValidator;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * SQL Database based {@link RulesStore} so that transactional support for Rules
 * can be provided.
 * 
 * @author ambud_sharma
 */
public class SQLRulesStore implements RulesStore, TemplateStore {

	public static final String COLUMN_RULE_GROUP_ID = "rule_group_id";
	public static final String STORE_SQL_URL = "store.sql.url";
	public static final String STORE_SQL_DB = "store.sql.db";
	public static final String TSTORE_SQL_TABLE = "tstore.sql.table";
	public static final String RSTORE_SQL_TABLE = "rstore.sql.table";
	public static final String COLUMN_TEMPLATE_ID = "template_id";
	public static final String COLUMN_TEMPLATE_CONTENT = "template_content";
	public static final String COLUMN_RULE_ID = "rule_id";
	public static final String COLUMN_RULE_CONTENT = "rule_content";
	public static final String RSTORE_RULE_GROUP_FILTER = "rstore.sql.tenant.filter";
	private static final Logger logger = LoggerFactory.getLogger(SQLRulesStore.class);
	private Connection conn;
	private String url;
	private String dbName;
	private String username;
	private String password;
	private String rulesTable;
	private String[] tenants;
	private String templateTable;

	public SQLRulesStore() {
	}

	@Override
	public void initialize(Map<String, String> conf) {
		this.url = conf.get(STORE_SQL_URL);
		this.dbName = conf.get(STORE_SQL_DB);
		this.rulesTable = conf.get(RSTORE_SQL_TABLE);
		this.templateTable = conf.get(TSTORE_SQL_TABLE);
		this.username = conf.get(Constants.STORE_USERNAME);
		this.password = conf.get(Constants.STORE_PASSWORD);
		if (conf.get(RSTORE_RULE_GROUP_FILTER) != null) {
			this.tenants = conf.get(RSTORE_RULE_GROUP_FILTER).toString().split(",");
		}
	}

	@Override
	public void connect() throws IOException {
		try {
			conn = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			throw new IOException(e);
		}
		this.password = null;
	}

	@Override
	public void disconnect() throws IOException {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public Map<String, Map<Short, Rule>> listGroupedRules() throws IOException {
		Map<String, Map<Short, Rule>> rules = new HashMap<>();
		try {
			PreparedStatement st = null;
			if (this.tenants == null) {
				st = conn.prepareStatement("select * from " + dbName + "." + rulesTable + "");
			} else {
				st = conn.prepareStatement("select * from " + dbName + "." + rulesTable + " where " + COLUMN_RULE_GROUP_ID + " in (?)");
				st.setString(1, StringUtils.join(tenants));
			}
			ResultSet resultSet = st.executeQuery();
			int counter = 0;
			while (resultSet.next()) {
				try {
					SimpleRule rule = RuleSerializer
							.deserializeJSONStringToRule(resultSet.getString(COLUMN_RULE_CONTENT));
					short ruleId = resultSet.getShort(COLUMN_RULE_ID);
					String tenantId = resultSet.getString(COLUMN_RULE_GROUP_ID);
					if (tenantId == null) {
						// rules with tenantIds are not allowed
						continue;
					}
					if (rule != null && ruleId == rule.getRuleId()) {
						try {
							RuleValidator.getInstance().validate(rule);
						} catch (ValidationException e) {
							logger.error("Dropping rule:" + rule.getRuleId() + " reason:" + e.getMessage());
							continue;
						}
						if (!rules.containsKey(tenantId)) {
							rules.put(tenantId, new LinkedHashMap<>());
						}
						rules.get(tenantId).put(ruleId, rule);
						counter++;
						logger.debug("Adding rule:" + rule.getRuleId() + "/" + rule.getName());
					} else {
						logger.error("Dropping rule, RuleId(PK) mismatch with Rule content RuleId");
					}
				} catch (Exception e) {
					logger.error("Dropping rule, json parse exception:" + e.getMessage());
				}
			}
			logger.info("Loaded " + counter + " rules from the database");
			resultSet.close();
			st.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return rules;
	}

	@Override
	public Map<Short, AlertTemplate> getAllTemplates() throws IOException {
		Map<Short, AlertTemplate> templateMap = new HashMap<>();
		AlertTemplateValidator validator = new AlertTemplateValidator();
		try {
			PreparedStatement st = null;
			if (this.tenants == null) {
				st = conn.prepareStatement("select * from " + dbName + "." + templateTable + "");
			} else {
				st = conn.prepareStatement("select * from " + dbName + "." + templateTable + " where rule_group_id in (?)");
				st.setString(1, StringUtils.join(tenants));
			}
			ResultSet resultSet = st.executeQuery();
			int counter = 0;
			while (resultSet.next()) {
				try {
					AlertTemplate template = AlertTemplateSerializer
							.deserialize(resultSet.getString(COLUMN_TEMPLATE_CONTENT));
					short templateId = resultSet.getShort(COLUMN_TEMPLATE_ID);
					if (template != null && templateId == template.getTemplateId()) {
						try{
						validator.validate(template);
						} catch (ValidationException e) {
							logger.error("Dropping template:" + template.getTemplateId() + " reason:" + e.getMessage());
							continue;
						}
						templateMap.put(templateId, template);
						counter++;
						logger.debug("Adding template:" + template.getTemplateId() + "/" + template.getTemplateName());
					} else {
						logger.error("Dropping template, TemplateId(PK) mismatch with Template content templateId");
					}
				} catch (Exception e) {
					logger.error("Dropping template, json parse exception:" + e.getMessage());
				}
			}
			logger.info("Loaded " + counter + " templates from the database");
			resultSet.close();
			st.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return templateMap;
	}

}
