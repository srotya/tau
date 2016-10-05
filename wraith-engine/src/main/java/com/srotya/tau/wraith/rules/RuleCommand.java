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

/**
 * Wrapper for transmission of {@link Rule} to the engine over a message broker
 * 
 * @author ambud_sharma
 */
public class RuleCommand implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String ruleGroup;
	private boolean delete;
	private String ruleContent;
	
	public RuleCommand() {
	}

	public RuleCommand(String ruleGroup, boolean delete, String ruleContent) {
		this.ruleGroup = ruleGroup;
		this.delete = delete;
		this.ruleContent = ruleContent;
	}

	/**
	 * @return the ruleGroup
	 */
	public String getRuleGroup() {
		return ruleGroup;
	}

	/**
	 * @param ruleGroup the ruleGroup to set
	 */
	public void setRuleGroup(String ruleGroup) {
		this.ruleGroup = ruleGroup;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * @return the ruleContent
	 */
	public String getRuleContent() {
		return ruleContent;
	}

	/**
	 * @param ruleContent the ruleContent to set
	 */
	public void setRuleContent(String ruleContent) {
		this.ruleContent = ruleContent;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RuleCommand [ruleGroup=" + ruleGroup + ", delete=" + delete + ", ruleContent=" + ruleContent + "]";
	}

}
