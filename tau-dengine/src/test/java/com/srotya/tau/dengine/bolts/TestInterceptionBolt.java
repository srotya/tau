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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.bolts.InterceptionBolt;
import com.srotya.tau.dengine.bolts.JSONTranslatorBolt;
import com.srotya.tau.wraith.Event;

import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestInterceptionBolt {

	@Mock
	private Tuple input;

	@Test
	public void testDateValidation() {
		InterceptionBolt bolt = new InterceptionBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		bolt.prepare(new HashMap<>(), null, MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				System.out.println("Event emitted:" + processedEventContainer.get());
				return new ArrayList<>();
			}
		}));
		when(input.getString(0)).thenReturn("{\"@timestamp\":\"2016-04-21T20:54:41.103Z\"}");
		bolt.execute(input);
		String result = processedEventContainer.get().get(0).toString();
		assertNotNull(result);
		assertEquals(1461272081103L, new Gson().fromJson(result, JsonObject.class).get("@timestamp").getAsLong());
	}
	
	@Test
	public void testDateValidationDataType() {
		BaseRichBolt bolt = new InterceptionBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		bolt.prepare(new HashMap<>(), null, MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				System.out.println("Event emitted:" + processedEventContainer.get());
				return new ArrayList<>();
			}
		}));
		when(input.getString(0)).thenReturn("{\"@timestamp\":\"2016-04-21T20:54:41.103Z\",\"tenant_id\":\"asdas\"}");
		bolt.execute(input);
		String result = processedEventContainer.get().get(0).toString();
		
		bolt = new JSONTranslatorBolt();
		bolt.prepare(new HashMap<>(), null, MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[1];
				processedEventContainer.set((Values) newEvent);
				System.out.println("Event emitted:" + processedEventContainer.get());
				return new ArrayList<>();
			}
		}));
		when(input.getString(0)).thenReturn(result);
		bolt.execute(input);
		Event event = (Event) processedEventContainer.get().get(0);
		System.out.println(((Double)event.getHeaders().get("@timestamp")).longValue());
		assertTrue(event.getHeaders().get("@timestamp") instanceof Double);
	}

}
