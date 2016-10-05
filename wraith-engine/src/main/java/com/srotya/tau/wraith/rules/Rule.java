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
package com.srotya.tau.wraith.rules;

import java.io.Serializable;
import java.util.List;

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.conditions.Condition;

/**
 * Abstract definition of a rule.<br><br>
 * 
 * 1 rule is composed for:
 * <ul>
 * 	<li>rule id that uniquely identifies a rule</li>
 *  <li>rule name</li>
 *  <li>1 {@link Condition}</li>
 *  <li>List of {@link Action}s</li>
 * </ul> 
 * 
 * @author ambud_sharma
 *
 */
public abstract class Rule implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private short ruleId;
	private String name;
	private boolean active;
	private String description;
	
	public Rule(short ruleId, String name, boolean active) {
		this.ruleId = ruleId;
		this.name = name;
		this.active = active;
	}

	/**
	 * Getter for condition
	 * @return condition
	 */
	public abstract Condition getCondition();
	
	/**
	 * Setter for condition
	 * @param condition
	 */
	public abstract void setCondition(Condition condition);
	
	/**
	 * Getter for actions
	 * @return actions
	 */
	public abstract List<Action> getActions();
	
	/**
	 * Setter for actions
	 * @param actions
	 */
	public abstract void setActions(List<Action> actions);
	
	/**
	 * Getter for ruleId
	 * @return ruleId
	 */
	public short getRuleId() {
		return ruleId;
	}
	
	/**
	 * Getter for rule name
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Rule) {
			return this.getRuleId() == ((Rule) obj).getRuleId();
		}else{
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return ruleId;
	}

	/**
	 * @return true if rule is active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param ruleId the ruleId to set
	 */
	public void setRuleId(short ruleId) {
		this.ruleId = ruleId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
}
