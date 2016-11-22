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
package com.srotya.tau.nucleus.wal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.ProducerType;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.CopyTranslator;
import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.Event;

@RunWith(MockitoJUnitRunner.class)
public class TestRocksDBWAL {
	
	@Mock
	AbstractProcessor caller;
	
	@Test
	public void testUsingFactory() throws Exception {
		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		RingBuffer<Event> buffer = RingBuffer.create(ProducerType.SINGLE, factory, 2, new BlockingWaitStrategy());
		when(caller.getDisruptorBuffer()).thenReturn(buffer);
		CopyTranslator copyTranslator = new CopyTranslator();
		when(caller.getCopyTranslator()).thenReturn(copyTranslator);
		try {
			WAL instance = factory.newWalInstance(RocksDBWALService.class, factory, caller, "target/wal"+System.currentTimeMillis(), "target/mem"+System.currentTimeMillis());
			assertNotNull(caller);
			assertNotNull(instance);
			instance.start();
			Event eventW = factory.buildEvent();
			eventW.setEventId("event1");
			eventW.getHeaders().put("event", "event1");
			instance.writeEvent(eventW);
			instance.stop();
			// attempt recovery
			instance.start();
			long next = buffer.getCursor();
			Event event = buffer.get(next);
			assertNotNull(event);
			assertEquals("event1", event.getEventId());
			assertEquals("event1", event.getHeaders().get("event"));
			
			// ack event to remove it from the WAL
			instance.ackEvent("event1");
			assertTrue(instance.getEarliestEventId()==null);
			instance.stop();

			buffer = RingBuffer.create(ProducerType.SINGLE, factory, 2, new BlockingWaitStrategy());
			when(caller.getDisruptorBuffer()).thenReturn(buffer);
			// attempted recovery shouldn't yield anything since the event should have been acknowledged
			instance.start();
			next = buffer.getCursor();
			assertEquals(-1, next);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Shouldn't have failed to initialize:" + e.getMessage());
		}
	}

}
