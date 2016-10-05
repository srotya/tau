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

import java.util.Map;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.PerformantException;
import com.srotya.tau.wraith.actions.Action;

/**
 * Aggregation Action defines abstractly aggregation capabilities in the system
 * 
 * @author ambud_sharma
 */
public abstract class AggregationAction implements Action {
	
	private static final long serialVersionUID = 1L;
	private short actionId;
	private String aggregationKey;
	private int aggregationWindow;
	
	public AggregationAction(short actionId, String aggregationKey, int aggregationWindow) {
		this.actionId = actionId;
		this.aggregationKey = aggregationKey;
		this.aggregationWindow = aggregationWindow;
	}
	
	@Override
	public Event actOnEvent(Event inputEvent) {
		Map<String, Object> headers = inputEvent.getHeaders();
		Object aggregationKey = headers.get(getAggregationKey());
		if(aggregationKey==null) {
			return null;
		}
		headers.put(Constants.FIELD_AGGREGATION_KEY, aggregationKey.toString());
		try {
			postProcessEvent(inputEvent);
		} catch (PerformantException e) {
			return null;
		}
		return inputEvent;
	}

	/**
	 * Run any post processing operations
	 * @param inputEvent
	 * @throws PerformantException
	 */
	public abstract void postProcessEvent(Event inputEvent) throws PerformantException;

	@Override
	public ACTION_TYPE getActionType() {
		return ACTION_TYPE.AGGREGATION;
	}
	
	/**
	 * @return aggregationKey
	 */
	public String getAggregationKey() {
		return aggregationKey;
	}

	@Override
	public short getActionId() {
		return actionId;
	}

	/**
	 * @return the aggregationWindow
	 */
	public int getAggregationWindow() {
		return aggregationWindow;
	}

	@Override
	public void setActionId(short actionId) {
		this.actionId = actionId;
	}

	/**
	 * @param aggregationKey the aggregationKey to set
	 */
	public void setAggregationKey(String aggregationKey) {
		this.aggregationKey = aggregationKey;
	}

	/**
	 * @param aggregationWindow the aggregationWindow to set
	 */
	public void setAggregationWindow(int aggregationWindow) {
		this.aggregationWindow = aggregationWindow;
	}
}
