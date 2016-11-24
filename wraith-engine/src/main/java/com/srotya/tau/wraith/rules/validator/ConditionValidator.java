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

import java.util.ArrayList;
import java.util.List;

import com.srotya.tau.wraith.conditions.AbstractSimpleCondition;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.logical.ComplexCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;
import com.srotya.tau.wraith.conditions.relational.NumericCondition;
import com.srotya.tau.wraith.rules.Rule;

/**
 * {@link Validator} for {@link Condition} associated with a {@link Rule}
 * 
 * @author ambud_sharma
 */
public class ConditionValidator implements Validator<Condition> {

	private static final int MAX_LENGTH_REGEX = 200;
	private List<Validator<Condition>> conditionValidators = new ArrayList<>();

	@SuppressWarnings("unchecked")
	@Override
	public void configure(List<Validator<?>> validators) {
		for (Validator<?> validator : validators) {
			try {
				if (validator.getType() == getType()) {
					conditionValidators.add((Validator<Condition>) validator);
					System.err.println("Adding validation to chain:" + validator.getClass());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void validate(Condition condition) throws ValidationException {
		if (condition instanceof ComplexCondition) {
			for (Condition childCondition : ((ComplexCondition) condition).getConditions()) {
				validate(childCondition);
			}
		} else {
			if (condition instanceof AbstractSimpleCondition) {
				AbstractSimpleCondition castedConditon = ((AbstractSimpleCondition) condition);
				if (castedConditon.getkey() == null || castedConditon.getkey().isEmpty()) {
					throw new ValidationException("Condition header key cannot be empty");
				}
				if (castedConditon instanceof NumericCondition) {
					if (((NumericCondition) castedConditon).getValue() == (Number)Double.MIN_VALUE) {
						throw new ValidationException("Numeric conditions must have a value");
					}
				} else if (castedConditon instanceof EqualsCondition) {
					Object value = ((EqualsCondition) castedConditon).getValue();
					if (value == null) {
						throw new ValidationException("Equals condition must have a value");
					}
					if ((value instanceof String) && ((String) (value)).isEmpty()) {
						throw new ValidationException("Equals condition value can't be empty");
					}
				} else if (castedConditon instanceof JavaRegexCondition) {
					String regex = ((JavaRegexCondition) castedConditon).getValue();
					if (regex.isEmpty()) {
						throw new ValidationException("Regex value can't be empty");
					}
					if (regex.length() > MAX_LENGTH_REGEX) {
						throw new ValidationException(
								"Regex value must be smaller than " + MAX_LENGTH_REGEX + " characters");
					}
				}
			} else {
				// unsupported condition type
				throw new ValidationException("Unsupported condition");
			}
		}
		for (Validator<Condition> validator : conditionValidators) {
			validator.validate(condition);
		}
	}

	@Override
	public Class<Condition> getType() {
		return Condition.class;
	}

}