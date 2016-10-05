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
package com.srotya.tau.dengine.bolts;

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.bolts.StateTrackingBolt;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.store.AggregationStore;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Unit tests for {@link StateTrackingBolt}
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestStateTrackingBolt {

	@Mock
	private OutputCollector mockCollector;
	@Mock
	private Tuple tuple;
	@Mock
	private TopologyContext contex;

	@Before
	public void before() throws IOException {
	}

	@Test
	public void testInitialize() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		bolt.prepare(new HashMap<>(), contex, mockCollector);
		assertEquals(mockCollector, bolt.getCollector());
		assertEquals(1, bolt.getStateTrackingEngine().getTaskId());
		bolt.cleanup();
	}

	@Test
	public void testTrackStateEmit() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		Map<String, String> conf = new HashMap<>();
		// set flush buffer to 1 so flushes happen right away
		conf.put(StateTrackingBolt.STATE_FLUSH_BUFFER_SIZE, "2");
		bolt.prepare(conf, contex, collector);
		// send a tracking tuple for series 1
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
				.thenReturn(Utils.combineRuleActionId((short) 2, (short) 2));
		when(tuple.getStringByField(Constants.FIELD_RULE_GROUP)).thenReturn("test");
		when(tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW)).thenReturn(10);
		when(tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY)).thenReturn("series1");
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(true);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038054645L);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		// should have 1 entry in the tracking map
		assertEquals(1, bolt.getStateTrackingEngine().getAggregationMap().size());
		// prepare a tick tuple
		when(tuple.getSourceStreamId()).thenReturn(Constants.TICK_STREAM_ID);
		// execute 3 times to emit the tracked entry; 3 times because 10s jitter tolerance + 10s window size + 10s delay for emissions (i.e. only last bucket is emitted out)
		bolt.execute(tuple);
		bolt.execute(tuple);
		bolt.execute(tuple);
		// shouldn't have any entries in the map
		assertEquals(0, bolt.getStateTrackingEngine().getAggregationMap().size());
		// validate emit happened
		verify(collector, times(1)).emit(Mockito.eq(Constants.AGGREGATION_OUTPUT_STREAM), Mockito.eq(tuple), Mockito.any(Values.class));
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		bolt.execute(tuple);
		verify(collector, times(5)).ack(tuple);
		bolt.cleanup();
	}

	@Test
	public void testUntrackStateEmit() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		Map<String, String> conf = new HashMap<>();
		// set flush buffer to 1 so flushes happen right away
		conf.put(StateTrackingBolt.STATE_FLUSH_BUFFER_SIZE, "1");
		bolt.prepare(conf, contex, collector);
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
				.thenReturn(Utils.combineRuleActionId((short) 2, (short) 2));
		when(tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW)).thenReturn(10);
		when(tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY)).thenReturn("series1");
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(true);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038054645L);
		// send state track event
		bolt.execute(tuple);
		verify(collector, times(1)).ack(tuple);
		assertEquals(1, bolt.getStateTrackingEngine().getAggregationMap().size());
		when(tuple.getSourceStreamId()).thenReturn(Constants.TICK_STREAM_ID);
		bolt.execute(tuple);
		verify(collector, times(2)).ack(tuple);
		// send tick stream, shouldn't emit anything
		assertEquals(1, bolt.getStateTrackingEngine().getAggregationMap().size());
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(false);
		// send untrack request
		bolt.execute(tuple);
		verify(collector, times(3)).ack(tuple);
		assertEquals(1, bolt.getStateTrackingEngine().getAggregationMap().size());
		assertFalse(bolt.getStateTrackingEngine().getAggregationMap().values().iterator().next().isVal());
		when(tuple.getSourceStreamId()).thenReturn(Constants.TICK_STREAM_ID);
		bolt.execute(tuple);
		verify(collector, times(4)).ack(tuple);
		assertEquals(1, bolt.getStateTrackingEngine().getAggregationMap().size());
		bolt.execute(tuple);
		assertEquals(0, bolt.getStateTrackingEngine().getAggregationMap().size());
		verify(collector, times(5)).ack(tuple);
		bolt.cleanup();
	}
	
	@Test
	public void testAggregationReject() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		Map<String, String> conf = new HashMap<>();
		// set flush buffer to 1 so flushes happen right away
		conf.put(StateTrackingBolt.STATE_FLUSH_BUFFER_SIZE, "1");
		bolt.prepare(conf, contex, collector);
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
				.thenReturn(Utils.combineRuleActionId((short) 2, (short) 2));
		when(tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW)).thenReturn(10);
		when(tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY)).thenReturn("series1");
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(true);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038054645L);
		bolt.execute(tuple);
		verify(collector, times(1)).ack(tuple);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038034645L);
		bolt.execute(tuple);
		verify(collector, times(2)).ack(tuple);
	}
	
	@Test
	public void testTupleBatch() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		Map<String, String> conf = new HashMap<>();
		// set flush buffer to 1 so flushes happen right away
		conf.put(StateTrackingBolt.STATE_FLUSH_BUFFER_SIZE, "4");
		bolt.prepare(conf, contex, collector);
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
				.thenReturn(Utils.combineRuleActionId((short) 2, (short) 2));
		when(tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW)).thenReturn(10);
		when(tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY)).thenReturn("series1");
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(true);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038054645L);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(4)).ack(tuple);
	}
	
	@Test
	public void testTupleBatchFailure() {
		StateTrackingBolt bolt = new StateTrackingBolt();
		when(contex.getThisTaskIndex()).thenReturn(1);
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		Map<String, String> conf = new HashMap<>();
		// set flush buffer to 1 so flushes happen right away
		conf.put(StateTrackingBolt.STATE_FLUSH_BUFFER_SIZE, "4");
		conf.put(Constants.ASTORE_TYPE, TestFailureAggregationStore.class.getName());
		bolt.prepare(conf, contex, collector);
		when(tuple.getSourceStreamId()).thenReturn(Constants.STATE_STREAM_ID);
		when(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
				.thenReturn(Utils.combineRuleActionId((short) 2, (short) 2));
		when(tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW)).thenReturn(10);
		when(tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY)).thenReturn("series1");
		when(tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)).thenReturn(true);
		when(tuple.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1464038054645L);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(0)).ack(tuple);
		bolt.execute(tuple);
		verify(collector, times(4)).fail(tuple);
	}
	
	public static class TestFailureAggregationStore implements AggregationStore {

		@Override
		public void initialize(Map<String, String> conf) {
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
			throw new IOException();
		}

		@Override
		public Map<String, MutableBoolean> retriveStates(int taskId) throws IOException {
			return new HashMap<>();
		}

		@Override
		public void purgeState(int taskId, String key) throws IOException {
		}

		@Override
		public Map<String, Aggregator> retrive(int taskId, Aggregator aggregator) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}