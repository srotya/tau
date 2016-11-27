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
package com.srotya.tau.nucleus;

import java.io.IOException;

import com.lmax.disruptor.RingBuffer;
import com.srotya.tau.nucleus.wal.WAL;
import com.srotya.tau.wraith.Event;

import io.dropwizard.lifecycle.Managed;

/**
 * @author ambudsharma
 */
public interface ManagedProcessor extends Managed {
	
	public boolean hasStarted();
	
	/**
	 * @return ringBuffer
	 */
	public RingBuffer<Event> getDisruptorBuffer();
	
	/**
	 * @return
	 */
	public WAL getProcessorWal();
	
	/**
	 * @param eventId
	 * @throws IOException 
	 */
	public void ackEvent(long eventId) throws IOException;
	
	public void start() throws Exception;
	
	public void stop() throws Exception;

}
