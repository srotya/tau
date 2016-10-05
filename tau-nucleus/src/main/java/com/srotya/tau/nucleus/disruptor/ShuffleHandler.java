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
package com.srotya.tau.nucleus.disruptor;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;

/**
 * @author ambudsharma
 *
 */
public abstract class ShuffleHandler implements EventHandler<Event> {

	private int taskId;
	private MutableInt taskCount;

	public ShuffleHandler(int taskId, MutableInt taskCount) {
		this.taskCount = taskCount;
		if (taskCount.getVal() <= 0) {
			throw new RuntimeException("Invalid task count, must be a positive number");
		}
		this.taskId = taskId;
	}

	@Override
	public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		if (sequence % taskCount.getVal() == taskId) {
			consumeEvent(event, sequence, endOfBatch);
		}
	}

	/**
	 * This method is NOT for external calls and is to be used for delegated
	 * functionality for the implementing class.
	 * 
	 * @param event
	 * @param sequence
	 * @param endOfBatch
	 * @throws Exception
	 */
	public abstract void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception;

}
