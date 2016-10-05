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
import java.util.Arrays;
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

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.TestUtils;
import com.srotya.tau.dengine.bolts.AlertingEngineBolt;
import com.srotya.tau.wraith.actions.alerts.AlertAction;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Test Alert generation and velocity templates
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAlertingEngineBolt {

	public static final String RULES_CONTENT = "rstore.rules.content";
	public static final String TEMPLATE_CONTENT = "tstore.template.content";
	private List<String> events = new ArrayList<>();
	@Mock
	private OutputCollector collector;
	@Mock
	private Tuple input;
	private Map<String, String> stormConf = new HashMap<>();
	private Rule hostRule, valueRule;

	@Before
	public void before() throws IOException {
		List<String> lines = TestUtils.linesFromFiles("src/test/resources/events.json");
		events.addAll(lines);
		hostRule = new SimpleRule((short) 1123, "hostrule", true, new EqualsCondition("host", "one"),
				new AlertAction((short) 1, "test@symantec.com", "mail", "Hello $host!"));
		valueRule = new SimpleRule((short) 1124, "hostrule", true, new EqualsCondition("value", 5),
				new AlertAction((short) 1, "test2@symantec.com", "mail", "Hello $value!"));
		List<Rule> rules = Arrays.asList(new Rule[] { hostRule, valueRule });
		stormConf.put(RULES_CONTENT, RuleSerializer.serializeRulesToJSONString(rules, false));
		stormConf.put(Constants.RSTORE_TYPE, TestStore.class.getName());
	}

	@Test
	public void testPerpare() {
		AlertingEngineBolt bolt = new AlertingEngineBolt();
		bolt.prepare(stormConf, null, collector);
		assertEquals(2, bolt.getTemplateMap().size());
		assertEquals(2, bolt.getRuleMap().size());
	}

	@Test
	public void testAlertExecution() {
		AlertingEngineBolt bolt = new AlertingEngineBolt();
		when(input.getSourceStreamId()).thenReturn(Constants.EVENT_STREAM_ID);
		int hostCounter = 0;
		for (String event : events) {
			final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
			bolt.prepare(stormConf, null, MockTupleHelpers.mockCollector(new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Object newEvent = invocation.getArguments()[2];
					processedEventContainer.set((Values) newEvent);
					System.out.println("Alert emitted:" + processedEventContainer.get());
					return new ArrayList<>();
				}
			}));
			when(input.getValueByField(Constants.FIELD_EVENT)).thenReturn(TestUtils.stringToEvent(event));
			when(input.getShortByField(Constants.FIELD_RULE_ID)).thenReturn((short) 1123);
			when(input.getShortByField(Constants.FIELD_ACTION_ID)).thenReturn((short) 0);
			when(input.getStringByField(Constants.FIELD_ALERT_TARGET)).thenReturn("dlp");
			when(input.getStringByField(Constants.FIELD_ALERT_MEDIA)).thenReturn("mail");
			bolt.execute(input);
			assertEquals("dlp", processedEventContainer.get().get(0));
			assertEquals("mail", processedEventContainer.get().get(1));
			if (processedEventContainer.get().get(2).toString().contains("test")) {
				hostCounter++;
			}
		}
		assertEquals(4, hostCounter);
	}

}