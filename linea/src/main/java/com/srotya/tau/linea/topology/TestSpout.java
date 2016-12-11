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

import java.util.Map;

import com.srotya.tau.linea.ft.Collector;
import com.srotya.tau.linea.processors.Spout;
import com.srotya.tau.wraith.Event;

/**
 * @author ambud.sharma
 */
public class TestSpout extends Spout {

	private static final long serialVersionUID = 1L;
	private transient Collector collector;

	@Override
	public void configure(Map<String, String> conf, int taskId, Collector collector) {
		this.collector = collector;
	}

	@Override
	public String getProcessorName() {
		return "testSpout";
	}
	
	@Override
	public void ready() {
		for(int i=0;i<5;i++) {
			Event event = collector.getFactory().buildEvent();
			collector.spoutEmit("testSpout", "jsonbolt", event);
		}
	}

	@Override
	public void ack(Long eventId) {
		// TODO Auto-generated method stub
		System.out.println("Spout acking event:"+eventId);

	}

	@Override
	public void fail(Long eventId) {
		// TODO Auto-generated method stub
		System.out.println("Spout failing event");
	}

}
