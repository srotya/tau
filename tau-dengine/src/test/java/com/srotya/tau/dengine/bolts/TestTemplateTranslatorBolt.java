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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.dengine.bolts.TemplateTranslatorBolt;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

@RunWith(MockitoJUnitRunner.class)
public class TestTemplateTranslatorBolt {

	@Mock
	private Tuple input;

	@Test
	public void testTemplateTranslationPositive() {
		TemplateTranslatorBolt bolt = new TemplateTranslatorBolt();
		
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		
		bolt.prepare(null, null, collector);
		
		TemplateCommand command = new TemplateCommand();
		command.setDelete(false);
		command.setTemplateContent(AlertTemplateSerializer
				.serialize(new AlertTemplate((short) 2, "simple", "test@xyz.com", "http", "hello", "world", 2, 2), false));
		command.setRuleGroup("222553");
		String cmd = new Gson().toJson(command);
		when(input.getString(0)).thenReturn(cmd);
		bolt.execute(input);
		
		assertNotNull(processedEventContainer.get());
		assertTrue(processedEventContainer.get().size()>0);
		
		verify(collector, times(1)).ack(input);
	}
	
	@Test
	public void testTemplateTranslationNegative() {
		TemplateTranslatorBolt bolt = new TemplateTranslatorBolt();
		
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				return new ArrayList<>();
			}
		});
		
		bolt.prepare(null, null, collector);
		
		String cmd = new Gson().toJson(null);
		when(input.getString(0)).thenReturn(cmd);
		bolt.execute(input);
		verify(collector, times(1)).emit(eq(Constants.ERROR_STREAM), eq(input), any());
		verify(collector, times(1)).ack(input);
	}

}
