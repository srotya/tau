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
package com.srotya.tau.api.dao;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.ChannelSelector;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.Transaction;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.source.SyslogUDPSource;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.CollectionConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.storage.Point;

import io.dropwizard.lifecycle.Managed;

/**
 * Hendrix performance monitor receives performance stats from the topologies
 * over Syslog UDP and stores them in-memory to be visualized by users.
 * 
 * @author ambud_sharma
 */
public class PerformanceMonitor implements Managed {

	private static final String RULE_ID = "ruleId";
	private static final String TIMESTAMP = "timestamp";
	private static final String RULE_GROUP = "rule_group";
	private static final String DEFAULT_SERIES_CAPACITY = "100";
	private static final String SERIES_CAPACITY = "series.capacity";
	private static final String SERIES_NAME = "seriesName";
	private static final String MCM_METRICS = "mcmMetrics";
	private static Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
	private int seriesSize;
	private IgniteCache<String, Set<String>> seriesLookup;
	private PerfMonChannel localChannel;
	private SyslogUDPSource source;
	private ExecutorService eventProcessor;
	private Ignite ignite;
	private CacheConfiguration<String, Set<String>> cacheCfg;
	private CollectionConfiguration cfg;
	private CollectionConfiguration colCfg;

	public PerformanceMonitor(ApplicationManager am) {
		ignite = am.getIgnite();
	}

	public void initIgniteCache() {
		cacheCfg = new CacheConfiguration<>();
		cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
		cacheCfg.setName(MCM_METRICS);
		cacheCfg.setBackups(2);
		seriesLookup = ignite.getOrCreateCache(cacheCfg);

		colCfg = new CollectionConfiguration();
		colCfg.setCollocated(true);
		colCfg.setBackups(1);
		colCfg.setCacheMode(CacheMode.REPLICATED);
		seriesSize = Integer.parseInt(System.getProperty(SERIES_CAPACITY, DEFAULT_SERIES_CAPACITY));

		cfg = new CollectionConfiguration();
		cfg.setBackups(1);
		cfg.setCacheMode(CacheMode.REPLICATED);
		logger.info("Initialized Ignite cache for performance monitoring");
	}

	public void initSyslogServer() {
		eventProcessor = Executors.newSingleThreadExecutor();
		source = new SyslogUDPSource();
		localChannel = new PerfMonChannel();
		localChannel.start();
		eventProcessor.submit(() -> {
			Event event = null;
			while (true) {
				try {
					event = localChannel.take();
					processEvent(event);
					Thread.sleep(10);
				} catch (Exception e) {
					logger.severe("Error processing metrics:"+e.getMessage());
					e.printStackTrace();
				}
			}
		});
		LocalChannelSelector selector = new LocalChannelSelector();
		selector.setChannels(Arrays.asList(localChannel));
		ChannelProcessor processor = new ChannelProcessor(selector);
		source.setChannelProcessor(processor);
		source.setName("syslogSource");
		Context context = new Context();
		context.put("host", "0.0.0.0");
		context.put("port", "5140");
		source.configure(context);
		source.start();
		logger.info("Syslog server initalized");
	}

	@Override
	public void start() throws Exception {
		initIgniteCache();
		initSyslogServer();
	}

	@Override
	public void stop() throws Exception {
		source.stop();
		eventProcessor.shutdownNow();
	}

	public void processEvent(Event event) throws Exception {
		long ts = Long.parseLong(event.getHeaders().get(TIMESTAMP));
		String message = new String(event.getBody());
		message = message.substring(message.indexOf('{'));
		// Send the Event to the external repository.
		Gson gson = new Gson();
		JsonElement b = gson.fromJson(message, JsonElement.class);
		JsonObject obj = b.getAsJsonObject();
		String seriesName = obj.get(SERIES_NAME).getAsString();
		if (seriesName.startsWith("mcm") && seriesName.contains("rule")) {
			String ruleGroup = obj.get(RULE_GROUP).getAsString();
			String ruleId = obj.get(RULE_ID).getAsString();
			Set<String> ruleGroups = seriesLookup.get(seriesName);
			if (ruleGroups == null) {
				ruleGroups = new HashSet<>();
				seriesLookup.put(seriesName, ruleGroups);
			}
			if (ruleGroups.add(seriesName + "_" + ruleGroup)) {
				seriesLookup.put(seriesName, ruleGroups);
			}
			IgniteSet<String> set = ignite.set(seriesName + "_" + ruleGroup, cfg);
			set.add(ruleId);
			IgniteQueue<Entry<Long, Number>> queue = ignite.queue(ruleId, seriesSize, colCfg);
			if (queue.size() >= seriesSize) {
				queue.remove();
			}
			Iterator<Entry<Long, Number>> iterator = queue.iterator();
			while(iterator.hasNext()) {
				Entry<Long, Number> next = iterator.next();
				if(System.currentTimeMillis() - next.getKey() > 1000 * 60) {
					iterator.remove();
				}
			}
			
			queue.add(new AbstractMap.SimpleEntry<Long, Number>(System.currentTimeMillis(),
					obj.get("value").getAsNumber()));
			logger.fine("Processed event:" + obj);
		} else if (seriesName.startsWith("cm")) {
			IgniteQueue<Entry<Long, Number>> queue = ignite.queue(seriesName, seriesSize, colCfg);
			queue.add(new AbstractMap.SimpleEntry<Long, Number>(ts, obj.get("value").getAsNumber()));
			if (queue.size() >= seriesSize) {
				queue.remove();
			}
		}
	}

