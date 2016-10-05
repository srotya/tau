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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.Queries;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.api.storage.Rules;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.validator.RuleValidator;

/**
 * Persistence manager for {@link Rules}
 * 
 * @author ambud_sharma
 */
public class RulesManager {

	public static final String PARAM_RULE_GROUP_ID = "ruleGroupId";
	private static final String PARAM_RULE_ID = "ruleId";
	private static final Logger logger = Logger.getLogger(RulesManager.class.getCanonicalName());
	private static final String PARAM_TEMPLATE = "template";
	private static RulesManager RULES_MANAGER = new RulesManager();;

	private RulesManager() {
	}

	public static RulesManager getInstance() {
		return RULES_MANAGER;
	}

	/**
	 * @param em
	 * @param dbRule
	 * @param ruleGroup
	 * @return
	 * @throws Exception
	 */
	public Rule createNewRule(EntityManager em, Rules dbRule, RuleGroup ruleGroup) throws Exception {
		if (dbRule == null) {
			logger.info("Rule was null can't save");
			throw new IllegalArgumentException("Rule was null");
		}
		EntityTransaction t = em.getTransaction();
		try {
			t.begin();
			if (ruleGroup == null) {
				logger.severe("Rule group is null");
				throw new Exception("Rule group can't be empty");
			} else {
				dbRule.setRuleGroup(ruleGroup);
			}
			em.persist(dbRule);
			em.flush();
			t.commit();
			logger.info("Created new rule with rule id:" + dbRule.getRuleId());
			return new SimpleRule(dbRule.getRuleId(), "", false, null, new Action[]{});
		} catch (Exception e) {
			if (t.isActive()) {
				t.rollback();
			}
			logger.log(Level.SEVERE, "Failed to create a new rule", e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param dbRule
	 * @param ruleGroup
	 * @param currRule
	 * @param am
	 * @return
	 * @throws Exception
	 */
	public Rule saveRule(EntityManager em, Rules dbRule, RuleGroup ruleGroup, Rule currRule, ApplicationManager am)
			throws Exception {
		if (currRule == null || dbRule == null || ruleGroup == null) {
			logger.info("Rule was null can't save");
			throw new IllegalArgumentException("Rule or rule group was null");
		}
		RuleValidator.getInstance().validate(currRule);
		for (Action action : currRule.getActions()) {
			if (action instanceof TemplatedAlertAction) {
				TemplateManager.getInstance().getTemplate(em, ruleGroup.getRuleGroupId(),
						((TemplatedAlertAction) action).getTemplateId());
			}
		}
		logger.info("Rule is valid attempting to save");
		try {
			em.getTransaction().begin();
			if (dbRule.getRuleGroup() == null) {
				dbRule.setRuleGroup(ruleGroup);
			}
			if (currRule.getRuleId() > 0) {
				dbRule.setRuleId(currRule.getRuleId());
			}
			dbRule = em.merge(dbRule);
			em.flush();
			em.getTransaction().commit();
			currRule.setRuleId(dbRule.getRuleId());
			em.getTransaction().begin();
			dbRule.setRuleContent(RuleSerializer.serializeRuleToJSONString(currRule, false));
			em.merge(dbRule);
			em.flush();
			logger.info("Rule " + dbRule.getRuleId() + ":" + dbRule.getRuleContent() + " saved");
			// publish rule to kafka
			am.getSourcer().sendRule(false, ruleGroup.getRuleGroupId(), dbRule.getRuleContent());
			em.getTransaction().commit();
			logger.info("Completed Transaction for rule " + dbRule.getRuleId() + ":" + dbRule.getRuleContent() + "");
			return currRule;
		} catch (Exception e) {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			logger.log(Level.SEVERE, "Failed to save rule", e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleId
	 * @return
	 */
	protected Rules getRule(EntityManager em, short ruleId) {
		try {
			Rules resultResult = em.createNamedQuery(Queries.RULES_FIND_BY_ID, Rules.class)
					.setParameter(PARAM_RULE_ID, ruleId).getSingleResult();
			return resultResult;
		} catch (Exception e) {
			throw new NoResultException("Rule:" + ruleId + " not found");
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param ruleId
	 * @return
	 * @throws Exception
	 */
	public Rules getRule(EntityManager em, String ruleGroupId, short ruleId) throws Exception {
		try {
			Rules resultResult = em.createNamedQuery(Queries.RULES_FIND_BY_ID_AND_RULE_GROUP, Rules.class)
					.setParameter(PARAM_RULE_ID, ruleId).setParameter(PARAM_RULE_GROUP_ID, ruleGroupId).getSingleResult();
			return resultResult;
		} catch (Exception e) {
			if (e instanceof NoResultException) {
				logger.log(Level.SEVERE, "Rule:" + ruleId + " not found for ruleGroupId:" + ruleGroupId);
			} else {
				logger.log(Level.SEVERE, "Error getting rule:" + ruleId + " for ruleGroupId:" + ruleGroupId, e);
			}
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param templateId
	 * @return
	 * @throws Exception
	 */
	public List<Short> getRuleByTemplateId(EntityManager em, String ruleGroupId, short templateId) throws Exception {
		List<Short> results = em.createNamedQuery(Queries.RULES_BY_TEMPLATE_ID_BY_RULE_GROUP, Short.class)
				.setParameter(PARAM_RULE_GROUP_ID, ruleGroupId)
				.setParameter(PARAM_TEMPLATE, "%\"templateId\":" + templateId + "%").getResultList();
		return results;
	}

	/**
	 * @param em
	 * @param ruleId
	 * @return
	 * @throws Exception
	 */
	protected Rule getRuleObject(EntityManager em, short ruleId) throws Exception {
		Rules rule = getRule(em, ruleId);
		if (rule.getRuleContent() != null) {
			return RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent());
		} else {
			return new SimpleRule(rule.getRuleId(), "", false, null, new Action[] {});
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param ruleId
	 * @param am
	 * @throws Exception
	 */
	public void deleteRule(EntityManager em, String ruleGroupId, short ruleId, ApplicationManager am) throws Exception {
		EntityTransaction transaction = em.getTransaction();
		try {
			Rules rule = getRule(em, ruleGroupId, ruleId);
			if (rule == null) {
				throw new NotFoundException();
			}
			transaction.begin();
			String ruleContent = rule.getRuleContent();
			em.createNamedQuery(Queries.RULES_DELETE_BY_ID).setParameter(PARAM_RULE_ID, ruleId).executeUpdate();
			if (ruleContent != null) {
				am.getSourcer().sendRule(false, ruleGroupId, ruleContent);
			}
			transaction.commit();
			logger.info("Deleted rule:" + ruleId);
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (e instanceof NoResultException) {
				logger.log(Level.SEVERE, "Rule " + ruleId + " not found");
			} else {
				logger.log(Level.SEVERE, "Failed to delete rule", e);
			}
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroup
	 * @param am
	 * @throws Exception
	 */
	public void deleteRules(EntityManager em, RuleGroup ruleGroup, ApplicationManager am) throws Exception {
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			List<Rules> rules = getRules(em, ruleGroup.getRuleGroupId());
			if (rules != null) {
				for (Rules rule : rules) {
					em.remove(rule);
					if (rule.getRuleContent() != null) {
						am.getSourcer().sendRule(false, ruleGroup.getRuleGroupId(), rule.getRuleContent());
					}
					logger.info("Deleting rule:" + rule.getRuleId() + " for ruleGroup id:" + ruleGroup);
				}
			}
			em.flush();
			transaction.commit();
			logger.info("All rules for rule group:" + ruleGroup);
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (!(e instanceof NoResultException)) {
				logger.log(Level.SEVERE, "Failed to delete rule", e);
			}
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param am
	 * @throws Exception
	 */
	public void disableAllRules(EntityManager em, String ruleGroupId, ApplicationManager am) throws Exception {
		try {
			List<Rules> rules = getRules(em, ruleGroupId);
			if (rules == null) {
				throw new NoResultException("No rules for rule group");
			}
			for (Rules rule : rules) {
				try {
					enableDisableRule(em, false, ruleGroupId, rule.getRuleId(), am);
					logger.info("Disabled rule:" + rule.getRuleId());
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Couldn't disable rule:" + rule.getRuleId() + " reason:" + e.getMessage());
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to disable all rules for rule group:" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @return
	 * @throws Exception
	 */
	public List<Rule> getRuleObjects(EntityManager em, String ruleGroupId) throws Exception {
		List<Rule> rules = new ArrayList<>();
		try {
			List<Rules> results = getRules(em, ruleGroupId);
			for (Rules rule : results) {
				if (rule.getRuleContent() != null) {
					rules.add(RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent()));
				} else {
					rules.add(new SimpleRule(rule.getRuleId(), "", false, null, new Action[] {}));
				}
			}
			return rules;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load rule objects for rule group:" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @return
	 * @throws Exception
	 */
	public List<Rules> getRules(EntityManager em, String ruleGroupId) throws Exception {
		RuleGroup ruleGroup = getRuleGroup(em, ruleGroupId);
		if (ruleGroup == null) {
			throw new NoResultException("Rule group not found");
		}
		try {
			List<Rules> result = em.createNamedQuery(Queries.RULES_FIND_ALL_BY_RULE_GROUP_ID, Rules.class)
					.setParameter(PARAM_RULE_GROUP_ID, ruleGroupId).getResultList();
			return result;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load rules for rule group:" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param pretty
	 * @param filter
	 * @return
	 * @throws Exception
	 */
	public String getRuleContents(EntityManager em, String ruleGroupId, boolean pretty, int filter) throws Exception {
		List<Rule> rules = new ArrayList<>();
		try {
			List<Rules> results = getRules(em, ruleGroupId);
			for (Rules rule : results) {
				switch (filter) {
				case 1:
					if (rule.getRuleContent() != null) {
						rules.add(RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent()));
					}
					break;
				case 2:
					if (rule.getRuleContent() != null) {
						SimpleRule ruleObj = RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent());
						if (ruleObj.isActive()) {
							rules.add(ruleObj);
						}
					}
					break;
				case 3:
					if (rule.getRuleContent() != null) {
						SimpleRule ruleObj = RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent());
						if (!ruleObj.isActive()) {
							rules.add(ruleObj);
						}
					}
					break;
				case 4:
					if (rule.getRuleContent() == null) {
						rules.add(new SimpleRule(rule.getRuleId(), "", false, null, new Action[] {}));
					}
					break;
				default:
					if (rule.getRuleContent() != null) {
						rules.add(RuleSerializer.deserializeJSONStringToRule(rule.getRuleContent()));
					} else {
						rules.add(new SimpleRule(rule.getRuleId(), "", false, null, new Action[] {}));
					}
					break;
				}
			}
			return RuleSerializer.serializeRulesToJSONString(rules, pretty);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load rule contents for rule group:" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleState
	 * @param ruleGroupId
	 * @param ruleId
	 * @param am
	 * @return
	 * @throws Exception
	 */
	public Rules enableDisableRule(EntityManager em, boolean ruleState, String ruleGroupId, short ruleId,
			ApplicationManager am) throws Exception {
		try {
			Rules rules = getRule(em, ruleGroupId, ruleId);
			if (rules != null) {
				if (rules.getRuleContent() != null) {
					Rule rule = RuleSerializer.deserializeJSONStringToRule(rules.getRuleContent());
					rule.setActive(ruleState);
					saveRule(em, rules, rules.getRuleGroup(), rule, am);
				} else {
					throw new BadRequestException("Cannot enable/disable empty rule:" + ruleId);
				}
			} else {
				throw new NoResultException("Rule not found");
			}
			return rules;
		} catch (BadRequestException | NoResultException e) {
			throw e;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to enabled disable rule" + ruleId + "\t" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @return
	 * @throws Exception
	 */
	public RuleGroup getRuleGroup(EntityManager em, String ruleGroupId) throws Exception {
		try {
			return em.createNamedQuery(Queries.RULE_GROUP_FIND_BY_ID, RuleGroup.class).setParameter(PARAM_RULE_GROUP_ID, ruleGroupId)
					.getSingleResult();
		} catch (Exception e) {
			if (e instanceof NoResultException) {
				logger.log(Level.SEVERE, "Rule group id:" + ruleGroupId + " not found");
			} else {
				logger.log(Level.SEVERE, "Failed to get rule group id:" + ruleGroupId, e);
			}
			throw e;
		}
	}
}