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
package com.srotya.tau.linea.topology;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.linea.clustering.Columbus;
import com.srotya.tau.linea.ft.AckerBolt;
import com.srotya.tau.linea.network.Router;
import com.srotya.tau.linea.processors.Bolt;
import com.srotya.tau.linea.processors.BoltExecutor;
import com.srotya.tau.linea.processors.DisruptorUnifiedFactory;
import com.srotya.tau.linea.processors.Spout;

/**
 * A simple topology builder
 * 
 * @author ambud
 */
public class TopologyBuilder {

	public static final String WORKER_COUNT = "worker.count";
	public static final String WORKER_ID = "worker.id";
	public static final String WORKER_DATA_PORT = "worker.data.port";
	public static final String WORKER_DISCOVERY_PORT = "worker.discovery.port";
	private Map<String, String> conf;
	private DisruptorUnifiedFactory factory;
	private Map<String, BoltExecutor> executorMap;
	private Router router;
	private Columbus columbus;
	private int workerCount;

	public TopologyBuilder(Map<String, String> conf) throws Exception {
		this.conf = conf;
		factory = new DisruptorUnifiedFactory();
		executorMap = new HashMap<>();
		columbus = new Columbus(Integer.parseInt(conf.getOrDefault(WORKER_DISCOVERY_PORT, "9920")),
				Integer.parseInt(conf.getOrDefault(WORKER_DATA_PORT, "5000")), 1, 1000 * 60,
				Integer.parseInt(conf.getOrDefault(WORKER_ID, "0")));
		workerCount = Integer.parseInt(conf.getOrDefault(WORKER_COUNT, "1"));
		router = new Router(factory, columbus, workerCount, executorMap);
	}
	
	public TopologyBuilder addSpout(Spout spout, int parallelism) throws IOException, ClassNotFoundException {
		return addBolt(spout, parallelism);
	}
 
	public TopologyBuilder addBolt(Bolt bolt, int parallelism) throws IOException, ClassNotFoundException {
		byte[] serializeBoltInstance = BoltExecutor.serializeBoltInstance(bolt);
		BoltExecutor boltExecutor = new BoltExecutor(conf, factory, serializeBoltInstance, columbus, parallelism,
				router);
		executorMap.put(boltExecutor.getTemplateBoltInstance().getBoltName(), boltExecutor);
		return this;
	}

	public TopologyBuilder start() throws Exception {
		addBolt(new AckerBolt(), 1);
		router.start();
		for (Entry<String, BoltExecutor> entry : executorMap.entrySet()) {
			entry.getValue().start();
		}
		return this;
	}

	public TopologyBuilder stop() throws Exception {
		for (Entry<String, BoltExecutor> entry : executorMap.entrySet()) {
			entry.getValue().stop();
		}
		router.stop();
		return this;
	}

	/**
	 * @return
	 */
	public DisruptorUnifiedFactory getFactory() {
		return factory;
	}

	/**
	 * @return
	 */
	public Router getRouter() {
		return router;
	}

}
