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
package com.srotya.tau.api.dao;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.Queries;
import com.srotya.tau.api.storage.RuleGroup;

/**
 * Persistence manager for {@link RuleGroup}s
 * 
 * @author ambud_sharma
 */
public class RuleGroupManager {

	public static final String CREATED_APIKEY = "created_apiKey";
	public static final String DISABLED_APIKEY = "disabled_apiKey";
	public static final String ENABLED_APIKEY = "enabled_apiKey";
	public static final String DELETED_APIKEY = "deleted_apiKey";
	private static final Logger logger = Logger.getLogger(RuleGroupManager.class.getCanonicalName());
	private static final RuleGroupManager RULE_GROUP_MANAGER = new RuleGroupManager();

	private RuleGroupManager() {
	}

	public static RuleGroupManager getInstance() {
		return RULE_GROUP_MANAGER;
	}

	/**
	 * Create rulegroup
	 * 
	 * @param em
	 * @param ruleGroup
	 * @throws Exception
	 */
	public void createRuleGroup(EntityManager em, RuleGroup ruleGroup) throws Exception {
		if (ruleGroup == null) {
			throw new NullPointerException("Rule group can't be empty");
		}
		EntityTransaction t = em.getTransaction();
		try {
			t.begin();
			em.persist(ruleGroup);
			em.flush();
			t.commit();
		} catch (Exception e) {
			if (t.isActive()) {
				t.rollback();
			}
			logger.log(Level.SEVERE, "Failed to create rule group:" + ruleGroup, e);
			throw e;
		}
	}

	public RuleGroup deleteRuleGroup(EntityManager em, String ruleGroupId, ApplicationManager am) throws Exception {
		RuleGroup ruleGroup = getRuleGroup(em, ruleGroupId);
		if (ruleGroup != null) {
			EntityTransaction t = em.getTransaction();
			try {
				RulesManager.getInstance().deleteRules(em, ruleGroup, am);
				TemplateManager.getInstance().deleteTemplates(em, ruleGroup, am);
				t.begin();
				ruleGroup.setRulesTables(null);
				ruleGroup.setTemplates(null);
				em.remove(ruleGroup);
				em.flush();
				t.commit();
				return ruleGroup;
			} catch (Exception e) {
				if (t.isActive()) {
					t.rollback();
				}
				logger.log(Level.SEVERE, "Failed to delete rule group:" + ruleGroup, e);
				throw e;
			}
		} else {
			throw new EntityNotFoundException("Rule group not found");
		}
	}

	/**
	 * Update rule group, only name can be updated
	 * 
	 * @param em
	 * @param ruleGroupId
	 * @param ruleGroupName
	 * @return
	 * @throws Exception
	 */
	public RuleGroup updateRuleGroup(EntityManager em, String ruleGroupId, String ruleGroupName) throws Exception {
		RuleGroup ruleGroup = getRuleGroup(em, ruleGroupId);
		if (ruleGroup != null) {
			EntityTransaction t = em.getTransaction();
			try {
				t.begin();
				ruleGroup.setRuleGroupName(ruleGroupName);
				em.merge(ruleGroup);
				em.flush();
				t.commit();
				return ruleGroup;
			} catch (Exception e) {
				if (t.isActive()) {
					t.rollback();
				}
				logger.log(Level.SEVERE, "Failed to update rule group:" + ruleGroup, e);
				throw e;
			}
		} else {
			throw new EntityNotFoundException("Rule group not found");
		}
	}

	/**
	 * Get all rule groups with rule group ids matching the list
	 * 
	 * @param em
	 * @param ruleGroups
	 * @return
	 * @throws Exception
	 */
	public List<RuleGroup> getRuleGroups(EntityManager em, List<String> ruleGroups) throws Exception {
		return em.createNamedQuery(Queries.RULE_GROUP_FILTERED, RuleGroup.class).setParameter("ruleGroups", ruleGroups)
				.getResultList();
	}

	/**
	 * Get Rule Group by rule group id
	 * 
	 * @param em
	 * @param ruleGroupId
	 * @return
	 * @throws Exception
	 */
	public RuleGroup getRuleGroup(EntityManager em, String ruleGroupId) throws Exception {
		return em.createNamedQuery(Queries.RULE_GROUP_FIND_BY_ID, RuleGroup.class).setParameter(RulesManager.PARAM_RULE_GROUP_ID, ruleGroupId)
				.getSingleResult();
	}

	/**
	 * Get all rule groups by rule group name
	 * 
	 * @param em
	 * @param ruleGroupName
	 * @return
	 * @throws Exception
	 */
	public List<RuleGroup> getTenantsByName(EntityManager em, String ruleGroupName) throws Exception {
		return em.createNamedQuery(Queries.RULE_GROUP_FIND_BY_NAME, RuleGroup.class).setParameter("ruleGroupName", ruleGroupName)
				.getResultList();
	}

	/**
	 * Get all rule groups
	 * 
	 * @param em
	 * @return
	 */
	public List<RuleGroup> getRuleGroups(EntityManager em) {
		return em.createNamedQuery(Queries.RULE_GROUP_FIND_ALL, RuleGroup.class).getResultList();
	}

}