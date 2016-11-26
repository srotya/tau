/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.linea.ft;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.nucleus.disruptor.GroupByHandler;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;

/**
 * Inspired by the XOR Ledger concept of Apache Storm by Nathan Marz.
 * <br><br>
 * Acker an efficient tracking mechanism for tracking "tuple trees" regardless of the
 * amount of branching there may be in a deterministic memory space using a fast XOR operator.
 * 
 * @author ambud
 */
public class Acker extends GroupByHandler {

	private static final float ACKER_MAP_LOAD_FACTOR = 0.9f;
	private static final int ACKER_MAP_SIZE = 1000000;
	private RotatingMap<Long, AckerEntry> ackerMap;

	public Acker(int taskId, MutableInt taskCount) {
		super(taskId, taskCount);
		ackerMap = new RotatingMap<>(3);
	}

	@Override
	public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		Object sourceId = event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY);
		if (sourceId == null) {
			// tick event
			expireEvents();
		} else {
			updateAckerMap((Long) sourceId, (Long) event.getHeaders().get(Constants.FIELD_AGGREGATION_VALUE));
		}
	}

	public void expireEvents() {
		Map<Long, AckerEntry> evictionEntries = ackerMap.rotate();
		for (Entry<Long, AckerEntry> entry : evictionEntries.entrySet()) {
			if (entry.getValue().isComplete()) {
				// exception; entry was asynchronously acked
			} else {
				// notify source
			}
		}
	}

	/**
	 * 
	 * @param sourceId
	 * @param nextEvent
	 */
	public void updateAckerMap(Long sourceId, Long nextEvent) {
		AckerEntry trackerValue = ackerMap.get(sourceId);
		if (trackerValue == null) {
			// this is the first time we are seeing this event
			trackerValue = new AckerEntry(nextEvent);
			ackerMap.put(sourceId, trackerValue);
		} else {
			// event tree xor logic
			trackerValue.setValue(trackerValue.getValue() ^ nextEvent);
			if (trackerValue.isComplete()) {
				// means event processing tree is complete

				// notify source that event's completely processed

				// remove entry from ackerMap
				ackerMap.remove(sourceId);
			}
		}
	}

	public static class RotatingMap<K, V> {

		private LinkedList<Map<K, V>> buckets;

		public RotatingMap(int bucketCount) {
			buckets = new LinkedList<>();
			for (int i = 0; i < bucketCount; i++) {
				buckets.add(new HashMap<>(ACKER_MAP_SIZE / bucketCount, ACKER_MAP_LOAD_FACTOR));
			}
		}

		public Map<K, V> rotate() {
			Map<K, V> expiredMap = buckets.remove();
			buckets.addLast(new HashMap<>());
			return expiredMap;
		}

		public void put(K key, V value) {
			buckets.getLast().put(key, value);
		}

		public V get(K key) {
			for (Map<K, V> map : buckets) {
				V value = map.get(key);
				if (value != null) {
					return value;
				}
			}
			return null;
		}

		public V remove(K key) {
			for (Map<K, V> map : buckets) {
				V remove = map.remove(key);
				if (remove != null) {
					return remove;
				}
			}
			return null;
		}

	}

}
