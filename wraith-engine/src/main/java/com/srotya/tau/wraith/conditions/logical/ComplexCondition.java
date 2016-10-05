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
package com.srotya.tau.wraith.conditions.logical;

import java.util.List;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.Required;
import com.srotya.tau.wraith.conditions.Condition;

/**
 * Complex conditions are made of more than 1 conditions. Complex conditions also have the provision to short circuit
 * 
 * @author ambud_sharma
 *
 */
public abstract class ComplexCondition implements Condition {
	
	private static final long serialVersionUID = 1L;
	@Required
	private List<Condition> conditions;
	
	public ComplexCondition(List<Condition> conditions) {
		this.conditions = conditions;
	}
	
	@Override
	public boolean matches(Event event) {
		boolean matchResult = conditions.get(0).matches(event);
		for(int i=1;i<conditions.size();i++) {
			boolean val = conditions.get(i).matches(event);
			matchResult = operator(matchResult, val);
			if(shortCircuit(matchResult, val)) {
				break;
			}
		}
		return matchResult;
	}
	
	/**
	 * Apply the operator defined by this condition on the results of the evaluated conditions
	 * @param c1
	 * @param c2
	 * @return true if c1 O c2 is true, where O is an operator
	 */
	public abstract boolean operator(boolean c1, boolean c2);
	
	/**
	 * Allows for early termination of the loop in the supplied list of conditions
	 * @param c1
	 * @param c2
	 * @return true if c1 O c2 should short circuit
	 */
	public abstract boolean shortCircuit(boolean c1, boolean c2);

	/**
	 * Getter for conditions
	 * @return conditions
	 */
	public List<Condition> getConditions() {
		return conditions;
	}

	/**
	 * Setter for conditions
	 * @param conditions
	 */
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ComplexCondition [conditions=" + conditions + "]";
	}
	
}
