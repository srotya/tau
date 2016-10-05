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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author ambudsharma
 */
public class BGTaskManager {
	
	private static BGTaskManager mgr = new BGTaskManager();
	private ScheduledExecutorService es;
	
	private BGTaskManager() {
		es = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = Executors.defaultThreadFactory().newThread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
	}
	
	/**
	 * @return
	 */
	public static BGTaskManager getInstance() {
		return mgr;
	}
	
	public void schedule(Runnable command, int initialDelay, int period, TimeUnit unit) {
		es.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

}
