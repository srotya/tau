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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.srotya.tau.linea.clustering.Columbus;
import com.srotya.tau.linea.ft.Acker;
import com.srotya.tau.linea.network.Router;
import com.srotya.tau.linea.processors.BoltExecutor;
import com.srotya.tau.linea.processors.DisruptorUnifiedFactory;

/**
 * Simple test topology to validate how Linea will launch and run pipelines and
 * acking.
 * 
 * Fixed bugs with copy translator causing issues in acking.
 * 
 * @author ambud
 */
public class SimpleTopology {

	public static Router workerInitialize(int i, int seedId, int seedPort, int seedDataPort) throws Exception {
		Map<String, String> conf = new HashMap<>();
		Columbus columbus = new Columbus("localhost", 9920 + i, 5000 + i, 1, 1000 * 60, i);
		ExecutorService bg = Executors.newFixedThreadPool(1);
		bg.submit(columbus);
//		columbus.addKnownPeer(seedId, InetAddress.getByName("localhost"), seedPort, seedDataPort);
		int w = 0;
		while (columbus.getWorkerCount() != 1) {
			StringBuilder builder = new StringBuilder();
			w++;
			for (int k = 0; k < w % 5; k++) {
				builder.append(".");
			}
			System.out.print("\rWaiting for worker discovery:" + builder.toString());
			Thread.sleep(1000);
		}
		Thread.sleep(5000);
		System.out.println("Nodes discovered each other!:" + columbus.getWorkerMap());

		DisruptorUnifiedFactory factory = new DisruptorUnifiedFactory();
		int parallelism = 6;

		Map<String, BoltExecutor> executorMap = new LinkedHashMap<>();
		Router router = new Router(factory, columbus, executorMap);

		PrinterBolt bolt = new PrinterBolt();
		byte[] serializeBolt = BoltExecutor.serializeBoltInstance(bolt);
		BoltExecutor transformerBoltExecutor = new BoltExecutor(conf, factory, serializeBolt, columbus, parallelism/2,
				router);

		Acker ackerBolt = new Acker();
		serializeBolt = BoltExecutor.serializeBoltInstance(ackerBolt);
		BoltExecutor ackerBoltExecutor = new BoltExecutor(conf, factory, serializeBolt, columbus, 2, router);

		TestSpout spout = new TestSpout();
		serializeBolt = BoltExecutor.serializeBoltInstance(spout);
		BoltExecutor spoutExecutor = new BoltExecutor(conf, factory, serializeBolt, columbus, parallelism, router);

		executorMap.put(transformerBoltExecutor.getTemplateBoltInstance().getProcessorName(), transformerBoltExecutor);
		executorMap.put(ackerBoltExecutor.getTemplateBoltInstance().getProcessorName(), ackerBoltExecutor);
		executorMap.put(spoutExecutor.getTemplateBoltInstance().getProcessorName(), spoutExecutor);

		router.start();

		for (Entry<String, BoltExecutor> entry : executorMap.entrySet()) {
			entry.getValue().start();
		}
		return router;
	}

	public static void main(String[] args) throws Exception {
//		Executors.newCachedThreadPool().submit(() -> {
//			try {
//				workerInitialize(0, 1, 9921, 5001);
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});
//		workerInitialize(1, 0, 9920, 5000);
		workerInitialize(0, 0, 9920, 5000);
		Thread.sleep(10000);

		System.exit(1);
	}
}
