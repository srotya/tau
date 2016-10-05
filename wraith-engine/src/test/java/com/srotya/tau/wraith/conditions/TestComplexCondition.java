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

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TestEvent;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.logical.AndCondition;
import com.srotya.tau.wraith.conditions.logical.NotCondition;
import com.srotya.tau.wraith.conditions.logical.OrCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;

/**
 * All unit tests for complex conditions
 * 
 * @author ambud_sharma
 */
public class TestComplexCondition {
	
	private Event testEvent = new TestEvent();
	
	@Before
	public void before() {
		Map<String, Object> headers = testEvent.getHeaders();
		headers.put("host", "test212");
		headers.put("name", "test");
	}
	
	/**
	 * When all conditions are true
	 */
	@Test
	public void testAndConditionPositive() {
		Condition condition1 = new EqualsCondition("host", "test212");
		Condition condition2 = new EqualsCondition("name", "test");
		Condition condition = new AndCondition(Arrays.asList(condition1, condition2));
		assertTrue(condition.matches(testEvent));
	}
	
	/**
	 * When at-least one condition is false
	 */
	@Test
	public void testAndConditionNegative() {
		Condition condition1 = new EqualsCondition("host", "test212");
		Condition condition2 = new EqualsCondition("name", "test2");
		Condition condition = new AndCondition(Arrays.asList(condition1, condition2));
		assertTrue(!condition.matches(testEvent));
	}
	
	/**
	 * When one at-least condition is true
	 */
	@Test
	public void testOrConditionPositive() {
		Condition condition1 = new EqualsCondition("host", "test212");
		Condition condition2 = new EqualsCondition("name", "test2");
		Condition condition = new OrCondition(Arrays.asList(condition1, condition2));
		assertTrue(condition.matches(testEvent));
		
		condition1 = new EqualsCondition("host", "test22");
		condition2 = new EqualsCondition("name", "test");
		condition = new OrCondition(Arrays.asList(condition1, condition2));
		assertTrue(condition.matches(testEvent));
	}

	/**
	 * When all conditions are false
	 */
	@Test
	public void testOrConditionNegative() {
		Condition condition1 = new EqualsCondition("host", "test12");
		Condition condition2 = new EqualsCondition("name", "test2");
		Condition condition = new OrCondition(Arrays.asList(condition1, condition2));
		assertTrue(!condition.matches(testEvent));
	}
	
	/**
	 * When condition is true
	 */
	@Test
	public void testNotNegative() {
		Condition condition1 = new EqualsCondition("host", "test212");
		Condition condition = new NotCondition(condition1);
		assertTrue(!condition.matches(testEvent));
	}
	
	/**
	 * When condition is not true
	 */
	@Test
	public void testNotPostive() {
		Condition condition1 = new EqualsCondition("host", "test21");
		Condition condition = new NotCondition(condition1);
		assertTrue(condition.matches(testEvent));
	}
	
}
