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
package com.srotya.tau.wraith.store;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.aggregators.Aggregator;

/**
 * An idempotent read store to be used for Wraith aggregation operations such
 * that when values are committed to the system, they are stored such that when
 * read they need to be idempotent as in duplicates are ignored. <br>
 * <br>
 * E.g. Counting aggregation of a field should not be done such that recounting
 * the same value in the event of a re-play is not possible. Use structures like
 * Sets, BloomFilters etc to accomplish this.
 * 
 * @author ambud_sharma
 */
public interface AggregationStore extends Store {

	/**
	 * Add a count value for an entity to a timeseries
	 * 
	 * @param taskId
	 * @param timestamp
	 * @param entity
	 * @param count
	 * @throws IOException
	 */
	public void putValue(int taskId, long timestamp, String entity, long count) throws IOException;

	/**
	 * Add a count value for an entity to a timeseries
	 * 
	 * @param taskId
	 * @param timestamp
	 * @param entity
	 * @param count
	 * @throws IOException
	 */
	public void putValue(int taskId, long timestamp, String entity, int count) throws IOException;

	/**
	 * Persist entity and aggregator
	 * 
	 * @param taskId
	 * @param entity
	 * @param aggregator
	 * @throws IOException
	 */
	public void persist(int taskId, String entity, Aggregator aggregator) throws IOException;

	/**
	 * @param taskId
	 * @param aggregator
	 * @return 
	 * @throws IOException
	 */
	public Map<String, Aggregator> retrive(int taskId, Aggregator aggregator) throws IOException;

	/**
	 * Put set of value to an existing set for an entity
	 * 
	 * @param taskId
	 * @param entity
	 * @param values
	 * @throws IOException
	 */
	public void mergeSetValues(int taskId, String entity, Set<Object> values) throws IOException;

	/**
	 * Put set of value to an existing set for an entity
	 * 
	 * @param taskId
	 * @param entity
	 * @param values
	 * @throws IOException
	 */
	public void mergeSetIntValues(int taskId, String entity, Set<Integer> values) throws IOException;

	/**
	 * Put byte array value to dataset
	 * 
	 * @param taskId
	 * @param entity
	 * @param value
	 * @throws IOException
	 */
	public void putValue(int taskId, String entity, ICardinality value) throws IOException;

	/**
	 * @param taskId
	 * @param key
	 * @param value
	 * @throws IOException
	 */
	public void persistState(int taskId, String key, MutableBoolean value) throws IOException;

	/**
	 * @param taskId
	 * @return
	 * @throws IOException
	 */
	public Map<String, MutableBoolean> retriveStates(int taskId) throws IOException;

	/**
	 * Purge / untrack state from {@link AggregationStore}, to be used when
	 * output has already been emitted for this key
	 * 
	 * @param taskId
	 * @param key
	 * @throws IOException
	 */
	public void purgeState(int taskId, String key) throws IOException;

}