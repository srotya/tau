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
package com.srotya.tau.api.dao.alertreceiver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;

/**
 * @author ambudsharma
 *
 */
public class CacheBiListenter implements IgniteBiPredicate<UUID, CacheEvent> {

	private static final long serialVersionUID = 1L;
	private Map<String, BlockingQueue<Map<String, Object>>> queueMap;
	private int channelSize;

	public CacheBiListenter(Map<String, BlockingQueue<Map<String, Object>>> queueMap, int channelSize) {
		this.queueMap = queueMap;
		this.channelSize = channelSize;
	}
	
	@Override
	public boolean apply(UUID e1, CacheEvent event) {
		if (event.type() == EventType.EVT_CACHE_OBJECT_PUT) {
			putQueue(event.key());
		} else if (event.type() == EventType.EVT_CACHE_OBJECT_REMOVED) {
			removeQueue(event.key());
		}
		return false;
	}

	public void putQueue(String ruleId) {
		queueMap.put(String.valueOf(ruleId), new ArrayBlockingQueue<>(channelSize));
	}

	public void removeQueue(String ruleId) {
		queueMap.remove(String.valueOf(ruleId));
	}

}
