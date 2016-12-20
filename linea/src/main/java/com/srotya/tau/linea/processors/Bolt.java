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

import java.io.Serializable;
import java.util.Map;

import com.srotya.tau.linea.ft.Collector;
import com.srotya.tau.nucleus.disruptor.ROUTING_TYPE;
import com.srotya.tau.wraith.Event;

/**
 * Bolt is custom user defined code for processing {@link Event}s. <br>
 * <br>
 * Bolt interface should be implemented by user-code.
 * 
 * @author ambud
 */
public interface Bolt extends Serializable {

	/**
	 * Configure method for initializing the bolt
	 * 
	 * @param conf
	 * @param taskId
	 * @param collector
	 */
	public void configure(Map<String, String> conf, int taskId, Collector collector);

	/**
	 * Method asynchronously called just before events are started
	 */
	public void ready();

	/**
	 * Method called on each Event
	 * 
	 * @param event
	 */
	public void process(Event event);

	/**
	 * Type of routing to this bolt i.e. Events processed by this bolt
	 * 
	 * @return
	 */
	public ROUTING_TYPE getRoutingType();

	/**
	 * Name of the bolt
	 * 
	 * @return name of bolt
	 */
	public String getBoltName();

}
