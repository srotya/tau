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

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.AggregationAction;
import com.srotya.tau.wraith.actions.aggregations.StateAggregationAction;
import com.srotya.tau.wraith.actions.aggregations.ValueAggregationAction;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.rules.Rule;

/**
 * {@link Validator} for {@link Action} associated with a {@link Rule}
 * 
 * @author ambud_sharma
 */
public class ActionValidator implements Validator<Action> {

	private List<Validator<Action>> conditionValidators = new ArrayList<>();
	private List<Validator<?>> globalValidators;

	@SuppressWarnings("unchecked")
	@Override
	public void configure(List<Validator<?>> validators) {
		this.globalValidators = validators;
		for (Validator<?> validator : validators) {
			try {
				conditionValidators.add((Validator<Action>) validator);
			} catch (Exception e) { // ignore incompatible validators
			}
		}
	}

	@Override
	public void validate(Action action) throws ValidationException {
		if (action instanceof TemplatedAlertAction) {
			TemplatedAlertAction alertAction = (TemplatedAlertAction) action;
			if (alertAction.getTemplateId() < 0) {
				throw new ValidationException("Template ids always start from 0 ");
			}
		} else if (action instanceof AggregationAction) {
			AggregationAction aggregationAction = (AggregationAction) action;
			if (!(aggregationAction instanceof StateAggregationAction)) {
				throw new ValidationException("Unsupported aggregation action type");
			}
			if (aggregationAction.getAggregationKey() == null
					|| aggregationAction.getAggregationKey().isEmpty()) {
				throw new ValidationException("Aggregation key can't be empty");
			}
			if (aggregationAction.getAggregationWindow() < 10) {
				throw new ValidationException("Aggregation window must be bigger than 10 seconds");
			}
			if (aggregationAction instanceof StateAggregationAction) {
				StateAggregationAction stateAggregation = (StateAggregationAction) action;
				if (stateAggregation.getStateCondition() == null) {
					throw new ValidationException("State condition can't be empty");
				}
				ConditionValidator validator = new ConditionValidator();
				if (globalValidators != null) {
					validator.configure(globalValidators);
				}
				validator.validate(stateAggregation.getStateCondition());
			}else if (aggregationAction instanceof ValueAggregationAction) {
				ValueAggregationAction valueAggregation = (ValueAggregationAction) action;
				if (valueAggregation.getAggregationValue() == null || valueAggregation.getAggregationValue().isEmpty()) {
					throw new ValidationException("Aggregation value can't be empty");
				}
			}
		} else {
			// unsupported action
			throw new ValidationException("Unsupported action type");
		}
		for (Validator<Action> validator : conditionValidators) {
			validator.validate(action);
		}
	}

}