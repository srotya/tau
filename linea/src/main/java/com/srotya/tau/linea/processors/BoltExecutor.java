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
package com.srotya.tau.linea.processors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.srotya.tau.nucleus.disruptor.CopyTranslator;
import com.srotya.tau.wraith.Event;

/**
 * @author ambud
 *
 */
public class BoltExecutor {

	private ExecutorService es;
	private Bolt templateBoltInstance;
	private Map<Integer, ProcessorWrapper> taskProcessorMap;
	private CopyTranslator copyTranslator;
	private int parallelism;

	public BoltExecutor(Map<String, String> conf, DisruptorUnifiedFactory factory, byte[] processorObject,
			int workerId, int workerCount, int parallelism) throws IOException, ClassNotFoundException {
		this.parallelism = parallelism;
		taskProcessorMap = new HashMap<>();

		templateBoltInstance = deserializeProcessorInstance(processorObject);
		int localTasks = (parallelism / workerCount);
		if (localTasks < 1) {
			localTasks = 1;
		}

		es = Executors.newFixedThreadPool(localTasks);

		/**
		 *First worker
		 * 0*4+0 = 0
		 * 0*4+1 = 1
		 * 0*4+2 = 2
		 *      = 3
		 *Second worker
		 * 1*4+0 = 4
		 * 1*4+1 = 5
		 * 1*4+2 = 6
		 *
		 *Third worker
		 * 2*3+0 = 6
		 */
		for (int i = 0; i < localTasks; i++) {
			int taskId = workerId * workerCount + i;
			Bolt object = deserializeProcessorInstance(processorObject);
			taskProcessorMap.put(taskId, new ProcessorWrapper(factory, es, object));
		}
		copyTranslator = new CopyTranslator();
	}
	
	public void start() {
		for (Entry<Integer, ProcessorWrapper> entry : taskProcessorMap.entrySet()) {
			entry.getValue().start();
		}
	}
	
	public void stop() throws InterruptedException {
		for (Entry<Integer, ProcessorWrapper> entry : taskProcessorMap.entrySet()) {
			entry.getValue().stop();
		}
		es.shutdownNow();
		es.awaitTermination(100, TimeUnit.SECONDS);
	}

	public static Bolt deserializeProcessorInstance(byte[] processorObject)
			throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(processorObject));
		Bolt processor = (Bolt) ois.readObject();
		ois.close();
		return processor;
	}

	public void process(int taskId, Event event) {
		ProcessorWrapper wrapper = taskProcessorMap.get(taskId);
		if (wrapper != null) {
			wrapper.getBuffer().publishEvent(copyTranslator, event);
		}
	}
	
	public Bolt getTemplateBoltInstance() {
		return templateBoltInstance;
	}
	
	public int getParallelism() {
		return parallelism;
	}

	public static class ProcessorWrapper implements EventHandler<Event> {

		private Bolt processor;
		private Disruptor<Event> disruptor;
		private RingBuffer<Event> buffer;

		@SuppressWarnings("unchecked")
		public ProcessorWrapper(DisruptorUnifiedFactory factory, ExecutorService pool, Bolt processor) {
			this.processor = processor;
			disruptor = new Disruptor<>(factory, 1024, pool, ProducerType.MULTI, new BlockingWaitStrategy());
			disruptor.handleEventsWith(this);
		}

		public void start() {
			buffer = disruptor.start();
		}

		public void stop() {
			disruptor.shutdown();
		}

		@Override
		public void onEvent(Event event, long arg1, boolean arg2) throws Exception {
			processor.process(event);
		}

		public RingBuffer<Event> getBuffer() {
			return buffer;
		}

		public Bolt getProcessor() {
			return processor;
		}
	}

}
