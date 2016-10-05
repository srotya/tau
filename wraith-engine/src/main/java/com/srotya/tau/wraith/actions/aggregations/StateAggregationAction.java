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
package com.srotya.tau.wraith.actions.aggregations;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.conditions.Condition;

/**
 * Validate the state for tracker to continue tracking this or mark the state as
 * untrackable
 * 
 * @author ambud_sharma
 */
public class StateAggregationAction extends AggregationAction {

	private static final long serialVersionUID = 1L;
	private Condition stateCondition;

	public StateAggregationAction(short actionId, String aggregationKey, int aggregationWindow,
			Condition stateCondition) {
		super(actionId, aggregationKey, aggregationWindow);
		this.stateCondition = stateCondition;
	}

	@Override
	public void postProcessEvent(Event inputEvent) {
		if (stateCondition.matches(inputEvent)) {
			inputEvent.getHeaders().put(Constants.FIELD_STATE_TRACK, true);
		} else {
			inputEvent.getHeaders().put(Constants.FIELD_STATE_TRACK, false);
		}
	}

	/**
	 * @return the stateCondition
	 */
	public Condition getStateCondition() {
		return stateCondition;
	}

	/**
	 * @param stateCondition
	 *            the stateCondition to set
	 */
	public void setStateCondition(Condition stateCondition) {
		this.stateCondition = stateCondition;
	}

	@Override
	public ACTION_TYPE getActionType() {
		return ACTION_TYPE.STATE;
	}

}
