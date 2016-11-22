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
package com.srotya.tau.nucleus;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.nucleus.wal.RocksDBWALService;
import com.srotya.tau.nucleus.wal.WAL;
import com.srotya.tau.wraith.Event;

/**
 * @author ambudsharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestDisruptorUnifiedFactory {

	@Mock
	AbstractProcessor caller;
	
	@Test
	public void testNewEventInstance() {
		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		Event event = factory.buildEvent();
		assertNotNull(event);
		assertNotNull(event.getHeaders());
	}

	@Test
	public void testNewWalInstance() throws Exception {
		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		try {
			WAL instance = factory.newWalInstance(RocksDBWALService.class, factory, caller, "target/wal"+System.currentTimeMillis(), "target/mem"+System.currentTimeMillis());
			assertNotNull(caller);
			assertNotNull(instance);
			instance.start();
			Event event = factory.buildEvent();
			event.setEventId("event1");
			event.setBody("event1".getBytes());
			instance.writeEvent(event);
			instance.stop();
			instance.start();
		} catch (Exception e) {
			e.printStackTrace();
			fail("Shouldn't have failed to initialize:" + e.getMessage());
		}
	}

}