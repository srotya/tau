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

import com.srotya.tau.nucleus.disruptor.ROUTING_TYPE;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;

/**
 * Spout is a type of {@link Bolt} that generates data that is processed
 * by the rest of the topology.
 * 
 * @author ambud
 */
public abstract class Spout implements Bolt {

	private static final long serialVersionUID = 1L;

	@Override
	public void process(Event event) {
		Object object = event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY);
		if(object!=null) {
			Boolean type = (Boolean) event.getHeaders().get(Constants.FIELD_AGGREGATION_TYPE);
			if(type) {
				ack((Long)object);
			}else {
				fail((Long)object);
			}
		}
	}
	
	public abstract void ready();
	
	/**
	 * Marking eventId as processed
	 * @param eventId
	 */
	public abstract void ack(Long eventId);
	
	/**
	 * Marking eventId as failed
	 * @param eventId
	 */
	public abstract void fail(Long eventId);
	
	@Override
	public ROUTING_TYPE getRoutingType() {
		return ROUTING_TYPE.GROUPBY;
	}

}
