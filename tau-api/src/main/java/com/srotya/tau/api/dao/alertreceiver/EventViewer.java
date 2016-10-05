/**
 * Copyright 2016 Symantec Corporation.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.api.dao.alertreceiver;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.api.ApplicationManager;

import io.dropwizard.lifecycle.Managed;

/**
 * Backend for alert receiver functionality
 * 
 * @author ambud_sharma
 */
public class EventViewer implements Managed {

	private static final String DEFAULT_CHANNEL_CAPACITY = "1000";
	private static final String CHANNEL_CAPACITY = "channel.capacity";
	private static final Logger logger = Logger.getLogger(EventViewer.class.getName());
	private static final String CHANNELS = "channels";
	private int channelSize = 0;
	private Ignite ignite;
	private IgniteCache<String, Integer> channelCache;
	private Map<String, BlockingQueue<Map<String, Object>>> queueMap;
	private KafkaConsumer<String, String> consumer;
	private ExecutorService pool;

	public EventViewer(ApplicationManager am) {
		this.ignite = am.getIgnite();
		this.consumer = am.getConsumer();
		CacheConfiguration<String, Integer> cacheCfg = new CacheConfiguration<>();
		cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
		cacheCfg.setName(CHANNELS);
		cacheCfg.setBackups(2);
		channelCache = ignite.getOrCreateCache(cacheCfg);
		logger.info("Channel cache:" + channelCache);
		channelSize = Integer.parseInt(System.getProperty(CHANNEL_CAPACITY, DEFAULT_CHANNEL_CAPACITY));
		queueMap = new HashMap<>();

		ignite.events().remoteListen(new CacheBiListenter(queueMap, channelSize),
				new CacheEventListener(queueMap, channelSize), EventType.EVT_CACHE_OBJECT_PUT,
				EventType.EVT_CACHE_OBJECT_REMOVED);
	}

	public void initializeKafkaConsumer() throws Exception {
		channelCache.forEach(e->{
			if(!queueMap.containsKey(e.getKey())) {
				queueMap.put(String.valueOf(e.getKey()), new ArrayBlockingQueue<>(channelSize));
			}
		});
		if (!ApplicationManager.LOCAL) {
			pool = Executors.newCachedThreadPool();

			pool.submit(() -> {
				try {
					Type type = new TypeToken<Map<String, Object>>() {
					}.getType();
					Gson gson = new Gson();
					while (true) {
						Map<String, ConsumerRecords<String, String>> records = consumer.poll(100);
						for (Iterator<Entry<String, ConsumerRecords<String, String>>> iterator = records.entrySet()
								.iterator(); iterator.hasNext();) {
							Entry<String, ConsumerRecords<String, String>> entry = iterator.next();
							ConsumerRecords<String, String> value = entry.getValue();
							for (ConsumerRecord<String, String> record : value.records()) {
								publishEvent(Short.parseShort(record.key()), gson.fromJson(record.value(), type));
							}
						}
					}
				} catch (Exception e) {
				}
			});
		}
		logger.info("Started event receiver");
	}

	@Override
	public void start() throws Exception {
		initializeKafkaConsumer();
	}

	@Override
	public void stop() throws Exception {
		pool.shutdownNow();
		pool.awaitTermination(10000, TimeUnit.SECONDS);
	}

	/**
	 * @param ruleId
	 */
	public void openChannel(Short ruleId) {
		if (ruleId != null) {
			String ruleIdStr = String.valueOf(ruleId);
			if (!channelCache.containsKey(ruleIdStr)) {
				channelCache.put(ruleIdStr, 1);
				logger.info("Adding channel for :" + ruleId);
			} else {
				Integer result = channelCache.get(ruleIdStr);
				channelCache.put(ruleIdStr, result + 1);
				logger.info("Channel for rule:" + ruleId + " is already open");
			}
		}
	}

	/**
	 * Put a new queue in the local map
	 * 
	 * @param ruleId
	 */
	public void putQueue(String ruleId) {
		queueMap.put(String.valueOf(ruleId), new ArrayBlockingQueue<>(channelSize));
	}

	/**
	 * Remove queue from the local map
	 * 
	 * @param ruleId
	 */
	public void removeQueue(String ruleId) {
		queueMap.remove(String.valueOf(ruleId));
	}

	/**
	 * @param ruleId
	 * @param event
	 * @return
	 * @throws InterruptedException
	 */
	public boolean publishEvent(short ruleId, Map<String, Object> event) throws InterruptedException {
		if (channelCache.containsKey(String.valueOf(ruleId))) {
			BlockingQueue<Map<String, Object>> channel = queueMap.get(String.valueOf(ruleId));
			if (channel != null) {
				if (channel.size() >= channelSize) {
					channel.take(); // evict event
				}
				channel.put(event);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * @return the channelSize
	 */
	public int getChannelSize() {
		return channelSize;
	}

	/**
	 * @param ruleId
	 * @return
	 */
	public Queue<Map<String, Object>> getChannel(short ruleId) {
		if (channelCache.containsKey(String.valueOf(ruleId))) {
			return queueMap.get(String.valueOf(ruleId));
		} else {
			return null;
		}
	}

	/**
	 * @param ruleId
	 */
	public void closeChannel(short ruleId) {
		String ruleIdStr = String.valueOf(ruleId);
		if (channelCache.containsKey(ruleIdStr)) {
			Integer val = channelCache.get(ruleIdStr);
			if (val > 1) {
				channelCache.put(ruleIdStr, val - 1);
			} else {
				channelCache.remove(ruleIdStr);
				queueMap.remove(ruleIdStr);
			}
		} else {
			// do nothing
		}
	}

	/**
	 * @return the ignite
	 */
	protected Ignite getIgnite() {
		return ignite;
	}

	/**
	 * @return the channelCache
	 */
	protected IgniteCache<String, Integer> getChannelCache() {
		return channelCache;
	}

}