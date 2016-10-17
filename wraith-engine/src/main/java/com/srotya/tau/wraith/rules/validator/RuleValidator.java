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
package com.srotya.tau.wraith.rules.validator;

import java.util.List;

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.rules.Rule;

/**
 * {@link Validator} for a {@link Rule}
 * 
 * @author ambud_sharma
 */
public class RuleValidator implements Validator<Rule> {
	
	private static final int MAX_LENGTH_RULE_NAME = 100;
	private static final int MAX_LENGTH_RULE_DESCRIPTION = 500;
	private static final RuleValidator INSTANCE = new RuleValidator();
	private ActionValidator actionValidator = new ActionValidator();
	private ConditionValidator conditionValidator = new ConditionValidator();
	
	private RuleValidator() {
	}
	
	public static RuleValidator getInstance() {
		return INSTANCE;
	}
	
	public void configure(List<Validator<?>> validators) {
		conditionValidator.configure(validators);
		actionValidator.configure(validators);
	}
	
	public void validate(Rule rule) throws ValidationException {
		if(rule.getCondition()==null) {
			throw new ValidationException("Rule can't have empty condition");
		}
		if(rule.getName()==null || rule.getName().trim().isEmpty()) {
			throw new ValidationException("Rule name can't be empty");
		}
		if(rule.getName().length()>MAX_LENGTH_RULE_NAME) {
			throw new ValidationException("Rule name must be less than "+MAX_LENGTH_RULE_NAME+" characters");
		}
		if(rule.getDescription()!=null && rule.getDescription().length()>MAX_LENGTH_RULE_DESCRIPTION) {
			throw new ValidationException("Rule description must be less than "+MAX_LENGTH_RULE_DESCRIPTION+" characters");
		}
		conditionValidator.validate(rule.getCondition());
		if(rule.getActions()==null || rule.getActions().size()==0) {
			throw new ValidationException("Rule can't have 0 Actions");
		}
		for(Action action:rule.getActions()) {
			actionValidator.validate(action);
		}
	}

	@Override
	public Class<Rule> getType() {
		return Rule.class;
	}

}