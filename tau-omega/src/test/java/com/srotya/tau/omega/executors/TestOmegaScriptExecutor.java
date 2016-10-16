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
package com.srotya.tau.omega.executors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.srotya.tau.omega.OmegaLogger;
import com.srotya.tau.omega.ScriptAction;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TestFactory;
import com.srotya.tau.wraith.actions.alerts.AlertAction;
import com.srotya.tau.wraith.actions.omega.OmegaAction.LANGUAGE;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

/**
 * @author ambudsharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestOmegaScriptExecutor {

	@Mock
	private OmegaLogger logger;

	@Test
	public void testInitialization() throws Exception {
		Map<String, String> conf = new HashMap<>();
		conf.put(TestFactory.RULES_CONTENT,
				RuleSerializer.serializeRulesToJSONString(Arrays.asList(new SimpleRule((short) 1122, "test1", true,
						new EqualsCondition("host", "val"), new AlertAction((short) 2, "test", "test", "test"))),
						false));
		TestFactory factory = new TestFactory();
		OmegaScriptExecutor executor = new OmegaScriptExecutor(factory);
		executor.initialize(conf);
		assertEquals(0, executor.getScriptLookup().size());

		conf.put(TestFactory.RULES_CONTENT,
				RuleSerializer
						.serializeRulesToJSONString(
								Arrays.asList(
										new SimpleRule((short) 1122, "test1", true,
												new EqualsCondition("host", "val"), new ScriptAction((short) 0,
														LANGUAGE.Javascript, "logger.log('hello');"))),
								false));
		executor.initialize(conf);
		assertEquals(1, factory.getRulesStore(null, conf).listGroupedRules().size());
		assertEquals(1, executor.getScriptLookup().size());
		assertEquals(1, executor.getLoggerLookup().size());
		assertNotNull(executor.getNashornEngine());
		assertNotNull(executor.getJrubyEngine());
	}

	@Test
	public void testJavascriptExecution() throws Exception {
		String script = "function f()\n{ \n\tlogger.log('hello');\n\treturn true;\n}\nf();";
		System.err.println("Javascript test script:\n"+script);
		Map<String, String> conf = new HashMap<>();
		conf.put(TestFactory.RULES_CONTENT,
				RuleSerializer.serializeRulesToJSONString(
						Arrays.asList(new SimpleRule((short) 1122, "test1",
								true, new EqualsCondition("host", "val"), new ScriptAction((short) 0,
										LANGUAGE.Javascript, script))),
						false));
		TestFactory factory = new TestFactory();
		OmegaScriptExecutor executor = new OmegaScriptExecutor(factory);
		executor.initialize(conf);
		executor.getLoggerLookup().put("all", logger);
		Event event = factory.buildEvent();
		event.getHeaders().put("host", "test");
		boolean result = executor.executeScript("all", (short) 1122, (short) 0, event);
		assertTrue(result);
		verify(logger, times(1)).log("hello");
	}
	
	@Test
	public void testJrubyExecution() throws Exception {
		String script = "def f(logger)\n\tlogger.log('hello')\n\treturn true\nend\nf(logger)";
		System.err.println("JRuby test script:\n"+script);
		Map<String, String> conf = new HashMap<>();
		conf.put(TestFactory.RULES_CONTENT,
				RuleSerializer.serializeRulesToJSONString(
						Arrays.asList(new SimpleRule((short) 1122, "test1",
								true, new EqualsCondition("host", "val"), new ScriptAction((short) 0,
										LANGUAGE.JRuby, script))),
						false));
		TestFactory factory = new TestFactory();
		OmegaScriptExecutor executor = new OmegaScriptExecutor(factory);
		executor.initialize(conf);
		executor.getLoggerLookup().put("all", logger);
		Event event = factory.buildEvent();
		event.getHeaders().put("host", "test");
		boolean result = executor.executeScript("all", (short) 1122, (short) 0, event);
		assertTrue(result);
		verify(logger, times(1)).log("hello");
	}

}
