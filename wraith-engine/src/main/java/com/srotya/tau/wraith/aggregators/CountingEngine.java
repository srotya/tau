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
package com.srotya.tau.wraith.aggregators;

import java.util.Map;

import com.srotya.tau.wraith.EventFactory;
import com.srotya.tau.wraith.aggregations.MarkovianAggregationEngineImpl;
import com.srotya.tau.wraith.store.StoreFactory;

/**
 * {@link CountingEngine} aggregates the value, group values by a key either put
 * them together or count them.<br>
 * <br>
 * 
 * {@link CountingEngine} is also fault tolerant by providing a flush feature
 * that periodically flushes results to a persistent store.<br>
 * <br>
 * 
 * {@link Aggregator}s are asked to be idempotent i.e. calling the same
 * aggregation operation with the same value should not have any effect in a
 * given window of time.
 * 
 * @author ambud_sharma
 */
public class CountingEngine extends MarkovianAggregationEngineImpl {

	public CountingEngine(EventFactory eventFactory, StoreFactory storeFactory, String agreggatorType) {
		super(eventFactory, storeFactory, agreggatorType);
	}

	/**
	 * Aggregator settings can be initialized with supplied configuration
	 * 
	 * @param conf
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@Override
	public void initialize(Map<String, String> conf, int taskId) throws Exception {
		super.initialize(conf, taskId);
	}

}