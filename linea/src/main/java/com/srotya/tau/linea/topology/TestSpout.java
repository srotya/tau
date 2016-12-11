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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.srotya.tau.linea.ft.Collector;
import com.srotya.tau.linea.processors.Spout;
import com.srotya.tau.wraith.Event;

/**
 * @author ambud.sharma
 */
public class TestSpout extends Spout {

	private static final long serialVersionUID = 1L;
	private transient Collector collector;
	private transient Set<Long> emittedEvents;
	private transient int taskId;

	@Override
	public void configure(Map<String, String> conf, int taskId, Collector collector) {
		this.taskId = taskId;
		this.collector = collector;
		emittedEvents = new HashSet<>();
	}

	@Override
	public String getProcessorName() {
		return "testSpout";
	}
	
	@Override
	public void ready() {
		for(int i=0;i<5;i++) {
			Event event = collector.getFactory().buildEvent();
			event.getHeaders().put("uuid", "host"+i);
			emittedEvents.add(event.getEventId());
			collector.spoutEmit("testSpout", "jsonbolt", event);
		}
	}

	@Override
	public void ack(Long eventId) {
		emittedEvents.remove(eventId);
		System.out.println("Spout acking event:"+eventId+"\tremaining:"+emittedEvents.size()+"\tspoutid:"+taskId);

	}

	@Override
	public void fail(Long eventId) {
		System.out.println("Spout failing event:"+eventId);
	}

}
