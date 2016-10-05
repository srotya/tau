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
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.TauEvent;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.TestUtils;
import com.srotya.tau.dengine.bolts.JSONTranslatorBolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Tests for JSON Translator bolt
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestJSONTranslatorBolt {

	private List<String> events = new ArrayList<>();
	@Mock
	private OutputCollector mockCollector;
	@Mock
	private Tuple input;

	@Before
	public void before() throws IOException {
		List<String> lines = TestUtils.linesFromFiles("src/test/resources/events.json");
		events.addAll(lines);
	}

	@Test
	public void testPrepare() {
		JSONTranslatorBolt bolt = new JSONTranslatorBolt();
		bolt.prepare(new HashMap<>(), null, mockCollector);
		assertNotNull(bolt.getFactory());
		assertNotNull(bolt.getGson());
		assertNotNull(bolt.getType());
		assertNotNull(bolt.getCollector());
		assertEquals(mockCollector, bolt.getCollector());
	}

	@Test
	public void testExecutePositive() {
		JSONTranslatorBolt bolt = new JSONTranslatorBolt();
		for (String event : events) {
			final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
			OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object newEvent = invocation.getArguments()[1];
					processedEventContainer.set((Values) newEvent);
					return new ArrayList<>();
				}
			});
			bolt.prepare(new HashMap<>(), null, collector);
			when(input.getString(0)).thenReturn(event);
			bolt.execute(input);
			assertNotNull(processedEventContainer.get());
			assertTrue(processedEventContainer.get().size()>0);
			TauEvent processedEvent = (TauEvent) processedEventContainer.get().get(0);
			assertTrue(processedEvent.getHeaders().size() > 0);
			// validate timestamp exists
			assertTrue(processedEvent.getHeaders().containsKey(Constants.FIELD_TIMESTAMP));
			// validate timestamp was copied
			assertEquals(((Double)processedEvent.getHeaders().get("@timestamp")).longValue(), processedEvent.getHeaders().get(Constants.FIELD_TIMESTAMP));
			// validate rule group was copied
			assertEquals(processedEvent.getHeaders().get("rule_group"), processedEvent.getHeaders().get(Constants.FIELD_RULE_GROUP));
			// validate event was successfully translated to a hashmap
			verify(collector, times(1)).ack(input);
		}
	}
	
	@Test
	public void testExecuteNegative() {
		JSONTranslatorBolt bolt = new JSONTranslatorBolt();
		for (String event : events) {
			final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
			OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object newEvent = invocation.getArguments()[2];
					processedEventContainer.set((Values) newEvent);
					return new ArrayList<>();
				}
			});
			bolt.prepare(new HashMap<>(), null, collector);
			when(input.getString(0)).thenReturn(event+"}");
			bolt.execute(input);
			verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), eq(input), any());
			verify(collector, times(1)).ack(input);
		}
	}

}
