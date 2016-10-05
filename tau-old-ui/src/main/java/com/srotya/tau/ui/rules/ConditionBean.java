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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.srotya.tau.ui.Utils;
import com.srotya.tau.wraith.conditions.AbstractSimpleCondition;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.logical.AndCondition;
import com.srotya.tau.wraith.conditions.logical.ComplexCondition;
import com.srotya.tau.wraith.conditions.logical.OrCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanEqualToCondition;
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanEqualToCondition;
import com.srotya.tau.wraith.conditions.relational.NumericCondition;
import com.srotya.tau.wraith.rules.Rule;

/**
 * JSF Condition Bean
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "cb")
@SessionScoped
public class ConditionBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ConditionBean.class.getName());
	private Condition condition;

	private String headerKey = "";
	private String matchValue = "";
	private boolean numeric =  true;
	private TreeNode root;
	private TreeNode selectedNode;
	private String conditionType;
	@ManagedProperty(value = "#{rb}")
	private RulesBean ruleBean;

	public ConditionBean() {
		root = new DefaultTreeNode("", null);
	}

	public void addCondition(boolean complex) {
		if (selectedNode != null) {
			if (complex) {
				addChildCondition(selectedNode, new AndCondition(null));
			} else {
				addChildCondition(selectedNode, new EqualsCondition(null, null));
			}
		} else {
			if (complex) {
				addChildCondition(root, new AndCondition(null));
			} else {
				addChildCondition(root, new EqualsCondition(null, null));
			}
		}
	}

	public void addChildCondition(TreeNode parent, Condition condition) {
		if (parent == root) {
			if (root.getChildCount() == 1) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Not allowed", "There can be only 1 root condition"));
				return;
			}
			DefaultTreeNode childNode = new DefaultTreeNode(condition, selectedNode);
			childNode.setExpanded(true);
			if (condition instanceof ComplexCondition) {
				selectedNode = childNode;
				childNode.setSelected(true);
			}
			parent.getChildren().add(childNode);
		} else {
			Condition temp = ((Condition) parent.getData());
			if (temp instanceof AbstractSimpleCondition) {
				// can't add to a simple condition
				logger.info("Please select a complex condition to add conditions to.");
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Not allowed", "Sub-conditions can only be added to Complex Conditions"));
			} else {
				DefaultTreeNode childNode = new DefaultTreeNode(condition, selectedNode);
				childNode.setExpanded(true);
				if (condition instanceof ComplexCondition) {
					selectedNode.setSelected(false);
					selectedNode = childNode;
					childNode.setSelected(true);
				}
				parent.getChildren().add(childNode);
			}
		}
	}

	public int removeCondition() {
		if (selectedNode != null && selectedNode != root && selectedNode.getParent() != null) {
			TreeNode parent = selectedNode.getParent();
			Iterator<TreeNode> iterator = parent.getChildren().iterator();
			int i = 0;
			while (iterator.hasNext()) {
				if (iterator.next() == selectedNode) {
					iterator.remove();
					break;
				}
				i++;
			}
			return i;
		} else {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not allowed", "Please select a conditon to remove"));
		}
		return -1;
	}

	public void addConditionToInternal(boolean complex) {
		if (complex) {
			if (condition == null) {
				condition = new AndCondition(null);
			} else {
				addChildCondition(new AndCondition(null));
			}
		} else {
			if (condition instanceof ComplexCondition) {
				addChildCondition(new EqualsCondition(null, null));
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage("Can't add a child condition to a simple condition"));
			}
		}
	}

	public void addChildCondition(Condition type) {
		ComplexCondition ccondition = ((ComplexCondition) condition);
		List<Condition> conditions = null;
		if (ccondition.getConditions() == null) {
			conditions = new ArrayList<>();
			ccondition.setConditions(conditions);
		} else {
			conditions = ccondition.getConditions();
		}
		conditions.add(type);
	}

	public void selectionChange() {
		if (selectedNode != null) {
			Condition condition = ((Condition) selectedNode.getData());
			Object val;
			if (!(condition instanceof ComplexCondition)) {
				AbstractSimpleCondition simpleCondition = (AbstractSimpleCondition) condition;
				headerKey = simpleCondition.getkey();
				if (condition instanceof NumericCondition) {
					val = ((NumericCondition) condition).getValue();
					matchValue = String.valueOf(val);
				} else if (condition instanceof EqualsCondition) {
					val = ((EqualsCondition) condition).getValue();
					matchValue = val != null ? val.toString() : "";
				} else if (condition instanceof JavaRegexCondition) {
					val = ((JavaRegexCondition) condition).getValue();
					matchValue = val != null ? val.toString() : "";
				} else {
					matchValue = "";
				}
				conditionType = Utils.SIMPLE_CONDITIONS.get(condition.getClass());
			} else {
				conditionType = Utils.COMPLEX_CONDITIONS.get(condition.getClass());
			}
		}
	}

	public void buildTreeFromRule() {
		Rule rule = ruleBean.getCurrRule();
		if (rule != null && rule.getCondition() != null) {
			root.getChildren().clear();
			buildSubTreeFromCondition(rule.getCondition(), root);
		}
	}

	public void buildSubTreeFromCondition(Condition condition, TreeNode node) {
		if (condition instanceof ComplexCondition) {
			TreeNode subNode = new DefaultTreeNode(condition, node);
			node.getChildren().add(subNode);
			for (Condition childCondition : ((ComplexCondition) condition).getConditions()) {
				buildSubTreeFromCondition(childCondition, subNode);
			}
		} else {
			node.getChildren().add(new DefaultTreeNode(condition, node));
		}
	}

	public void truncateConditions() {
		root = new DefaultTreeNode("Root", null);
		selectedNode = null;
	}

	public boolean isComplex(TreeNode node) {
		return (node.getData() instanceof ComplexCondition);
	}

	public boolean isEquals(TreeNode node) {
		return (node.getData() instanceof EqualsCondition);
	}

	public void changeSimpleCondition() {
		if (selectedNode != null) {
			TreeNode parent = selectedNode.getParent();
			selectedNode.setSelected(false);
			int index = removeCondition();
			double numVal = Double.MIN_VALUE;
			if (matchValue == null) {
				matchValue = "0";
			} else if (matchValue.matches("-?\\d+(\\.\\d+)?")) {
				numVal = parseToNumber(matchValue);
			}
			switch (conditionType) {
			case "eq":
				if (numVal != Double.MIN_VALUE) {
					selectedNode = new DefaultTreeNode(new EqualsCondition(headerKey, numVal), parent);
				}else {
					selectedNode = new DefaultTreeNode(new EqualsCondition(headerKey, matchValue), parent);
				}
				break;
			case "gte":
				selectedNode = new DefaultTreeNode(new GreaterThanEqualToCondition(headerKey, numVal), parent);
				break;
			case "lte":
				selectedNode = new DefaultTreeNode(new LessThanEqualToCondition(headerKey, numVal), parent);
				break;
			case "gt":
				selectedNode = new DefaultTreeNode(new GreaterThanCondition(headerKey, numVal), parent);
				break;
			case "lt":
				selectedNode = new DefaultTreeNode(new LessThanCondition(headerKey, numVal), parent);
				break;
			case "matches":
				selectedNode = new DefaultTreeNode(new JavaRegexCondition(headerKey, matchValue), parent);
				break;
			}
			if (selectedNode == null || parent == null) {
				logger.severe("Invalid situation:" + parent);
				return;
			}
			parent.getChildren().add(index, selectedNode);
			selectedNode.setSelected(true);
			selectedNode.setExpanded(true);
		}
	}

	public static double parseToNumber(String value) {
		return Double.parseDouble(value);
	}

	public void buildCondition() {
		if (root != null && root.getChildCount() > 0) {
			TreeNode baseCondition = root.getChildren().get(0);
			Condition condition = extractSubConditions(baseCondition);
			selectedNode = baseCondition;
			removeCondition();
			DefaultTreeNode newBase = new DefaultTreeNode(condition, root);
			newBase.getChildren().addAll(baseCondition.getChildren());
			newBase.setExpanded(true);
			newBase.setSelected(false);
			root.getChildren().add(newBase);
			ruleBean.getCurrRule().setCondition(condition);
		}
	}

	public Condition extractSubConditions(TreeNode node) {
		Condition condition = (Condition) node.getData();
		if (condition instanceof ComplexCondition) {
			List<Condition> conditions = new ArrayList<>();
			for (TreeNode child : node.getChildren()) {
				conditions.add(extractSubConditions(child));
			}
			((ComplexCondition) condition).setConditions(conditions);
		}
		return condition;
	}

	public void changeComplexCondition() {
		if (selectedNode != null) {
			TreeNode parent = selectedNode.getParent();
			selectedNode.setSelected(false);
			int index = removeCondition();
			List<TreeNode> children = null;
			if (selectedNode.getChildCount() > 0) {
				children = selectedNode.getChildren();
			}
			if (conditionType.equalsIgnoreCase("or")) {
				selectedNode = new DefaultTreeNode(new OrCondition(null), parent);
			} else {
				selectedNode = new DefaultTreeNode(new AndCondition(null), parent);
			}
			parent.getChildren().add(index, selectedNode);
			selectedNode.setSelected(true);
			selectedNode.setExpanded(true);
			if (children != null) {
				selectedNode.getChildren().addAll(children);
			}
		}
	}

	/**
	 * @return the condition
	 */
	public Condition getCondition() {
		return condition;
	}

	/**
	 * @return the headerKey
	 */
	public String getHeaderKey() {
		return headerKey;
	}

	/**
	 * @param headerKey
	 *            the headerKey to set
	 */
	public void setHeaderKey(String headerKey) {
		this.headerKey = headerKey;
	}

	/**
	 * @return the matchValue
	 */
	public String getMatchValue() {
		return matchValue;
	}

	/**
	 * @param matchValue
	 *            the matchValue to set
	 */
	public void setMatchValue(String matchValue) {
		this.matchValue = matchValue;
	}

	/**
	 * @return the root
	 */
	public TreeNode getRoot() {
		return root;
	}

	/**
	 * @return the selectedNode
	 */
	public TreeNode getSelectedNode() {
		return selectedNode;
	}

	/**
	 * @param selectedNode
	 *            the selectedNode to set
	 */
	public void setSelectedNode(TreeNode selectedNode) {
		this.selectedNode = selectedNode;
	}

	/**
	 * @return the conditionType
	 */
	public String getConditionType() {
		return conditionType;
	}

	/**
	 * @param conditionType
	 *            the conditionType to set
	 */
	public void setConditionType(String conditionType) {
		this.conditionType = conditionType;
	}

	/**
	 * @return the ruleBean
	 */
	public RulesBean getRuleBean() {
		return ruleBean;
	}

	/**
	 * @param ruleBean
	 *            the ruleBean to set
	 */
	public void setRuleBean(RulesBean ruleBean) {
		this.ruleBean = ruleBean;
	}

	/**
	 * @return simpleConditions
	 */
	public Collection<String> getSimpleConditions() {
		ArrayList<String> list = new ArrayList<>(Utils.SIMPLE_CONDITIONS.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * @return complexConditions
	 */
	public Collection<String> getComplexConditions() {
		ArrayList<String> list = new ArrayList<>(Utils.COMPLEX_CONDITIONS.values());
		Collections.sort(list);
		return list;
	}

	/**
	 * @return the numeric
	 */
	public boolean isNumeric() {
		return numeric;
	}

	/**
	 * @param numeric
	 *            the numeric to set
	 */
	public void setNumeric(boolean numeric) {
		this.numeric = numeric;
	}

}