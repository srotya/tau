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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregators.AggregationRejectException;
import com.srotya.tau.wraith.aggregators.StaleDataException;
import com.srotya.tau.wraith.store.AggregationStore;
import com.srotya.tau.wraith.store.StoreFactory;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author ambud_sharma
 */
public class StateTrackingEngine implements MarkovianAggregationEngine {

	private StaleDataException StaleDataException = new StaleDataException();
	private long jitterTolerance;
	private Map<String, Integer> lastEmittedBucketMap;
	private SortedMap<String, MutableBoolean> aggregationMap;
	private SortedMap<String, MutableBoolean> flushAggregationMap;
	private AggregationStore store;
	private int taskId;
	private StoreFactory factory;
	private EventFactory eventFactory;

	public StateTrackingEngine(EventFactory eventFactory, StoreFactory factory) {
		this.eventFactory = eventFactory;
		this.factory = factory;
	}

	@Override
	public void initialize(Map<String, String> conf, int taskId) throws Exception {
		this.taskId = taskId;
		lastEmittedBucketMap = new HashMap<String, Integer>();
		aggregationMap = new TreeMap<>();
		flushAggregationMap = new TreeMap<>();
		jitterTolerance = Integer.parseInt(
				conf.getOrDefault(Constants.AGGREGATION_JITTER_TOLERANCE, Constants.DEFAULT_JITTER_TOLERANCE)) * 1000;
		if (conf.get(Constants.ASTORE_TYPE) != null) {
			store = factory.getAggregationStore(conf.get(Constants.ASTORE_TYPE), conf);
			if (store != null) {
				store.connect();
				restore();
			}
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	@Override
	public void restore() throws IOException {
		Map<String, MutableBoolean> states = store.retriveStates(taskId);
		for (Entry<String, MutableBoolean> entry : states.entrySet()) {
			aggregationMap.put(entry.getKey(), entry.getValue());
		}
	}

	public void track(long timestamp, int aggregationWindow, String ruleActionId, String aggregationKey)
			throws AggregationRejectException {
		checkStaleData(timestamp, aggregationWindow, ruleActionId);
		String key = Utils.createMapKey(timestamp, aggregationWindow, ruleActionId, aggregationKey);
		MutableBoolean val = aggregationMap.get(key);
		if (val == null) {
			val = new MutableBoolean();
			val.setVal(true);
			getAggregationMap().put(key, val);
			getFlushAggregationMap().put(key, val);
		}
	}

	public void untrack(long timestamp, int aggregationWindow, String ruleActionId, String aggregationKey)
			throws AggregationRejectException {
		checkStaleData(timestamp, aggregationWindow, ruleActionId);
		String key = Utils.createMapKey(timestamp, aggregationWindow, ruleActionId, aggregationKey);
		MutableBoolean val = getAggregationMap().get(key);
		if (val != null) {
			val.setVal(false);
		}
	}

	public void checkStaleData(long timestamp, int aggregationWindow, String ruleActionId) throws StaleDataException {
		Integer lastEmits = lastEmittedBucketMap.get(ruleActionId);
		if (lastEmits != null && Utils.floorTs(timestamp + jitterTolerance, aggregationWindow) <= lastEmits) {
			throw StaleDataException;
		}
	}

	@Override
	public void flush() throws IOException {
		if (store != null) {
			for (Entry<String, MutableBoolean> entry : getFlushAggregationMap().entrySet()) {
				store.persistState(taskId, entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public void emit(int aggregationWindow, String ruleActionId, List<Event> events) throws IOException {
		flush();
		SortedMap<String, MutableBoolean> map = getAggregationMap().subMap(
				Utils.concat(ruleActionId, Constants.KEY_SEPARATOR),
				Utils.concat(ruleActionId, Constants.KEY_SEPARATOR, String.valueOf(Character.MAX_VALUE)));
		if(map.isEmpty()) {
			// no emits
			return;
		}
		int lastTs = 0;
		if (getLastEmittedBucketMap().containsKey(ruleActionId)) {
			lastTs = getLastEmittedBucketMap().get(ruleActionId) + aggregationWindow;
		} else {
			lastTs = MarkovianAggregationEngineImpl.extractTsFromAggregationKey(map.lastKey());
			lastTs = lastTs - aggregationWindow - (int) (getJitterTolerance() / 1000);
		}
		String val = Utils.intToString(lastTs);
		val = new StringBuilder(ruleActionId.length() + 3 + val.length()).append(ruleActionId)
				.append(Constants.KEY_SEPARATOR).append(val).append(Constants.KEY_SEPARATOR).append(Character.MAX_VALUE)
				.toString();
		map = getAggregationMap().subMap(ruleActionId, val);
		Set<Entry<String, MutableBoolean>> set = map.entrySet();
		for (Iterator<Entry<String, MutableBoolean>> iterator = set.iterator(); iterator.hasNext();) {
			Entry<String, MutableBoolean> entry = iterator.next();
			if (entry.getValue().isVal()) {
				Event event = eventFactory.buildEvent();
				String[] keyParts = Utils.splitMapKey(entry.getKey());
				long ts = MarkovianAggregationEngineImpl.extractTsFromAggregationKey(entry.getKey());
				event.getHeaders().put(Constants.FIELD_AGGREGATION_KEY, keyParts[keyParts.length - 1]);
				event.getHeaders().put(Constants.FIELD_TIMESTAMP, ts * 1000);
				event.getHeaders().put(Constants.FIELD_AGGREGATION_WINDOW, aggregationWindow);
				event.setEventId(new StringBuilder().append(keyParts[keyParts.length - 1]).append("_").append(ts * 1000)
						.toString());
				events.add(event);
			}
			if (store != null) {
				store.purgeState(taskId, entry.getKey());
			}
			getFlushAggregationMap().remove(entry.getKey());
			iterator.remove();
		}
		getLastEmittedBucketMap().put(ruleActionId, lastTs);
	}

	/**
	 * Is this aggregator processing data for a supplied ruleActionId key
	 * 
	 * @param ruleActionId
	 * @return true if it is
	 */
	public boolean containsRuleActionId(String ruleActionId) {
		StringBuilder builder = new StringBuilder(ruleActionId.length() + 2);
		builder.append(ruleActionId).append(Constants.KEY_SEPARATOR).append(Character.MAX_VALUE);
		return getAggregationMap().subMap(ruleActionId, builder.toString()).size() > 0;
	}

	/**
	 * @return the jitterTolerance
	 */
	protected long getJitterTolerance() {
		return jitterTolerance;
	}

	/**
	 * @return the lastEmittedBucketMap
	 */
	protected Map<String, Integer> getLastEmittedBucketMap() {
		return lastEmittedBucketMap;
	}

	/**
	 * @return the aggregationMap
	 */
	public SortedMap<String, MutableBoolean> getAggregationMap() {
		return aggregationMap;
	}

	/**
	 * @return the flushAggregationMap
	 */
	public SortedMap<String, MutableBoolean> getFlushAggregationMap() {
		return flushAggregationMap;
	}

	@Override
	public void cleanup() throws IOException {
		if (store != null) {
			store.disconnect();
		}
	}

	/**
	 * @return the store
	 */
	protected AggregationStore getStore() {
		return store;
	}

	/**
	 * @return the taskId
	 */
	public int getTaskId() {
		return taskId;
	}

}
