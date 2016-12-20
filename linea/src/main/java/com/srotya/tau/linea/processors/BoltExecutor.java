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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import com.srotya.tau.linea.clustering.Columbus;
import com.srotya.tau.linea.ft.Collector;
import com.srotya.tau.linea.network.Router;
import com.srotya.tau.nucleus.disruptor.CopyTranslator;
import com.srotya.tau.wraith.Event;

/**
 * Bolt Executor is wrapper that instantiates and executes bolt code.
 * 
 * @author ambud
 */
public class BoltExecutor {

	private ExecutorService es;
	private Bolt templateBoltInstance;
	private Map<Integer, BoltExecutorWrapper> taskProcessorMap;
	private CopyTranslator copyTranslator;
	private int parallelism;
	private Columbus columbus;
	private DisruptorUnifiedFactory factory;
	private byte[] serializedBoltInstance;
	private Map<String, String> conf;
	private Router router;

	/**
	 * @param conf
	 * @param factory
	 * @param serializedBoltInstance
	 * @param columbus
	 * @param parallelism
	 * @param router
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public BoltExecutor(Map<String, String> conf, DisruptorUnifiedFactory factory, byte[] serializedBoltInstance,
			Columbus columbus, int parallelism, Router router) throws IOException, ClassNotFoundException {
		this.conf = conf;
		this.factory = factory;
		this.serializedBoltInstance = serializedBoltInstance;
		this.columbus = columbus;
		this.parallelism = parallelism;
		this.router = router;
		this.taskProcessorMap = new HashMap<>();

		this.templateBoltInstance = deserializeBoltInstance(serializedBoltInstance);
		this.es = Executors.newFixedThreadPool((parallelism * 2) + 1);
		this.copyTranslator = new CopyTranslator();
	}

	/**
	 * Start method for this bolt executor
	 */
	public void start() {
		/**
		 * First worker 0*4+0 = 0 0*4+1 = 1 0*4+2 = 2 = 3 Second worker 1*4+0 =
		 * 4 1*4+1 = 5 1*4+2 = 6
		 *
		 * Third worker 2*3+0 = 6
		 * 
		 * Or First worker 0*2+0 = 0 Second worker 1*2+0 = 2
		 */
		es.submit(() -> {
			try {
				for (int i = 0; i < parallelism; i++) {
					int taskId = columbus.getSelfWorkerId() * parallelism + i;
					Bolt object = deserializeBoltInstance(serializedBoltInstance);
					object.configure(conf, taskId, new Collector(factory, router, object.getBoltName(), taskId));
					taskProcessorMap.put(taskId, new BoltExecutorWrapper(factory, es, object));
				}
				for (Entry<Integer, BoltExecutorWrapper> entry : taskProcessorMap.entrySet()) {
					entry.getValue().start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Stop method for this bolt executor
	 * @throws InterruptedException
	 */
	public void stop() throws InterruptedException {
		for (Entry<Integer, BoltExecutorWrapper> entry : taskProcessorMap.entrySet()) {
			entry.getValue().stop();
		}
		es.shutdownNow();
		es.awaitTermination(100, TimeUnit.SECONDS);
	}

	/**
	 * Deserialize bolt instance from the byte array
	 * 
	 * @param processorObject
	 * @return bolt instance
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Bolt deserializeBoltInstance(byte[] processorObject) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(processorObject));
		Bolt processor = (Bolt) ois.readObject();
		ois.close();
		return processor;
	}

	/**
	 * Serialize {@link Bolt} instance to byte array
	 * 
	 * @param boltInstance
	 * @return byte array
	 * @throws IOException
	 */
	public static byte[] serializeBoltInstance(Bolt boltInstance) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		ObjectOutputStream ois = new ObjectOutputStream(stream);
		ois.writeObject(boltInstance);
		ois.close();
		return stream.toByteArray();
	}

	/**
	 * Method called by Router
	 * @param taskId
	 * @param event
	 */
	public void process(int taskId, Event event) {
		BoltExecutorWrapper wrapper = taskProcessorMap.get(taskId);
		if (wrapper != null) {
			wrapper.getBuffer().publishEvent(copyTranslator, event);
		} else {
			System.out.println("Executor not found for:" + taskId + "\t" + columbus.getSelfWorkerId() + "\t"
					+ taskProcessorMap + "\t" + event);
		}
	}

	/**
	 * @return templatedBoltInstance
	 */
	public Bolt getTemplateBoltInstance() {
		return templateBoltInstance;
	}

	/**
	 * @return parallelism
	 */
	public int getParallelism() {
		return parallelism;
	}

	/**
	 * Bolt Executor Wrapper
	 * 
	 * @author ambud
	 */
	public static class BoltExecutorWrapper implements EventHandler<Event> {

		private Bolt bolt;
		private Disruptor<Event> disruptor;
		private RingBuffer<Event> buffer;
		private ExecutorService pool;

		@SuppressWarnings("unchecked")
		public BoltExecutorWrapper(DisruptorUnifiedFactory factory, ExecutorService pool, Bolt processor) {
			this.pool = pool;
			this.bolt = processor;
			disruptor = new Disruptor<>(factory, 1024, pool, ProducerType.MULTI, new BlockingWaitStrategy());
			disruptor.handleEventsWith(this);
		}

		/**
		 * Start {@link BoltExecutorWrapper}
		 */
		public void start() {
			buffer = disruptor.start();
			pool.submit(() -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				bolt.ready();
			});
		}

		/**
		 * Stop {@link BoltExecutorWrapper}
		 */
		public void stop() {
			disruptor.shutdown();
		}

		@Override
		public void onEvent(Event event, long arg1, boolean arg2) throws Exception {
			bolt.process(event);
		}

		/**
		 * @return buffer
		 */
		public RingBuffer<Event> getBuffer() {
			return buffer;
		}

		/**
		 * @return processor
		 */
		public Bolt getBolt() {
			return bolt;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "ProcessorWrapper [processor=" + bolt.getBoltName() + "]";
		}
	}

}
