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
package com.srotya.tau.api.dao.alertreceiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.dao.alertreceiver.EventViewer;

/**
 * Tests for alert viewer functionality
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAlertReceiver {

	private static Ignite ignite;
	@Mock
	private ApplicationManager am;

	@BeforeClass
	public static void beforeClass() {
		Ignition.setClientMode(false);
		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setIncludeEventTypes(org.apache.ignite.events.EventType.EVTS_CACHE);
		ignite = Ignition.start(cfg);
	}

	@AfterClass
	public static void afterClass() {
		if (ignite != null) {
			ignite.close();
		}
	}

	@Before
	public void before() {
		when(am.getIgnite()).thenReturn(ignite);
	}

	@Test
	public void test1Initialize() throws Exception {
		EventViewer receiver = new EventViewer(am);
		System.setProperty("channel.capacity", "1000");
		receiver.initializeKafkaConsumer();
		assertEquals(ignite, receiver.getIgnite());
		assertEquals(1000, receiver.getChannelSize());
	}

	@Test
	public void test2OpenCloseChannel() throws Exception {
		EventViewer receiver = new EventViewer(am);
		receiver.initializeKafkaConsumer();
		receiver.openChannel((short) 2);
		assertTrue(receiver.getIgnite().cache("channels").containsKey("2"));
		assertEquals(1, receiver.getIgnite().cache("channels").get("2"));
		receiver.openChannel((short) 2);
		assertEquals(2, receiver.getIgnite().cache("channels").get("2"));
		receiver.closeChannel((short) 2);
		assertEquals(1, receiver.getIgnite().cache("channels").get("2"));
		receiver.closeChannel((short) 2);
		assertTrue(!receiver.getIgnite().cache("channels").containsKey("2"));
	}

	@Test
	public void test3PublishEvents() throws Exception {
		EventViewer receiver = new EventViewer(am);
		receiver.initializeKafkaConsumer();
		receiver.openChannel((short) 3);
		Map<String, Object> headers = new HashMap<>();
		headers.put("time", "232");
		assertTrue(receiver.publishEvent((short) 3, headers));
		assertTrue(!receiver.publishEvent((short) 4, headers));
		Queue<Map<String, Object>> queue = receiver.getChannel((short) 3);
		assertNotNull(queue);
		assertEquals(1, queue.size());
		assertTrue(queue.peek().containsKey("time"));
		assertEquals("232", queue.peek().get("time"));
	}

}