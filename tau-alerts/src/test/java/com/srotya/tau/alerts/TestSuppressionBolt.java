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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.srotya.tau.alerts.SuppressionBolt;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.bolts.TestAlertingEngineBolt;
import com.srotya.tau.dengine.bolts.TestStore;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSuppressionBolt {

	@Mock
	private OutputCollector mockCollector;
	@Mock
	private Tuple input;
	private Map<String, String> conf;

	@Before
	public void before() throws IOException {
		conf = new HashMap<>();
		conf.put(TestAlertingEngineBolt.TEMPLATE_CONTENT,
				AlertTemplateSerializer.serialize(new AlertTemplate[] {
						new AlertTemplate((short) 0, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 5, 2),
						new AlertTemplate((short) 1, "t2", "t2@xyz.com", "mail", "t2", "hello t2", 10, 1) }));
		conf.put(Constants.TSTORE_TYPE, TestStore.class.getName());
	}

	@Test
	public void testPrepare() {
		SuppressionBolt bolt = new SuppressionBolt();
		bolt.prepare(conf, null, mockCollector);
		assertEquals(mockCollector, bolt.getCollector());
		assertEquals(2, bolt.getTemplateMap().size());
	}

	@Test
	public void testSuppressionMocked() {
		SuppressionBolt bolt = new SuppressionBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		conf.put(TestAlertingEngineBolt.TEMPLATE_CONTENT, AlertTemplateSerializer.serialize(new AlertTemplate[] {
				new AlertTemplate((short) 0, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 2, 2) }));
		bolt.prepare(conf, null, collector);
		Alert alert = new Alert();
		alert.setId((short) 0);
		alert.setBody("test");
		alert.setTimestamp(3253454235L);
		when(input.contains(Constants.FIELD_ALERT)).thenReturn(true);
		when(input.getValueByField(Constants.FIELD_ALERT)).thenReturn(alert);
		int i = 0;
		for (i = 0; i < 2; i++) {
			bolt.execute(input);
			assertEquals(i + 1, bolt.getCounter().get(alert.getId()).getVal());
			verify(collector, times(i + 1)).emit(eq(Constants.DELIVERY_STREAM), eq(input), any());
			assertEquals(alert, (Alert) processedEventContainer.get().get(0));
			verify(collector, times(i + 1)).ack(input);
		}
		bolt.execute(input);
		assertEquals(i + 1, bolt.getCounter().get(alert.getId()).getVal());
		verify(collector, times(i + 1)).ack(input);
		when(input.contains(Constants.FIELD_ALERT)).thenReturn(false);
		when(input.getSourceComponent()).thenReturn(backtype.storm.Constants.SYSTEM_COMPONENT_ID);
		when(input.getSourceStreamId()).thenReturn(backtype.storm.Constants.SYSTEM_TICK_STREAM_ID);
		bolt.execute(input);
		assertEquals(0, bolt.getCounter().get(alert.getId()).getVal());
	}

	@Test
	public void testSuppressionTick() {
		SuppressionBolt bolt = new SuppressionBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		conf.put(TestAlertingEngineBolt.TEMPLATE_CONTENT, AlertTemplateSerializer.serialize(new AlertTemplate[] {
				new AlertTemplate((short) 0, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 2, 2) }));
		bolt.prepare(conf, null, collector);
		Alert alert = new Alert();
		alert.setId((short) 0);
		alert.setBody("test");
		alert.setTimestamp(3253454235L);
		when(input.getValueByField(Constants.FIELD_ALERT)).thenReturn(alert);
		int i = 0;
		int j = 1;
		for (i = 1; i < 5; i++) {
			when(input.contains(Constants.FIELD_ALERT)).thenReturn(true);
			bolt.execute(input);
			assertEquals(j % 3, bolt.getCounter().get(alert.getId()).getVal());
			verify(collector, times(i)).emit(eq(Constants.DELIVERY_STREAM), eq(input), any());
			assertEquals(alert, (Alert) processedEventContainer.get().get(0));
			verify(collector, times(j++)).ack(input);
			if (i % 2 == 0) {
				when(input.contains(Constants.FIELD_ALERT)).thenReturn(false);
				when(input.getSourceComponent()).thenReturn(backtype.storm.Constants.SYSTEM_COMPONENT_ID);
				when(input.getSourceStreamId()).thenReturn(backtype.storm.Constants.SYSTEM_TICK_STREAM_ID);
				long counter = bolt.getGlobalCounter();
				bolt.execute(input);
				verify(collector, times(j++)).ack(input);
				assertEquals(counter + 1, bolt.getGlobalCounter());
			}
		}
	}

	@Test
	public void testSuppressionTemplateUpdates() {
		SuppressionBolt bolt = new SuppressionBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		conf.put(TestAlertingEngineBolt.TEMPLATE_CONTENT, AlertTemplateSerializer.serialize(new AlertTemplate[] {
				new AlertTemplate((short) 0, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 5, 2) }));
		bolt.prepare(conf, null, collector);

		assertEquals(1, bolt.getTemplateMap().size());

		TemplateCommand cmd = new TemplateCommand("rgxyz", false, AlertTemplateSerializer
				.serialize(new AlertTemplate((short) 1, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 5, 2), false));
		when(input.getValueByField(Constants.FIELD_TEMPLATE_CONTENT)).thenReturn(cmd);
		when(input.getSourceStreamId()).thenReturn(Constants.SYNC_STREAM_ID);
		when(input.getSourceComponent()).thenReturn(Constants.TEMPLATE_SYNC_COMPONENT);
		bolt.execute(input);
		assertEquals(2, bolt.getTemplateMap().size());
		
		cmd = new TemplateCommand("rgxyz", true, AlertTemplateSerializer
				.serialize(new AlertTemplate((short) 1, "t1", "t1@xyz.com", "mail", "t1", "hello t1", 5, 2), false));
		when(input.getValueByField(Constants.FIELD_TEMPLATE_CONTENT)).thenReturn(cmd);
		bolt.execute(input);
		assertEquals(1, bolt.getTemplateMap().size());
	}
}
