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
package com.srotya.tau.nucleus.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.Utils;
import com.srotya.tau.nucleus.disruptor.ShuffleHandler;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;

/**
 * @author ambudsharma
 */
public class TestAbstractProcessor {

	private AtomicInteger counter = new AtomicInteger(0);

	@BeforeClass
	public static void beforeClass() {
		File[] listFiles = new File("target/").listFiles(new FileFilter() {

			@Override
			public boolean accept(File path) {
				if ((path.getName().startsWith("m") || path.getName().startsWith("w"))
						&& path.isDirectory()) {
					return true;
				} else {
					return false;
				}
			}
		});
		for(File file:listFiles) {
			Utils.wipeDirectory(file.getAbsolutePath());
		}
	}

	@Test
	public void testStartStop() throws Exception {
		AbstractProcessor processor = new TempAbstractProcessor1(new DisruptorUnifiedFactory(), 1, 128, new HashMap<>(),
				null);
		try {
			processor.start();
			fail("Can't create a processor will null handlers");
		} catch (Exception e) {
		}

		processor = new TempAbstractProcessor2(new DisruptorUnifiedFactory(), 1, 128, new HashMap<>(), null, this);
		assertTrue(!new File("target/mtemp").exists());
		processor.start();
		assertTrue(processor.isStarted());
		assertNotNull(processor.getDisruptorBuffer());
		assertNotNull(processor.getLogger());
		assertNotNull(processor.getProcessorWal());
		assertEquals("temp", processor.getConfigPrefix());
		processor.stop();
	}

	@Test
	public void testEventProcessing() throws Exception {
		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		AbstractProcessor processor = new TempAbstractProcessor2(factory, 1, 2, new HashMap<>(), null, this);
		processor.start();
		
		Event event = factory.buildEvent();
		event.setEventId("one");
		event.setBody("hello".getBytes());
		processor.processEventWaled(event);
		processor.processEventWaled(event);
		processor.processEventWaled(event);
		processor.stop();		
		assertEquals(3, counter.getAndSet(0));
		assertTrue(!processor.isStarted());
	}
	
	private static class TempAbstractProcessor1 extends AbstractProcessor {

		private static final Logger logger = Logger.getLogger(TempAbstractProcessor1.class.getName());

		public TempAbstractProcessor1(DisruptorUnifiedFactory factory, int parallelism, int bufferSize,
				Map<String, String> conf, AbstractProcessor[] outputProcessors) {
			super(factory, parallelism, bufferSize, conf, outputProcessors);
		}

		@Override
		public String getConfigPrefix() {
			return "temp";
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

		@Override
		public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
				Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
			return null;
		}

	}

	private static class TempAbstractProcessor2 extends AbstractProcessor {

		private static final Logger logger = Logger.getLogger(TempAbstractProcessor2.class.getName());
		private TestAbstractProcessor caller;

		public TempAbstractProcessor2(DisruptorUnifiedFactory factory, int parallelism, int bufferSize,
				Map<String, String> conf, AbstractProcessor[] outputProcessors, TestAbstractProcessor caller) {
			super(factory, parallelism, bufferSize, conf, outputProcessors);
			this.caller = caller;
		}

		@Override
		public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
				Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
			return new Temp2Handler(taskId, parallelism, caller);
		}

		@Override
		public String getConfigPrefix() {
			return "temp";
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

		private static class Temp2Handler extends ShuffleHandler {

			private TestAbstractProcessor testCallback;

			public Temp2Handler(int taskId, MutableInt taskCount, TestAbstractProcessor testCallback) {
				super(taskId, taskCount);
				this.testCallback = testCallback;
			}

			@Override
			public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
				testCallback.counter.incrementAndGet();
			}

		}

	}
}
