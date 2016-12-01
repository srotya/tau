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

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
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

	private ExecutorService pool;
	private Disruptor<Event> networkTranmissionDisruptor;
	private DisruptorUnifiedFactory factory;
	private Map<String, BoltExecutor> executorMap;
	private CopyTranslator translator;
	private int workerCount;
	private int workerId;

	public Router(DisruptorUnifiedFactory factory, int workerId, int workerCount, Map<String, BoltExecutor> executorMap) {
		this.factory = factory;
		this.workerId = workerId;
		this.workerCount = workerCount;
		this.executorMap = executorMap;
	}

	public void start() throws Exception {
		pool = Executors.newSingleThreadExecutor();
		networkTranmissionDisruptor = new Disruptor<>(factory, 1024, pool, ProducerType.MULTI,
				new BlockingWaitStrategy());
		translator = new CopyTranslator();
	}
	
	public void stop() throws Exception {
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
			System.err.println("Next processor null, droping event:"+event);
			return;
		}
		int taskId = -1;

		// normalize parallelism
		int parallelism = (nextProcessor.getParallelism() / workerCount); // get
																			// local
																			// parallelism
		if (parallelism < 1) {
			parallelism = 1;
		}
		parallelism = workerCount * parallelism;

		switch (nextProcessor.getTemplateBoltInstance().getRoutingType()) {
		case GROUPBY:
			Object key = event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY);
			if (key != null) {
				taskId = MurmurHash.hash32(key.toString()) % parallelism;
			} else {
				// discard event
			}
			break;
		case SHUFFLE:
			taskId = (int) (event.getEventId() % parallelism);
			break;
		}

		// check if this taskId is local to this worker
		if (taskId / workerCount == workerId) {
			nextProcessor.process(taskId, event);
		} else {
			event.getHeaders().put(Constants.NEXT_PROCESSOR, nextProcessorId);
			event.getHeaders().put(Constants.FIELD_DESTINATION_TASK_ID, taskId);
			event.getHeaders().put(Constants.FIELD_DESTINATION_WORKER_ID, taskId / workerCount);
			networkTranmissionDisruptor.publishEvent(translator, event);
		}
	}

}
