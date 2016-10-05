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
package com.srotya.tau.wraith.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TestFactory;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.AlertAction;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.ConditionSerializer;
import com.srotya.tau.wraith.conditions.logical.AndCondition;
import com.srotya.tau.wraith.conditions.logical.OrCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;
import com.srotya.tau.wraith.rules.RuleCommand;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

/**
 * Serialization deserialization unit tests
 * 
 * @author ambud_sharma
 */
public class TestSerialization {

	@Test
	public void testRuleSerialDeserialization() {
		Condition one = new OrCondition(Arrays.asList((Condition) new EqualsCondition("header1", "val1"),
				new EqualsCondition("test", "asdas")));
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Action action = new TemplatedAlertAction((short) 2, (short) 2);
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);
		SimpleRule deserializedRule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
		assertEquals(rule, deserializedRule);
	}

	@Test
	public void testRuleSerialDeserializationRegex() {
		Condition one = new JavaRegexCondition("header", "\\d+");
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Action action = new AlertAction((short) 2, "test", "test", "test");
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);
		System.out.println(jsonRule);
		SimpleRule deserializedRule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
		assertEquals(rule, deserializedRule);
		System.out.println(((AndCondition) deserializedRule.getCondition()).getConditions().get(0));
		System.out.println();
	}

	@Test
	public void testRuleSerialDeserializationRE2Regex() {
		Condition one = new JavaRegexCondition("header", "\\d+");
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Action action = new AlertAction((short) 2, "test", "test", "test");
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);
		System.out.println(jsonRule);
		SimpleRule deserializedRule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
		assertEquals(rule, deserializedRule);
		System.out.println(((AndCondition) deserializedRule.getCondition()).getConditions().get(0));
		System.out.println();
	}

	@Test
	public void testRuleSerialization() {
		Condition one = new OrCondition(Arrays.asList((Condition) new EqualsCondition("header1", "val1"),
				(Condition) new JavaRegexCondition("header", "\\d+")));
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Action action = new TemplatedAlertAction((short) 2, (short) 2);
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);

		JsonObject object = new Gson().fromJson(jsonRule, JsonObject.class);
		assertEquals(123, object.get("ruleId").getAsShort());
		assertEquals("hello", object.get("name").getAsString());
		JsonObject actionArray = object.get("actions").getAsJsonArray().get(0).getAsJsonObject();
		assertEquals(Utils.CLASSNAME_FORWARD_MAP.get(TemplatedAlertAction.class.getCanonicalName()),
				actionArray.get("type").getAsString());
	}

	@Test
	public void testConditionSerialization() {
		Condition one = new OrCondition(Arrays.asList((Condition) new EqualsCondition("header1", "val1"),
				(Condition) new JavaRegexCondition("header", "\\d+")));
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Gson gson = new GsonBuilder().registerTypeAdapter(Condition.class, new ConditionSerializer()).create();
		String json = gson.toJson(condition);

		JsonArray object = new Gson().fromJson(json, JsonObject.class).get("conditions").getAsJsonArray();
		assertEquals(Utils.CLASSNAME_FORWARD_MAP.get(OrCondition.class.getCanonicalName()),
				object.get(0).getAsJsonObject().get("type").getAsString());
		assertEquals(Utils.CLASSNAME_FORWARD_MAP.get(EqualsCondition.class.getCanonicalName()),
				object.get(1).getAsJsonObject().get("type").getAsString());
	}

	@Test
	public void testRuleCommandSerialization() {
		Condition one = new OrCondition(Arrays.asList((Condition) new EqualsCondition("header1", "val1"),
				new EqualsCondition("test", "asdas")));
		Condition two = new EqualsCondition("header2", "val2");
		Condition condition = new AndCondition(Arrays.asList(one, two));

		Action action = new AlertAction((short) 2, "test", "test", "test");
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		RuleCommand cmd = new RuleCommand();
		cmd.setDelete(true);
		cmd.setRuleContent(RuleSerializer.serializeRuleToJSONString(rule, false));
		cmd.setRuleGroup("22444");

		Gson gson = new Gson();
		String json = gson.toJson(cmd);

		RuleCommand fromJson = gson.fromJson(json, RuleCommand.class);
		assertEquals(cmd.isDelete(), fromJson.isDelete());
		assertEquals(cmd.getRuleGroup(), fromJson.getRuleGroup());
		rule = RuleSerializer.deserializeJSONStringToRule(fromJson.getRuleContent());
		System.out.println("Command rule:" + rule);
		assertEquals(0, rule.getActions().get(0).getActionId());
	}

	@Test
	public void testBadRuleValidation() {
		Condition condition = new AndCondition(null);
		Action action = new AlertAction((short) 2, "test", "test", "test");
		SimpleRule rule = new SimpleRule((short) 123, "hello", true, condition, action);
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);
		System.err.println(jsonRule);
		try {
			rule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
			System.out.println("AndConditions:" + ((AndCondition) rule.getCondition()).getConditions());
			Assert.fail("Should have thrown an exception");
		} catch (JsonParseException e) {
		}
	}

	@Test
	public void testRegexSerialization() {
		Condition regexCondition = new JavaRegexCondition("host", ".*check_rtsock_rc.*");

		SimpleRule rule = new SimpleRule((short) 1, "test", true, regexCondition,
				new Action[] { new AlertAction((short) 0, "test", "email", "test") });
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);

		rule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
		Event event = new TestFactory().buildEvent();
		event.getHeaders().put("host",
				"MIB2D_RTSLIB_READ_FAILURE: check_rtsock_rc: failed in reading mac_db: 0 (Invalid argument)");
		assertTrue(rule.getCondition().matches(event));
	}

	@Test
	public void testRegexSerialization1() {
		JavaRegexCondition regexCondition = new JavaRegexCondition("host", "\\b\\w{13}\\.\\w{4}\\.\\w{6}\\.\\w{3}\\b");

		SimpleRule rule = new SimpleRule((short) 1, "test", true, regexCondition,
				new Action[] { new AlertAction((short) 0, "test", "email", "test") });
		String jsonRule = RuleSerializer.serializeRuleToJSONString(rule, false);
		System.out.println(regexCondition.getValue() + "\t" + jsonRule);
		rule = RuleSerializer.deserializeJSONStringToRule(jsonRule);
		Event event = new TestFactory().buildEvent();
		event.getHeaders().put("host", "testoneserver.test.symcpe.com");
		assertTrue(rule.getCondition().matches(event));
	}

}