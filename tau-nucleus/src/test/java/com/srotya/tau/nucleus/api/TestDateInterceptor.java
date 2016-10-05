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
package com.srotya.tau.nucleus.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.srotya.tau.interceptors.ValidationException;

/**
 * Test cases for date interceptor/validator
 * 
 * @author ambud_sharma
 */
public class TestDateInterceptor {

	private static final String TIMESTAMP = "@timestamp";
	private DateInterceptor interceptor;
	private Map<String, String> config;

	public TestDateInterceptor() {
	}

	@Before
	public void before() {
		this.config = new HashMap<>();
		this.config.put(DateInterceptor.DATEFIELD, TIMESTAMP);
		this.interceptor = new DateInterceptor();
		this.interceptor.configure(config);
	}

	@Test
	public void testISO8601() throws ValidationException {
		JsonObject obj = new JsonObject();
		obj.addProperty(TIMESTAMP, "2011-04-19T03:44:01.103Z");
		interceptor.validate(obj);
		obj.remove(TIMESTAMP);
		try {
			interceptor.validate(obj);
			fail("Must have thrown validation exception");
		} catch (Exception e) {
		}
		obj.addProperty(TIMESTAMP, "2016-04-21T20:54:41.103Z");
		interceptor.validate(obj);
		long ts = obj.get(TIMESTAMP).getAsLong();
		assertEquals(1461272081103L, ts);
		try {
			interceptor.validate(new JsonObject());
			fail("Must have thrown validation exception");
		} catch (Exception e) {
		}
		this.config.put(DateInterceptor.DATEFIELD, null);
		try{
			interceptor.validate(new JsonObject());
		} catch (Exception e) {
		}
	}

}
