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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.srotya.tau.alerts.MailBolt;
import com.srotya.tau.alerts.media.MailService;
import com.srotya.tau.dengine.MockTupleHelpers;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.Alert;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestMailBolt {

	@Mock
	private MailService service;
	@Mock
	private Tuple tuple;

	@Test
	public void testMailBolt() {
		MailBolt bolt = new MailBolt();
		final AtomicReference<Values> processedEventContainer = new AtomicReference<Values>(null);
		OutputCollector collector = MockTupleHelpers.mockCollector(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Object newEvent = invocation.getArguments()[2];
				processedEventContainer.set((Values) newEvent);
				System.out.println("Alert emitted:" + processedEventContainer.get());
				return new ArrayList<>();
			}
		});
		bolt.prepare(null, null, collector);
		bolt.setMailService(service);
		Alert alert = new Alert();
		when(service.sendMail(alert)).thenReturn(true);
		when(tuple.getValueByField(Constants.FIELD_ALERT)).thenReturn(alert);
		bolt.execute(tuple);
		verify(collector, times(1)).ack(tuple);
		verify(service, times(1)).sendMail(alert);
	}

}
