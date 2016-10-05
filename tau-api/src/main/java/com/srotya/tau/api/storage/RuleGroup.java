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
package com.srotya.tau.api.storage;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srotya.tau.api.Queries;
import com.srotya.tau.api.dao.RulesManager;

/**
 * The persistent class for the rule group database table.
 */
@Entity
@Table(name = "rule_group")
@NamedQueries({ @NamedQuery(name = Queries.RULE_GROUP_FIND_ALL, query = "SELECT t FROM RuleGroup t"),
		@NamedQuery(name = Queries.RULE_GROUP_FIND_BY_ID, query = "SELECT t FROM RuleGroup t where t.ruleGroupId=:"+RulesManager.PARAM_RULE_GROUP_ID),
		@NamedQuery(name = Queries.RULE_GROUP_FIND_BY_IDS, query = "SELECT t FROM RuleGroup t where t.ruleGroupId in :ruleGroups"),
		@NamedQuery(name = Queries.RULE_GROUP_FIND_BY_NAMES, query = "SELECT t FROM RuleGroup t where t.ruleGroupName in :ruleGroupNames"),
		@NamedQuery(name = Queries.RULE_GROUP_FIND_BY_NAME, query = "SELECT t FROM RuleGroup t where t.ruleGroupName like :ruleGroupName"),
		@NamedQuery(name = Queries.RULE_GROUP_DELETE_BY_ID, query = "DELETE FROM RuleGroup t where t.ruleGroupId=:"+RulesManager.PARAM_RULE_GROUP_ID),
		@NamedQuery(name = Queries.RULE_GROUP_FILTERED, query = "SELECT t FROM RuleGroup t where t.ruleGroupId IN :rulegroups") })
public class RuleGroup implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final int RULE_GROUP_ID_MAX_SIZE = 50;
	public static final int RULE_GROUP_NAME_MAX_SIZE = 100;

	@Id
	@Column(name = "rule_group_id", length = RULE_GROUP_ID_MAX_SIZE)
	private String ruleGroupId;

	@Column(name = "rule_group_name", length = RULE_GROUP_NAME_MAX_SIZE)
	private String ruleGroupName;

	// bi-directional many-to-one association to RulesTable
	@OneToMany(mappedBy = "ruleGroup", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Rules> rulesTables;

	@OneToMany(mappedBy = "ruleGroup", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<AlertTemplates> templates;
	
	public RuleGroup() {
	}

	/**
	 * @return the rule group id
	 */
	public String getRuleGroupId() {
		return ruleGroupId;
	}

	/**
	 * @return the rule group name
	 */
	public String getRuleGroupName() {
		return ruleGroupName;
	}

	/**
	 * @param rule group name the ruleGroupName to set
	 */
	public void setRuleGroupName(String ruleGroupName) {
		this.ruleGroupName = ruleGroupName;
	}

	/**
	 * @param ruleGroupId the ruleGroupId to set
	 */
	public void setRuleGroupId(String ruleGroupId) {
		this.ruleGroupId = ruleGroupId;
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public List<Rules> getRulesTables() {
		return this.rulesTables;
	}

	/**
	 * @param rulesTables
	 */
	public void setRulesTables(List<Rules> rulesTables) {
		this.rulesTables = rulesTables;
	}

	/**
	 * @return the templates
	 */
	@JsonIgnore
	public List<AlertTemplates> getTemplates() {
		return templates;
	}

	/**
	 * @param templates
	 *            the templates to set
	 */
	public void setTemplates(List<AlertTemplates> templates) {
		this.templates = templates;
	}

	/**
	 * @param rulesTable
	 * @return
	 */
	public Rules addRulesTable(Rules rulesTable) {
		getRulesTables().add(rulesTable);
		rulesTable.setRuleGroup(this);
		return rulesTable;
	}

	/**
	 * @param rulesTable
	 * @return
	 */
	public Rules removeRulesTable(Rules rulesTable) {
		getRulesTables().remove(rulesTable);
		rulesTable.setRuleGroup(null);
		return rulesTable;
	}

	/**
	 * @param alertTemplates
	 * @return
	 */
	public AlertTemplates addTemplates(AlertTemplates alertTemplates) {
		getTemplates().add(alertTemplates);
		alertTemplates.setRuleGroup(this);
		return alertTemplates;
	}

	/**
	 * @param alertTemplates
	 * @return
	 */
	public AlertTemplates removeTemplates(AlertTemplates alertTemplates) {
		getTemplates().remove(alertTemplates);
		alertTemplates.setRuleGroup(null);
		return alertTemplates;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RuleGroup [RuleGroupId=" + ruleGroupId + ", RuleGroupName=" + ruleGroupName + "]";
	}

}