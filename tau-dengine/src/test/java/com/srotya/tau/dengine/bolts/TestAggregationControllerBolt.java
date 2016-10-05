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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.TopologyTestRulesStore;
import com.srotya.tau.dengine.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.StateAggregationAction;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.RuleCommand;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAggregationControllerBolt {

	@Mock
	private OutputCollector collector;
	@Mock
	private Tuple tuple;

	@Test
	public void testInitialize() {
		Map<String, String> conf = new HashMap<>();
		conf.put(Constants.RSTORE_TYPE, TopologyTestRulesStore.class.getName());
		RuleCommand rc = new RuleCommand("test", false, RuleSerializer.serializeRulesToJSONString(Arrays
				.asList(new SimpleRule((short) 2, "test", true, new EqualsCondition("test", "test"), new Action[] {
						new StateAggregationAction((short) 0, "test", 100, new EqualsCondition("test", "test")) })),
				false));
		conf.put(TestAlertingEngineBolt.RULES_CONTENT, new Gson().toJson(new RuleCommand[] { rc }));
		AggregationControllerBolt bolt = new AggregationControllerBolt();
		bolt.prepare(conf, null, collector);
		assertEquals(1, bolt.getRuleGroupMap().get("test").size());
	}

	@Test
	public void testTickTuple() {
		Map<String, String> conf = new HashMap<>();
		conf.put(Constants.RSTORE_TYPE, TopologyTestRulesStore.class.getName());
		AggregationControllerBolt bolt = new AggregationControllerBolt();
		RuleCommand rc = new RuleCommand("test", false, RuleSerializer.serializeRulesToJSONString(Arrays
				.asList(new SimpleRule((short) 2, "test", true, new EqualsCondition("test", "test"), new Action[] {
						new StateAggregationAction((short) 0, "test", 2, new EqualsCondition("test", "test")) })),
				false));
		conf.put(TestAlertingEngineBolt.RULES_CONTENT, new Gson().toJson(new RuleCommand[] { rc }));
		bolt.prepare(conf, null, collector);
		when(tuple.getSourceComponent()).thenReturn(backtype.storm.Constants.SYSTEM_COMPONENT_ID);
		when(tuple.getSourceStreamId()).thenReturn(backtype.storm.Constants.SYSTEM_TICK_STREAM_ID);
		assertEquals(1, bolt.getTickCounter());
		bolt.execute(tuple);
		assertEquals(2, bolt.getTickCounter());
		verify(collector, times(1)).ack(tuple);
		verify(collector, times(1)).emit(Constants.TICK_STREAM_ID, tuple,
				new Values(Utils.combineRuleActionId((short) 2, (short) 0), 2, "test"));
	}

	@Test
	public void testRuleSyncTuple() {
		Map<String, String> conf = new HashMap<>();
		conf.put(Constants.RSTORE_TYPE, TopologyTestRulesStore.class.getName());
		AggregationControllerBolt bolt = new AggregationControllerBolt();
		SimpleRule rule = new SimpleRule((short) 2, "test", true, new EqualsCondition("test", "test"), new Action[] {
				new StateAggregationAction((short) 0, "test", 20, new EqualsCondition("test", "test")) });
		RuleCommand rc = new RuleCommand("test", false, RuleSerializer.serializeRulesToJSONString(Arrays
				.asList(rule),
				false));
		conf.put(TestAlertingEngineBolt.RULES_CONTENT, new Gson().toJson(new RuleCommand[] { rc }));
		bolt.prepare(conf, null, collector);
		assertEquals(1, bolt.getRuleGroupMap().size());
		rule = new SimpleRule((short) 3, "test", true, new EqualsCondition("test", "test"), new Action[] {
				new StateAggregationAction((short) 0, "test", 20, new EqualsCondition("test", "test")) });
		Tuple tuple = MockTupleHelpers.mockRuleTuple(false, "test", RuleSerializer.serializeRuleToJSONString(rule, false));
		bolt.execute(tuple);
		assertEquals(2, bolt.getRuleGroupMap().get("test").size());
		verify(collector, times(1)).ack(tuple);
	}

}
