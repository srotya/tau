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

import com.clearspring.analytics.hash.MurmurHash;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;

/**
 * An {@link AggregationAction} that defines counting capabities
 * 
 * @author ambud_sharma
 */
public abstract class CountingAggregationAction extends ValueAggregationAction {

	private static final long serialVersionUID = 1L;
	
	public CountingAggregationAction(short actionId, String aggregationHeaderKey, String aggregationHeaderValueKey, int aggregationWindow) {
		super(actionId, aggregationHeaderKey, aggregationHeaderValueKey, aggregationWindow);
	}

	@Override
	public void postProcessEvent(Event inputEvent) {
		// counting aggregations are hashed tostring values of the aggregation value 
		inputEvent.getHeaders().put(Constants.FIELD_AGGREGATION_VALUE
				, MurmurHash.hash(
						inputEvent.getHeaders().get(Constants.FIELD_AGGREGATION_VALUE).toString()
						)
				);
	}

}