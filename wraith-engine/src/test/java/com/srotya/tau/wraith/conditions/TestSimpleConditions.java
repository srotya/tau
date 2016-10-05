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
package com.srotya.tau.wraith.conditions;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TestEvent;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.conditions.relational.ExistsCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanEqualToCondition;
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanEqualToCondition;

/**
 * All tests for simple comparision conditions
 * 
 * @author ambud_sharma
 */
public class TestSimpleConditions {
	
	private Event testEvent = new TestEvent();
	
	@Before
	public void before() {
		Map<String, Object> headers = testEvent.getHeaders();
		headers.put("host", "test212");
		headers.put("name", "test");
		headers.put("key", 2);
		headers.put("key2", 2.5);
	}
	
	@Test
	public void testEqualsCondition() {
		Condition condition = new EqualsCondition("host", "test212");
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testExistsCondition() {
		Condition condition = new ExistsCondition("host");
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testGreaterThanCondition() {
		Condition condition = new GreaterThanCondition("key", 1);
		assertTrue(condition.matches(testEvent));
		condition = new GreaterThanCondition("key2", 1.5);
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testLessThanCondition() {
		Condition condition = new LessThanCondition("key", 4);
		assertTrue(condition.matches(testEvent));
		condition = new LessThanCondition("key2", 4.5);
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testGreaterThanEqualCondition() {
		Condition condition = new GreaterThanEqualToCondition("key", 2);
		assertTrue(condition.matches(testEvent));
		condition = new GreaterThanCondition("key2", 1.5);
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testLessThanEqualsCondition() {
		Condition condition = new LessThanEqualToCondition("key", 2);
		assertTrue(condition.matches(testEvent));
		condition = new LessThanCondition("key2", 4.5);
		assertTrue(condition.matches(testEvent));
	}

	@Test
	public void testRegexConditionPositive() {
		Condition condition = new JavaRegexCondition("host", "t.*");
		assertTrue(condition.matches(testEvent));
	}
	
	@Test
	public void testRegexConditionNegative() {
		Condition condition = new JavaRegexCondition("host", "\\d+");
		assertTrue(!condition.matches(testEvent));
	}
	
}
