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
import javax.ws.rs.NotFoundException;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.Queries;
import com.srotya.tau.api.storage.AlertTemplates;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.rules.validator.AlertTemplateValidator;

/**
 * @author ambud_sharma
 */
public class TemplateManager {

	private static final Logger logger = Logger.getLogger(TemplateManager.class.getName());
	private static final String PARAM_TEMPLATE_ID = "templateId";
	private static final String PARAM_RULE_GROUP_ID = "ruleGroupId";
	private static TemplateManager TEMPLATE_MANAGER = new TemplateManager();

	private TemplateManager() {
	}

	/**
	 * @return
	 */
	public static TemplateManager getInstance() {
		return TEMPLATE_MANAGER;
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param pretty
	 * @return
	 * @throws Exception
	 */
	public String getTemplateContents(EntityManager em, String ruleGroupId, boolean pretty) throws Exception {
		List<AlertTemplate> templates = new ArrayList<>();
		try {
			List<AlertTemplates> results = getTemplates(em, ruleGroupId);
			for (AlertTemplates template : results) {
				if (template.getTemplateContent() != null) {
					templates.add(AlertTemplateSerializer.deserialize(template.getTemplateContent()));
				} else {
					templates.add(new AlertTemplate(template.getTemplateId(), "", "", "", "", ""));
				}
			}
			return AlertTemplateSerializer.serialize(templates, pretty);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to load template contents for rule group:" + ruleGroupId, e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param templateId
	 * @return
	 */
	public AlertTemplates getTemplate(EntityManager em, String ruleGroupId, short templateId) throws Exception {
		try {
			AlertTemplates result = em.createNamedQuery(Queries.TEMPLATE_FIND_BY_ID, AlertTemplates.class)
					.setParameter("templateId", templateId).setParameter("ruleGroupId", ruleGroupId).getSingleResult();
			return result;
		} catch (Exception e) {
			throw new NoResultException("Template:" + templateId + " not found");
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @return
	 */
	public List<AlertTemplates> getTemplates(EntityManager em, String ruleGroupId) {
		try {
			List<AlertTemplates> result = em.createNamedQuery(Queries.TEMPLATE_FIND_BY_RULE_GROUP_ID, AlertTemplates.class)
					.setParameter(PARAM_RULE_GROUP_ID, ruleGroupId).getResultList();
			return result;
		} catch (Exception e) {
			throw new NoResultException("Templates for:" + ruleGroupId + " not found");
		}
	}

	/**
	 * @param em
	 * @param templates
	 * @param ruleGroup
	 * @return
	 * @throws Exception
	 */
	public short createNewTemplate(EntityManager em, AlertTemplates templates, RuleGroup ruleGroup) throws Exception {
		if (templates == null) {
			return -1;
		}
		EntityTransaction t = em.getTransaction();
		try {
			t.begin();
			if (ruleGroup == null) {
				logger.severe("Template group is null");
				return -1;
			} else {
				templates.setRuleGroup(ruleGroup);
			}
			em.persist(templates);
			em.flush();
			t.commit();
			logger.info("Created new template with template id:" + templates.getTemplateId());
			return templates.getTemplateId();
		} catch (Exception e) {
			if (t.isActive()) {
				t.rollback();
			}
			logger.log(Level.SEVERE, "Failed to create a new template", e);
			throw e;
		}
	}

	/**
	 * @param em
	 * @param templates
	 * @param ruleGroup
	 * @param currTemplate
	 * @return
	 * @throws Exception
	 */
	public short saveTemplate(EntityManager em, AlertTemplates templates, RuleGroup ruleGroup, AlertTemplate currTemplate,
			ApplicationManager am) throws Exception {
		if (currTemplate == null || templates == null || ruleGroup == null) {
			logger.info("Template was null can't save");
			return -1;
		}
		AlertTemplateValidator validator = new AlertTemplateValidator();
		validator.validate(currTemplate);
		logger.info("Template is valid attempting to save");
		try {
			em.getTransaction().begin();
			if (templates.getRuleGroup() == null) {
				templates.setRuleGroup(ruleGroup);
			}
			if (currTemplate.getTemplateId() > 0) {
				templates.setTemplateId(currTemplate.getTemplateId());
			}
			templates = em.merge(templates);
			em.flush();
			em.getTransaction().commit();
			currTemplate.setTemplateId(templates.getTemplateId());
			em.getTransaction().begin();
			templates.setTemplateContent(AlertTemplateSerializer.serialize(currTemplate, false));
			em.merge(templates);
			em.flush();
			logger.info("Template " + templates.getTemplateId() + ":" + templates.getTemplateContent() + " saved");
			// publish template to kafka
			
			am.getSourcer().sendTemplate(false, ruleGroup.getRuleGroupId(), templates.getTemplateContent());
			em.getTransaction().commit();
			logger.info("Completed Transaction for template " + templates.getTemplateId() + ":"
					+ templates.getTemplateContent() + "");
			return templates.getTemplateId();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
			throw e;
		}
	}

	/**
	 * @param em
	 * @param templateId
	 * @return
	 * @throws Exception
	 */
	public AlertTemplate getTemplateObj(EntityManager em, String ruleGroupId, short templateId) throws Exception {
		AlertTemplates template = getTemplate(em, ruleGroupId, templateId);
		if (template.getTemplateContent() != null) {
			return AlertTemplateSerializer.deserialize(template.getTemplateContent());
		} else {
			return new AlertTemplate(templateId, "", "", "", "", "");
		}
	}

	/**
	 * @param em
	 * @param ruleGroupId
	 * @param templateId
	 * @throws Exception
	 */
	public void deleteTemplate(EntityManager em, String ruleGroupId, short templateId, ApplicationManager am)
			throws Exception {
		EntityTransaction transaction = em.getTransaction();
		RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
		try {
			AlertTemplates template = getTemplate(em, ruleGroupId, templateId);
			if (template == null) {
				throw new NotFoundException();
			}
			List<Short> result = null;
			try {
				result = RulesManager.getInstance().getRuleByTemplateId(em, ruleGroupId, templateId);
			} catch (Exception e) {
			}
			if(result!=null && result.size()>0) {
				throw new Exception("Can't delete template when it has rules referring to it:"+result.toString());
			}
			transaction.begin();
			String templateContent = template.getTemplateContent();
			em.createNamedQuery(Queries.TEMPLATE_DELETE_BY_ID).setParameter(PARAM_TEMPLATE_ID, templateId)
					.executeUpdate();
			if (templateContent != null) {
				am.getSourcer().sendTemplate(true, template.getRuleGroup().getRuleGroupId(), templateContent);
			}
			transaction.commit();
			logger.info("Deleted template:" + templateId);
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (e instanceof NoResultException) {
				logger.log(Level.SEVERE, "Template " + templateId + " not found");
			} else {
				logger.log(Level.SEVERE, "Failed to delete template", e);
			}
			throw e;
		}
	}

	/**
	 * @param em
	 * @param ruleGroup
	 * @throws Exception
	 */
	public void deleteTemplates(EntityManager em, RuleGroup ruleGroup, ApplicationManager am) throws Exception {
		EntityTransaction transaction = em.getTransaction();
		try {
			transaction.begin();
			List<AlertTemplates> templates = getTemplates(em, ruleGroup.getRuleGroupId());
			if (templates != null) {
				for (AlertTemplates template : templates) {
					List<Short> result = null;
					try {
						result = RulesManager.getInstance().getRuleByTemplateId(em, ruleGroup.getRuleGroupId(), template.getTemplateId());
					} catch (Exception e) {
					}
					if(result!=null && result.size()>0) {
						throw new Exception("Can't delete template when it has rules referring to it:"+result.toString());
					}
					em.remove(template);
					if (template.getTemplateContent() != null) {
						am.getSourcer().sendTemplate(true, template.getRuleGroup().getRuleGroupId(), template.getTemplateContent());
					}
					logger.info("Deleting template:" + template.getTemplateId() + " for rule group id:" + ruleGroup);
				}
			}
			em.flush();
			transaction.commit();
			logger.info("All templates for rule group:" + ruleGroup);
		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			if (!(e instanceof NoResultException)) {
				logger.log(Level.SEVERE, "Failed to delete template", e);
			}
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