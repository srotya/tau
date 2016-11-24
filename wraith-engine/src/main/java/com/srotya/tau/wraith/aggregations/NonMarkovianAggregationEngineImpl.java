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
package com.srotya.tau.wraith.aggregations;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregators.AggregationRejectException;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.store.AggregationStore;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * {@link NonMarkovianAggregationEngineImpl} aggregates the value, group values
 * by a key either put them together or count them.<br>
 * <br>
 * 
 * {@link NonMarkovianAggregationEngineImpl} is also fault tolerant by providing
 * a flush feature that periodically flushes results to a persistent store.<br>
 * <br>
 * 
 * {@link Aggregator}s are asked to be idempotent i.e. calling the same
 * aggregation operation with the same value should not have any effect in a
 * given window of time.
 * 
 * @author ambud_sharma
 */
public class NonMarkovianAggregationEngineImpl implements NonMarkovianAggregationEngine {

	private AggregationRejectException AggregationRejectException = new AggregationRejectException();
	private SortedMap<String, Aggregator> aggregationMap;
	private SortedMap<String, Aggregator> flushAggregationMap;
	private Aggregator template;
	private AggregationStore store;
	private int taskId;

	/**
	 * {@link Aggregator} settings can be initialized with supplied
	 * configuration
	 * 
	 * @param conf
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public void initialize(Map<String, String> conf, int taskId) throws Exception {
		this.taskId = taskId;
		aggregationMap = new TreeMap<>();
		flushAggregationMap = new TreeMap<>();
		String agreggatorType = conf.get(Constants.AGGREGATOR_TYPE);
		template = (Aggregator) Class.forName(agreggatorType).newInstance();
		template.initialize(conf);
	}

	/**
	 * Aggregate the value for the key and notify the caller if this aggregation
	 * had an effect on the internal state or data structure the aggregator is
	 * using.
	 * 
	 * @param timestamp
	 * @param aggregationWindow
	 *            in seconds
	 * @param ruleActionId
	 * @param aggregationKey
	 * @param aggregationValue
	 * @return if the supplied value changed the aggregator state
	 * @throws AggregationRejectException
	 */
	public boolean aggregate(long timestamp, int aggregationWindow, String ruleActionId, String aggregationKey,
			Object aggregationValue) throws AggregationRejectException {
		String key = createMapKey(timestamp, aggregationWindow, ruleActionId, aggregationKey);
		Aggregator aggregator = getAggregationMap().get(key);
		if (aggregator == null) {
			aggregator = template.getInstance();
			getAggregationMap().put(key, aggregator);
			getFlushMap().put(key, template.getInstance());
		}
		if (aggregator.disableLimitChecks() || (aggregator.size() < aggregator.getHardLimit())) {
			if (aggregator.add(aggregationValue)) {
				return getFlushMap().get(key).add(aggregationValue);
			} else {
				return false;
			}
		} else {
			throw AggregationRejectException;
		}
	}

	/**
	 * Flush and commit data
	 * 
	 * @throws IOException
	 */
	public void flush() throws IOException {
		for (Entry<String, Aggregator> entry : getFlushMap().entrySet()) {
			if (store != null) {
				store.persist(taskId, entry.getKey(), entry.getValue());
			}
			entry.getValue().reset();
		}
	}

	/**
	 * Is this aggregator processing data for a supplied ruleActionId key
	 * 
	 * @param ruleActionId
	 * @return true if it is
	 */
	public boolean containsRuleActionId(String ruleActionId) {
		return getAggregationMap().containsKey(ruleActionId);
	}

	/**
	 * Emit the aggregates for a given ruleActionId and reset the counters for
	 * it
	 **/
	@Override
	public void commit(int aggregationWindow, String ruleActionId) throws IOException {
		flush();
	}

	public static int extractTsFromAggregationKey(String key) {
		return Utils.stringToInt(key.split(Constants.KEY_SEPARATOR)[1]);
	}

	/**
	 * @return
	 */
	protected final SortedMap<String, Aggregator> getAggregationMap() {
		return aggregationMap;
	}

	/**
	 * @return
	 */
	protected final SortedMap<String, Aggregator> getFlushMap() {
		return flushAggregationMap;
	}

	/**
	 * @return the flushAggregationMap
	 */
	public SortedMap<String, Aggregator> getFlushAggregationMap() {
		return flushAggregationMap;
	}

	/**
	 * @param timestamp
	 * @param aggregationWindow
	 * @param ruleActionId
	 * @param aggregationKey
	 * @return
	 */
	public static String createMapKey(long timestamp, int aggregationWindow, String ruleActionId,
			String aggregationKey) {
		String ts = Utils.intToString((int) (timestamp / (1000 * aggregationWindow)) * aggregationWindow);
		return new StringBuilder(ruleActionId.length() + 1 + ts.length() + 1 + aggregationKey.length())
				.append(ruleActionId).append(Constants.KEY_SEPARATOR).append(ts).append(Constants.KEY_SEPARATOR)
				.append(aggregationKey).toString();
	}

	/**
	 * @param key
	 * @return
	 */
	public static String[] splitMapKey(String key) {
		return key.split("\\" + Constants.KEY_SEPARATOR);
	}

	@Override
	public void cleanup() throws IOException {
		store.disconnect();
	}

	@Override
	public void restore() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void flush(int aggregationWindow, String ruleActionId) throws IOException {
		for (Entry<String, Aggregator> entry : getFlushMap()
				.subMap(Utils.concat(ruleActionId, Constants.KEY_SEPARATOR),
						Utils.concat(ruleActionId, Constants.KEY_SEPARATOR, String.valueOf(Character.MAX_VALUE)))
				.entrySet()) {
			if (store != null) {
				store.persist(taskId, entry.getKey(), entry.getValue());
			}
			entry.getValue().reset();
		}
	}

}