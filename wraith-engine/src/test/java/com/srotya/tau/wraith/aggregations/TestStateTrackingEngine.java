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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.TestFactory;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregations.StateTrackingEngine;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.store.AggregationStore;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.StoreFactory;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * @author ambud_sharma
 */
public class TestStateTrackingEngine {
	
	private TestFactory factory;

	@Before
	public void before() {
		factory = new TestFactory();
	}

	@Test
	public void testKeyBucketing() {
		short rule = 2;
		short action = 2;
		String ruleActionId = Utils.combineRuleActionId(rule, action);
		String aggregationKey = "series1";
		long timestamp = 1461272081103L;
		int aggregationWindow = 233;
		String key = Utils.createMapKey(timestamp, aggregationWindow, ruleActionId, aggregationKey);
		assertEquals("AAIAAg==_57193d29_series1", key);
	}

	@Test
	public void testTracking() throws Exception {
		StateTrackingEngine engine = new StateTrackingEngine(factory, new TestStateFactory());
		engine.initialize(new HashMap<>(), 1);
		engine.track(1461272081000L, 10, Utils.combineRuleActionId((short) 2, (short) 3), "series1");
		engine.track(1461272082000L, 10, Utils.combineRuleActionId((short) 2, (short) 3), "series1");
		engine.track(1461272083000L, 10, Utils.combineRuleActionId((short) 2, (short) 3), "series1");
		engine.track(1461272084000L, 10, Utils.combineRuleActionId((short) 2, (short) 3), "series1");
		// note the sort order based on action id above and that the first key
		// is still r:2 a:2 and will be untracked
		engine.track(1461272081000L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		engine.track(1461272082000L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		engine.track(1461272083000L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		engine.track(1461272084000L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		engine.untrack(146127209100L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		assertEquals(2, engine.getAggregationMap().size());
		assertEquals(true, engine.getAggregationMap().values().iterator().next().isVal());
		assertTrue(Utils.containsRuleActionId(engine.getAggregationMap(), Utils.combineRuleActionId((short) 2, (short) 2)));
		assertFalse(Utils.containsRuleActionId(engine.getAggregationMap(), Utils.combineRuleActionId((short) 3, (short) 2)));
		assertFalse(Utils.containsRuleActionId(engine.getAggregationMap(), Utils.combineRuleActionId((short) 2, (short) 1)));
		assertFalse(Utils.containsRuleActionId(engine.getAggregationMap(), Utils.combineRuleActionId((short) 2, (short) 4)));
		engine.untrack(1461272085000L, 10, Utils.combineRuleActionId((short) 2, (short) 2), "series1");
		assertEquals(2, engine.getAggregationMap().size());
		assertEquals(false, engine.getAggregationMap().values().iterator().next().isVal());
	}

	@Test
	public void testStateDataRejection() throws Exception {
		StateTrackingEngine engine = new StateTrackingEngine(factory, new TestStateFactory());
		String raId = Utils.combineRuleActionId((short) 2, (short) 3);
		Map<String, String> conf = new HashMap<>();
		conf.put(Constants.AGGREGATION_JITTER_TOLERANCE, "0");
		engine.initialize(conf, 1);
		try {
			engine.track(1461272081000L, 10, raId, "series1");
			engine.track(1461272082000L, 10, raId, "series1");
			engine.track(1461272083000L, 10, raId, "series1");
			engine.track(1461272084000L, 10, raId, "series1");
			List<Event> emits = new ArrayList<>();
			engine.emit(10, raId, emits);
			assertEquals(0, emits.size());
			engine.emit(10, raId, emits);
			assertEquals(1, emits.size());
			engine.track(1461272071000L, 10, raId, "series1");
			fail("Can't execute because exception should be thrown");
		} catch (Exception e) {
		}
	}

	@Test
	public void testStoreFlush() throws Exception {
		StateTrackingEngine engine = new StateTrackingEngine(factory, new TestStateFactory());
		String raId = Utils.combineRuleActionId((short) 2, (short) 3);
		Map<String, String> conf = new HashMap<>();
		conf.put(Constants.ASTORE_TYPE, "io.symcpe.wraith.aggregations.TestStateTrackingEngine.TestStateAggregationStore");
		conf.put(Constants.AGGREGATION_JITTER_TOLERANCE, "0");
		engine.initialize(conf, 1);
		assertNotNull(engine.getStore());
		engine.track(1461272081000L, 10, raId, "series1");
		engine.track(1461272082000L, 10, raId, "series1");
		engine.track(1461272083000L, 10, raId, "series1");
		engine.track(1461272084000L, 10, raId, "series1");
		engine.track(1461272090000L, 10, raId, "series1");
		assertEquals(2, engine.getAggregationMap().size());
		assertEquals(2, engine.getFlushMap().size());
		engine.flush();
		assertEquals(2, TestStateAggregationStore.store.size());
		List<Event> emits = new ArrayList<>();
		engine.emit(10, raId, emits);
		assertEquals(1, emits.size());
		assertEquals(1, engine.getAggregationMap().size());
		assertEquals(1, engine.getFlushMap().size());
		assertEquals(1, TestStateAggregationStore.store.size());
		engine = new StateTrackingEngine(factory, new TestStateFactory());
		engine.initialize(conf, 1);
		assertEquals(1, engine.getAggregationMap().size());
		assertEquals(0, engine.getFlushMap().size());
		assertEquals(1, TestStateAggregationStore.store.size());
	}

	public static class TestStateFactory implements StoreFactory {

		@Override
		public RulesStore getRulesStore(String type, Map<String, String> conf) throws Exception {
			return null;
		}

		@Override
		public TemplateStore getTemplateStore(String type, Map<String, String> conf) throws Exception {
			return null;
		}

		@Override
		public AggregationStore getAggregationStore(String type, Map<String, String> conf) throws Exception {
			TestStateAggregationStore store = new TestStateAggregationStore();
			store.initialize(conf);
			return store;
		}

	}

	public static class TestStateAggregationStore implements AggregationStore {

		private static SortedMap<String, MutableBoolean> store;

		@Override
		public void initialize(Map<String, String> conf) {
			if (store == null) {
				System.err.println("Initializing test state aggregation store");
				store = new TreeMap<>();
			}
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public void disconnect() throws IOException {
		}

		@Override
		public void putValue(int taskId, long timestamp, String entity, long count) throws IOException {
		}

		@Override
		public void putValue(int taskId, long timestamp, String entity, int count) throws IOException {
		}

		@Override
		public void persist(int taskId, String entity, Aggregator aggregator) throws IOException {
		}

		@Override
		public void mergeSetValues(int taskId, String entity, Set<Object> values) throws IOException {
		}

		@Override
		public void mergeSetIntValues(int taskId, String entity, Set<Integer> values) throws IOException {
		}

		@Override
		public void putValue(int taskId, String entity, ICardinality value) throws IOException {
		}

		@Override
		public void persistState(int taskId, String key, MutableBoolean value) throws IOException {
			store.put("state_" + taskId + "_" + key, value);
		}

		@Override
		public Map<String, MutableBoolean> retriveStates(int taskId) throws IOException {
			return store.subMap("state_" + taskId + "_", "state_" + taskId + "_" + Character.MAX_VALUE);
		}

		@Override
		public void purgeState(int taskId, String key) throws IOException {
			store.remove("state_" + taskId + "_" + key);
		}

		/**
		 * @return the store
		 */
		public SortedMap<String, MutableBoolean> getStore() {
			return store;
		}

		@Override
		public Map<String, Aggregator> retrive(int taskId, Aggregator aggregator) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

	}
}