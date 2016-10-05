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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.conditions.Condition;

/**
 * A simple {@link Rule} concrete implementation
 * 
 * @author ambud_sharma
 *
 */
public class SimpleRule extends Rule {

	private static final long serialVersionUID = 1L;
	private Condition condition;
	private List<Action> actions;
	
	public SimpleRule(short ruleId, String name, boolean active, Condition condition, Action ...action) {
		super(ruleId, name, active);
		this.condition = condition;
		this.actions = new ArrayList<>(Arrays.asList(action));
	}
	
	@Override
	public Condition getCondition() {
		return condition;
	}

	/**
	 * @return the action
	 */
	@Override
	public List<Action> getActions() {
		return actions;
	}

	/**
	 * @param actions the action to set
	 */
	@Override
	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	/**
	 * @param condition the condition to set
	 */
	public void setCondition(Condition condition) {
		this.condition = condition;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SimpleRule) {
			return ((SimpleRule)obj).getRuleId()==getRuleId();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SimpleRule [id="+getRuleId()+", name="+getName()+", active="+isActive()+", condition=" + condition + ", action=" + actions + "]";
	}

}