	/**
	 * @param seriesName
	 * @param ruleGroup
	 * @return
	 */
	public Map<String, List<Point>> getSeriesForRuleGroup(String seriesName, String ruleGroup, int filterSeconds) {
		Map<String, List<Point>> efficiencySeries = new HashMap<>();
		Set<String> set = seriesLookup.get(seriesName);
		long ts = -1;
		if (set != null && !set.isEmpty()) {
			for (String series : set) {
				if (series.contains(ruleGroup)) {
					IgniteSet<String> rules = ignite.set(series, cfg);
					for (String ruleId : rules) {
						IgniteQueue<Entry<Long, Number>> queue = ignite.queue(ruleId, seriesSize, colCfg);
						List<Point> list = queueToList(queue);
						if (list.size() > 0) {
							long tts = list.get(list.size() - 1).getKey();
							if (tts > ts) {
								ts = tts;
							}
							efficiencySeries.put(ruleId, list);
						}
					}
				}
			}
			for (Iterator<Entry<String, List<Point>>> iterator = efficiencySeries.entrySet().iterator(); iterator
					.hasNext();) {
				Entry<String, List<Point>> entry = iterator.next();
				if (entry.getValue().size() > 0 && entry.getValue().get(entry.getValue().size() - 1)
						.getKey() < (ts - ((long) filterSeconds) * 1000)) {
					iterator.remove();
				}
			}
		} else {
			System.out.println("Perf stats not found for rule group:" + ruleGroup);
		}
		return efficiencySeries;
	}

	/**
	 * @param seriesName
	 * @return
	 */
	public List<Point> getSeries(String seriesName) {
		return queueToList(ignite.queue(seriesName, seriesSize, colCfg));
	}

	/**
	 * @param queue
	 * @return
	 */
	public List<Point> queueToList(Queue<Entry<Long, Number>> queue) {
		List<Point> list = new ArrayList<>();
		for (Entry<Long, Number> entry : queue) {
			list.add(new Point(entry.getKey(), entry.getValue()));
		}
		return list;
	}

	/**
	 * @return the seriesLookup
	 */
	protected IgniteCache<String, Set<String>> getSeriesLookup() {
		return seriesLookup;
	}

	/**
	 * @return the ignite
	 */
	protected Ignite getIgnite() {
		return ignite;
	}

	/**
	 * @return the cacheCfg
	 */
	protected CacheConfiguration<String, Set<String>> getCacheCfg() {
		return cacheCfg;
	}

	/**
	 * @return the cfg
	 */
	protected CollectionConfiguration getCfg() {
		return cfg;
	}

	/**
	 * @return the colCfg
	 */
	protected CollectionConfiguration getColCfg() {
		return colCfg;
	}

	/**
	 * @return the seriesSize
	 */
	protected int getSeriesSize() {
		return seriesSize;
	}

	public static class LocalChannelSelector implements ChannelSelector {

		private List<Channel> channels;

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return "localChannelSelector";
		}

		@Override
		public void configure(Context context) {
		}

		@Override
		public void setChannels(List<Channel> channels) {
			this.channels = channels;
		}

		@Override
		public List<Channel> getRequiredChannels(Event event) {
			return channels;
		}

		@Override
		public List<Channel> getOptionalChannels(Event event) {
			return new ArrayList<>();
		}

		@Override
		public List<Channel> getAllChannels() {
			return channels;
		}

	}

	public static class PerfMonChannel implements Channel {

		private ArrayBlockingQueue<Event> eventQueue;

		private LifecycleState state;

		public PerfMonChannel() {
		}

		@Override
		public void start() {
			eventQueue = new ArrayBlockingQueue<>(1000);
			state = LifecycleState.START;
		}

		@Override
		public void stop() {
			state = LifecycleState.STOP;
		}

		@Override
		public LifecycleState getLifecycleState() {
			return state;
		}

		@Override
		public void setName(String name) {
		}

		@Override
		public String getName() {
			return PerfMonChannel.class.getName();
		}

		@Override
		public void put(Event event) throws ChannelException {
			try {
				eventQueue.put(event);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@Override
		public Event take() throws ChannelException {
			try {
				return eventQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public Transaction getTransaction() {
			return new org.apache.flume.channel.PseudoTxnMemoryChannel.NoOpTransaction();
		}

	}

}