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
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.srotya.tau.api.Queries;
import com.srotya.tau.api.dao.RulesManager;

/**
 * The persistent class for the rules_table database table.
 */
@Entity
@Table(name = "rules_table")
@NamedQueries({ @NamedQuery(name = Queries.RULES_FIND_ALL, query = "SELECT r FROM Rules r"),
		@NamedQuery(name = Queries.RULES_STATS, query = "SELECT r.ruleGroup.ruleGroupName,count(r) FROM Rules r group by r.ruleGroup.ruleGroupName"),
		@NamedQuery(name = Queries.RULES_FIND_ALL_BY_RULE_GROUP_ID, query = "SELECT r FROM Rules r where r.ruleGroup.ruleGroupId=:"+RulesManager.PARAM_RULE_GROUP_ID),
		@NamedQuery(name = Queries.RULES_FIND_BY_ID, query = "SELECT r FROM Rules r where r.ruleId=:ruleId"),
		@NamedQuery(name = Queries.RULES_FIND_BY_ID_AND_RULE_GROUP, query = "SELECT r FROM Rules r where r.ruleId=:ruleId and r.ruleGroup.ruleGroupId=:"+RulesManager.PARAM_RULE_GROUP_ID),
		@NamedQuery(name = Queries.RULES_FIND_ALL_BY_RULE_GROUP_IDS, query = "SELECT r FROM Rules r where r.ruleGroup.ruleGroupId in :ruleGroupIds"),
		@NamedQuery(name = Queries.RULES_FIND_ALL_BY_RULE_GROUP_NAME, query = "SELECT r FROM Rules r where r.ruleGroup.ruleGroupName=:ruleGroupName"),
		@NamedQuery(name = Queries.RULES_DELETE_BY_ID, query = "DELETE FROM Rules r where r.ruleId=:ruleId"),
		@NamedQuery(name = Queries.RULES_LATEST_RULE_ID, query = "SELECT r.ruleId from Rules r order by r.ruleId desc"),
		@NamedQuery(name = Queries.RULES_BY_TEMPLATE_ID_BY_RULE_GROUP, query = "SELECT r.ruleId from Rules r where r.ruleGroup.ruleGroupId=:"+RulesManager.PARAM_RULE_GROUP_ID+" and r.ruleContent like :template"), })
public class Rules implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final int MAX_RULE_LENGTH = 32000;

	@Id
	@Column(name = "rule_id")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private short ruleId;

	@Column(name = "rule_content", length = Rules.MAX_RULE_LENGTH)
	private String ruleContent;

	// bi-directional many-to-one association to Rule Group
	@ManyToOne()
	@JoinColumn(name = "rule_group_id")
	@JsonIgnore
	private RuleGroup ruleGroup;

	public Rules() {
	}

	/**
	 * @return the ruleId
	 */
	public short getRuleId() {
		return ruleId;
	}

	/**
	 * @param ruleId
	 *            the ruleId to set
	 */
	public void setRuleId(short ruleId) {
		this.ruleId = ruleId;
	}

	/**
	 * @return the ruleContent
	 */
	public String getRuleContent() {
		return ruleContent;
	}

	/**
	 * @param ruleContent
	 *            the ruleContent to set
	 */
	public void setRuleContent(String ruleContent) {
		this.ruleContent = ruleContent;
	}

	/**
	 * @return the ruleGroup
	 */
	public RuleGroup getRuleGroup() {
		return ruleGroup;
	}

	/**
	 * @param ruleGroup
	 *            the ruleGroup to set
	 */
	public void setRuleGroup(RuleGroup ruleGroup) {
		this.ruleGroup = ruleGroup;
	}

}