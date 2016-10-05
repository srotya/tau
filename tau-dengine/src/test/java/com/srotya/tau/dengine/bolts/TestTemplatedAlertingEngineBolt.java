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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.TestUtils;
import com.srotya.tau.dengine.bolts.TemplatedAlertingEngineBolt;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Test Alert generation and velocity templates
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestTemplatedAlertingEngineBolt {

	public static final String RULES_CONTENT = "rstore.rules.content";
	public static final String TEMPLATE_CONTENT = "tstore.template.content";
	private List<String> events = new ArrayList<>();
	@Mock
	private OutputCollector collector;
	@Mock
	private Tuple input;
	private Map<String, String> stormConf = new HashMap<>();

	@Before
	public void before() throws IOException {
		List<String> lines = TestUtils.linesFromFiles("src/test/resources/events.json");
		events.addAll(lines);
		stormConf.put(Constants.RSTORE_TYPE, TestStore.class.getName());
		stormConf.put(TEMPLATE_CONTENT,
				AlertTemplateSerializer.serialize(new AlertTemplate[] {
						new AlertTemplate((short) 0, "t1", "t1@xyz.com", "mail", "t1", "$host t1", 5, 2),
						new AlertTemplate((short) 1, "t2", "t2@xyz.com", "mail", "t2", "hello t2", 10, 1) }));
		stormConf.put(Constants.TSTORE_TYPE, TestStore.class.getName());
	}

	@Test
	public void testPerpare() {
		TemplatedAlertingEngineBolt bolt = new TemplatedAlertingEngineBolt();
		bolt.prepare(stormConf, null, collector);
		assertEquals(2, bolt.getTemplateMap().size());
	}

	@Test
	public void testAlertExecution() {
		TemplatedAlertingEngineBolt bolt = new TemplatedAlertingEngineBolt();
		when(input.getSourceStreamId()).thenReturn(Constants.EVENT_STREAM_ID);
		int hostCounter = 0;
		for (String event : events) {
			final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
			bolt.prepare(stormConf, null, MockTupleHelpers.mockCollector(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object newEvent = invocation.getArguments()[2];
					processedEventContainer.set((Values) newEvent);
					System.out.println("Alert emitted: " + processedEventContainer.get());
					return new ArrayList<>();
				}
			}));
			when(input.getValueByField(Constants.FIELD_EVENT)).thenReturn(TestUtils.stringToEvent(event));
			when(input.getShortByField(Constants.FIELD_RULE_ID)).thenReturn((short) 1123);
			when(input.getStringByField(Constants.FIELD_RULE_NAME)).thenReturn("hello");
			when(input.getStringByField(Constants.FIELD_RULE_GROUP)).thenReturn("test");
			when(input.getLongByField(Constants.FIELD_TIMESTAMP)).thenReturn(1L);
			when(input.getShortByField(Constants.FIELD_ALERT_TEMPLATE_ID)).thenReturn((short) 0);
			when(input.getShortByField(Constants.FIELD_ACTION_ID)).thenReturn((short) 0);
			bolt.execute(input);
			Gson gson = new Gson();
			Alert alert = gson.fromJson(processedEventContainer.get().get(0).toString(), Alert.class);
			System.out.println("Alert:::" + processedEventContainer.get().get(0).toString());
			assertEquals("mail", alert.getMedia());
			if (alert.getBody().contains("test")) {
				hostCounter++;
			}
		}
		assertEquals(4, hostCounter);
	}

}