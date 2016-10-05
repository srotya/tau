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

import com.srotya.tau.alerts.AlertRouterBolt;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.wraith.actions.alerts.Alert;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * 
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAlertRouterBolt {

	@Mock
	private Tuple input;
	private Map<String, String> conf;

	@Before
	public void before() throws IOException {
		conf = new HashMap<>();
	}
	
	@Test
	public void testRouting() {
		AlertRouterBolt bolt = new AlertRouterBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		bolt.prepare(conf, null, collector);
		Alert alert = new Alert();
		alert.setId((short) 0);
		alert.setBody("test");
		alert.setMedia("mail");
		alert.setTimestamp(3253454235L);
		when(input.contains(Constants.FIELD_ALERT)).thenReturn(true);
		when(input.getValueByField(Constants.FIELD_ALERT)).thenReturn(alert);
		bolt.execute(input);
		verify(collector, times(1)).emit(eq("mail"), eq(input), any());
		verify(collector, times(1)).ack(input);
		
		// make a call with unknown stream
		alert.setId((short) 0);
		alert.setBody("test");
		alert.setMedia(null);
		alert.setTimestamp(3253454235L);
		bolt.execute(input);
		verify(collector, times(1)).emit(eq(null), eq(input), any());
		verify(collector, times(2)).ack(input);
		
		alert.setId((short) 0);
		alert.setBody("test");
		alert.setMedia("http");
		alert.setTimestamp(3253454235L);
		bolt.execute(input);
		verify(collector, times(1)).emit(eq("http"), eq(input), any());
		verify(collector, times(3)).ack(input);
	}
}
