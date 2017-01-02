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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Blueprint for how aggregations should be done in Wraith and what actions an {@link Aggregator} must support.
 * <br><br>
 * {@link Aggregator}s are asked to be idempotent i.e. calling the same aggregation operation with the same value
 * should not have any effect in a given window of time.
 * 
 * @author ambud_sharma
 */
public interface Aggregator extends Serializable {
	
	/**
	 * To initialize {@link Aggregator} with the supplied configuration
	 * @param conf
	 */
	public void initialize(Map<String, String> conf);
	
	/**
	 * To populate the aggregator restored data
	 * @param data
	 * @throws IOException
	 */
	public void initialize(Object data) throws IOException;
	
	/**
	 * The maximum number of values the {@link Aggregator} can store
	 * @return limit
	 */
	public int getHardLimit();
	
	/**
	 * Self factory, to create a brand new {@link Aggregator} instance
	 * with the same configuration
	 * @return
	 */
	public Aggregator getInstance();
	
	/**
	 * @return current size of the {@link Aggregator}
	 */
	public long size();
	
	/**
	 * Add the aggregation value to the {@link Aggregator}
	 * @param timestamp
	 * @param aggregationValue
	 * @return true if this value changed the internal state of the {@link Aggregator}
	 */
	public boolean add(Long timestamp, Object aggregationValue);
	
	/**
	 * Optimization to disable checks on hard limits
	 * @return true if hard limits are not to be checked
	 */
	public boolean disableLimitChecks();
	
	/**
	 * @return datastructure used to actually store data
	 */
	public Object getDatastructure();

	/**
	 * Reset the {@link Aggregator}'s data structure
	 */
	public void reset();
}
