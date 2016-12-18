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
package com.srotya.tau.linea.network;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.srotya.tau.linea.clustering.Columbus;
import com.srotya.tau.linea.processors.BoltExecutor;
import com.srotya.tau.linea.processors.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.CopyTranslator;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MurmurHash;

/**
 * {@link Event} router
 * 
 * @author ambud
 */
public class Router {

	private static final Logger logger = Logger.getLogger(Router.class.getName());
	private Disruptor<Event> networkTranmissionDisruptor;
	private DisruptorUnifiedFactory factory;
	private Map<String, BoltExecutor> executorMap;
	private CopyTranslator translator;
	private Columbus columbus;
	private InternalUDPTransportServer server;
	private int workerCount;
	private ExecutorService pool;

	public Router(DisruptorUnifiedFactory factory, Columbus columbus, int workerCount,
			Map<String, BoltExecutor> executorMap) {
		this.factory = factory;
		this.columbus = columbus;
		this.workerCount = workerCount;
		this.executorMap = executorMap;
	}

	@SuppressWarnings("unchecked")
	public void start() throws Exception {
		while (columbus.getWorkerCount() < workerCount) {
			Thread.sleep(2000);
			logger.info("Waiting for worker discovery");
		}
		pool = Executors.newFixedThreadPool(2);
		server = new InternalUDPTransportServer(this,
				columbus.getWorkerMap().get(columbus.getSelfWorkerId()).getDataPort());
		pool.submit(() -> {
			try {
				server.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		networkTranmissionDisruptor = new Disruptor<>(factory, 1024, pool, ProducerType.MULTI,
				new BlockingWaitStrategy());
		InternalUDPTransportClient client = new InternalUDPTransportClient(columbus,
				columbus.getWorkerMap().get(columbus.getSelfWorkerId()).getDataPort() + 1000, false);
		client.init();
		networkTranmissionDisruptor.handleEventsWith(client);
		networkTranmissionDisruptor.start();
		translator = new CopyTranslator();
	}

	public void stop() throws Exception {
		server.stop();
		networkTranmissionDisruptor.shutdown();
		pool.shutdownNow();
	}

	public void directLocalRouteEvent(String nextProcessorId, int taskId, Event event) {
		executorMap.get(nextProcessorId).process(taskId, event);
	}

	public void routeEvent(String nextProcessorId, Event event) {
		BoltExecutor nextProcessor = executorMap.get(nextProcessorId);
		if (nextProcessor == null) {
			// drop this event
			System.err.println("Next processor null, droping event:" + event);
			return;
		}
		int taskId = -1;
		int workerCount = columbus.getWorkerCount();

		// normalize parallelism
		int totalParallelism = nextProcessor.getParallelism(); // get
																// local
																// parallelism
		totalParallelism = workerCount * totalParallelism;
		switch (nextProcessor.getTemplateBoltInstance().getRoutingType()) {
		case GROUPBY:
			Object key = event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY);
			if (key != null) {
				taskId = (Math.abs(MurmurHash.hash32(key.toString())) % totalParallelism);
			} else {
				// discard event
			}
			break;
		case SHUFFLE:
			// taskId = columbus.getSelfWorkerId() * totalParallelism
			// + (Math.abs((int) (event.getEventId() % totalParallelism)));
			taskId = Math.abs((int) (event.getEventId() % totalParallelism));
			break;
		}

		// check if this taskId is local to this worker
		routeToTaskId(nextProcessorId, event, nextProcessor, taskId);
	}

	public void routeToTaskId(String nextProcessorId, Event event, BoltExecutor nextProcessor, int taskId) {
		if (nextProcessor == null) {
			nextProcessor = executorMap.get(nextProcessorId);
		}
		int destinationWorker = 0;
		if (taskId >= nextProcessor.getParallelism()) {
			destinationWorker = taskId / nextProcessor.getParallelism();
		}

		if (destinationWorker == columbus.getSelfWorkerId()) {
			nextProcessor.process(taskId, event);
		} else {
			logger.info("Network routing");
			event.getHeaders().put(Constants.NEXT_PROCESSOR, nextProcessorId);
			event.getHeaders().put(Constants.FIELD_DESTINATION_TASK_ID, taskId);
			event.getHeaders().put(Constants.FIELD_DESTINATION_WORKER_ID, destinationWorker);
			networkTranmissionDisruptor.publishEvent(translator, event);
		}
	}

	public Columbus getColumbus() {
		return columbus;
	}

}
