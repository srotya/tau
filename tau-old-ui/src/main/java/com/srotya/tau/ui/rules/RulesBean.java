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
package com.srotya.tau.ui.rules;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.event.CloseEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.srotya.tau.ui.ApplicationManager;
import com.srotya.tau.ui.UserBean;
import com.srotya.tau.ui.storage.Tenant;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

/**
 * JSF Rule Bean
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "rb")
@SessionScoped
public class RulesBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(RulesBean.class.getName());
	@ManagedProperty(value = "#{am}")
	private ApplicationManager am;
	@ManagedProperty(value = "#{ub}")
	private UserBean ub;
	private Rule currRule;
	private boolean editable;
	private boolean editRule;

	public RulesBean() {
	}

	@PostConstruct
	public void init() {
		currRule = new SimpleRule((short) 0, "", true, null, new Action[0]);
	}

	public static List<String> groupsToTenantString(List<Tenant> tenants) {
		List<String> tenantId = new ArrayList<>();
		for (Tenant tenant : tenants) {
			tenantId.add(tenant.getTenantId());
		}
		return tenantId;
	}

	public void addRule() {
		try {
			short ruleId = RulesManager.getInstance().createNewRule(ub, ub.getTenant());
			if (ruleId > 0) {
				changeCurrentRule(ruleId);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Couldn't create new empty rule", e);
		}
	}

	public void saveRule() {
		if (currRule == null) {
			logger.info("Rule was null can't save");
			return;
		}
		try {
			short ruleId = RulesManager.getInstance().saveRule(ub, ub.getTenant(), currRule);
			if (ruleId > 0) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage("Rule " + currRule.getName() + " successfully saved"));
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Rule not saved", "Rule is not valid"));
			}
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Saving rule failed", e.getMessage()));
		}
	}

	public void print() {
		System.out.println(currRule);
	}

	/**
	 * @return the currRule
	 */
	public Rule getCurrRule() {
		return currRule;
	}

	/**
	 * @param currRule
	 *            the currRule to set
	 */
	public void setCurrRule(Rule currRule) {
		this.currRule = currRule;
	}

	/**
	 * @return the rules
	 */
	public List<Rule> getRules() {
		try {
			return RulesManager.getInstance().getRuleObjects(ub, ub.getTenant().getTenantId());
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	/**
	 * @return the editable
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * @param editable
	 *            the editable to set
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	/**
	 * @return json form of rule
	 */
	public String getJson() {
		if (currRule == null) {
			return "{}";
		} else {
			return ruleToJson(currRule);
		}
	}

	public String ruleToJson(Rule rule) {
		return RuleSerializer.serializeRuleToJSONString(rule, true).replace("\n", "<br/>").replaceAll("\\s", "&nbsp;");
	}

	public void enableDisableRule(short ruleId) {
		try {
			Rule ruleObj = RulesManager.getInstance().getRule(ub, ub.getTenant().getTenantId(), ruleId);
			if (ruleObj.isActive()) {
				RulesManager.getInstance().enableDisableRule(ub, false, ub.getTenant().getTenantId(), ruleId);
			} else {
				RulesManager.getInstance().enableDisableRule(ub, true, ub.getTenant().getTenantId(), ruleId);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Couldn'tenable disable rule:" + ruleId, e);
		}
	}

	public void deleteRule(short ruleId) {
		try {
			RulesManager.getInstance().deleteRule(ub, ub.getTenant().getTenantId(), ruleId);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Couldn't delete rule:" + ruleId, e);
		}
	}

	public void handleClose(CloseEvent event) {
		editRule = false;
	}

	public StreamedContent getFile() {
		if (currRule != null) {
			return new DefaultStreamedContent(
					new ByteArrayInputStream(RuleSerializer.serializeRuleToJSONString(currRule, true).getBytes()),
					"text/json", "rule-" + ((currRule.getName() != null && !currRule.getName().isEmpty())
							? currRule.getName() : currRule.getRuleId()) + ".json");
		} else {
			return new DefaultStreamedContent(new ByteArrayInputStream("".getBytes()), "text/json", "empty.json");
		}
	}

	public void changeCurrentRule(Short ruleId) {
		if (ruleId != null) {
			try {
				currRule = RulesManager.getInstance().getRule(ub, ub.getTenant().getTenantId(), ruleId);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			if (currRule == null) {
				currRule = new SimpleRule((short) ruleId, "", true, null, new Action[0]);
			}
			editRule = true;
			System.out.println("selected rule:" + currRule);

			FacesContext context = FacesContext.getCurrentInstance();
			ExpressionFactory expressionFactory = context.getApplication().getExpressionFactory();
			ELContext elContext = context.getELContext();
			ValueExpression vex = expressionFactory.createValueExpression(elContext, "#{cb}", ConditionBean.class);
			ConditionBean result = (ConditionBean) vex.getValue(elContext);
			result.buildTreeFromRule();

			vex = expressionFactory.createValueExpression(elContext, "#{ab}", ActionBean.class);
			ActionBean ab = (ActionBean) vex.getValue(elContext);
			ab.loadActions();

			RequestContext.getCurrentInstance().execute("location.reload();");
		}
	}

	/**
	 * @return the ub
	 */
	public UserBean getUb() {
		return ub;
	}

	/**
	 * @param ub
	 *            the ub to set
	 */
	public void setUb(UserBean ub) {
		this.ub = ub;
	}

	/**
	 * @return the am
	 */
	public ApplicationManager getAm() {
		return am;
	}

	/**
	 * @param am
	 *            the am to set
	 */
	public void setAm(ApplicationManager am) {
		this.am = am;
	}

	/**
	 * @return the editRule
	 */
	public boolean isEditRule() {
		return editRule;
	}

}