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

/**
 * {@link AggregationAction}s where values are aggregated
 * 
 * @author ambud_sharma
 */
public abstract class ValueAggregationAction extends AggregationAction {

	private static final long serialVersionUID = 1L;
	private String aggregate;
	
	public ValueAggregationAction(short actionId, String groupBy, String aggregate, int aggregationWindow) {
		super(actionId, groupBy, aggregationWindow);
		this.aggregate = aggregate;
	}
	
	@Override
	public Event actOnEvent(Event inputEvent) {
		Map<String, Object> headers = inputEvent.getHeaders();
		Object aggregationKey = headers.get(getGroupBy());
		Object aggregationValue = headers.get(getAggregationValue());
		if(aggregationKey==null || aggregationValue==null) {
			return null;
		}
		headers.put(Constants.FIELD_AGGREGATION_KEY, aggregationKey.toString());
		headers.put(Constants.FIELD_AGGREGATION_VALUE, aggregationValue);
		try {
			postProcessEvent(inputEvent);
		} catch (PerformantException e) {
			return null;
		}
		return inputEvent;
	}

	/**
	 * @return the aggregationValue
	 */
	public String getAggregationValue() {
		return aggregate;
	}

	/**
	 * @param aggregationValue the aggregationValue to set
	 */
	public void setAggregationValue(String aggregationValue) {
		this.aggregate = aggregationValue;
	}

}
