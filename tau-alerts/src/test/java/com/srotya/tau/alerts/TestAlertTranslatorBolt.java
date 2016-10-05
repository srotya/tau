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
package com.srotya.tau.alerts;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.srotya.tau.alerts.AlertTranslatorBolt;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.TestUtils;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAlertTranslatorBolt {

	@Mock
	private OutputCollector mockCollector;
	@Mock
	private Tuple input;
	private List<String> eventsPositive = new ArrayList<>();
	private List<String> eventsNegative = new ArrayList<>();

	@Before
	public void before() throws IOException {
		List<String> lines = TestUtils.linesFromFiles("src/test/resources/alerts_positive.txt");
		eventsPositive.addAll(lines);
		lines = TestUtils.linesFromFiles("src/test/resources/alerts_negative.txt");
		eventsNegative.addAll(lines);
	}

	@Test
	public void testPrepare() {
		AlertTranslatorBolt bolt = new AlertTranslatorBolt();
		bolt.prepare(new HashMap<>(), null, mockCollector);
		assertEquals(mockCollector, bolt.getCollector());
	}

	@Test
	public void testExecutePositive() {
		AlertTranslatorBolt bolt = new AlertTranslatorBolt();
		for (String event : eventsPositive) {
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
			System.out.println("Positive event:" + event);
			when(input.getString(0)).thenReturn(event);
			bolt.execute(input);
			verify(collector, times(1)).emit(eq(input), any());
			verify(collector, times(1)).ack(input);
		}
	}

	@Test
	public void testExecuteNegative() {
		AlertTranslatorBolt bolt = new AlertTranslatorBolt();
		for (String event : eventsNegative) {
			OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					return new ArrayList<>();
				}
			});
			bolt.prepare(new HashMap<>(), null, collector);
			System.out.println("Negative event:" + event);
			when(input.getString(0)).thenReturn(event);
			bolt.execute(input);
			verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), eq(input), any());
			verify(collector, times(1)).ack(input);
		}
	}
}
