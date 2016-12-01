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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.linea.ft.Acker;
import com.srotya.tau.linea.ft.Collector;
import com.srotya.tau.linea.network.Router;
import com.srotya.tau.linea.processors.BoltExecutor;
import com.srotya.tau.linea.processors.DisruptorUnifiedFactory;
import com.srotya.tau.wraith.Event;

/**
 * Simple test topology to validate how Linea will launch and run pipelines and acking.
 * 
 * Fixed bugs with copy translator causing issues in acking.
 * 
 * @author ambud
 */
public class SimpleTopology {

	public static void main(String[] args) throws Exception {

		Map<String, String> conf = new HashMap<>();

		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		int workerId = 0;
		int workerCount = 1;
		int parallelism = 1;
		
		Map<String, BoltExecutor> executorMap = new HashMap<>();
		Router router = new Router(factory, workerId, workerCount, executorMap);

		PrinterBolt bolt = new PrinterBolt();
		byte[] serializeBolt = BoltExecutor.serializeBoltInstance(bolt);
		BoltExecutor transformerBoltExecutor = new BoltExecutor(conf, factory, serializeBolt, workerId, workerCount,
				parallelism, router);

		Acker ackerBolt = new Acker();
		serializeBolt = BoltExecutor.serializeBoltInstance(ackerBolt);
		BoltExecutor ackerBoltExecutor = new BoltExecutor(conf, factory, serializeBolt, workerId, workerCount,
				parallelism, router);

		executorMap.put(transformerBoltExecutor.getTemplateBoltInstance().getProcessorName(), transformerBoltExecutor);
		executorMap.put(ackerBoltExecutor.getTemplateBoltInstance().getProcessorName(), ackerBoltExecutor);

		router.start();
		
		for (Entry<String, BoltExecutor> entry : executorMap.entrySet()) {
			entry.getValue().start();
		}
		
		Collector collector = new Collector(factory, router);
		for (int i = 0; i < 10; i++) {
			Event event = factory.buildEvent();
			event.getHeaders().put("host", "test"+i);
//			collector.ack(event);
			collector.emit(bolt.getProcessorName(), event, event);
		}
		Thread.sleep(1000);
		
		for (Entry<String, BoltExecutor> entry : executorMap.entrySet()) {
			entry.getValue().stop();
		}
	}
}
