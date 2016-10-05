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
package com.srotya.tau.dengine.metrics;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

import backtype.storm.metric.api.IMetricsConsumer;
import backtype.storm.task.IErrorReporter;
import backtype.storm.task.TopologyContext;

/**
 * Collect instrumentation metrics for different Hendrix components
 * 
 * @author ambud_sharma
 */
public class HendrixMetricsCollector implements IMetricsConsumer {

	private static final String METRICS_JSON_INSTANCES = "metrics.json.instances";
	private static final String METRICS_STATSD = "metrics.statsd";
	private static final String METRICS_PORT = "metrics.port";
	private static final String METRICS_HOST = "metrics.host";
	private static final String METRICS_PREFIX = "metrics.prefix";
	private static final String DEFAULT_TOPOLOGY_METRICS_PREFIX = "hendrix";
	private static final String DEFAULT_TOPOLOGY_METRICS_HOST = "localhost";
	private static final Integer DEFAULT_TOPOLOGY_METRICS_PORT = 8125;
	private Set<IMetricsProcessor> metricsProcessors;
	private StatsDClient statsDClient;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void prepare(Map stormConf, Object registrationArgument, TopologyContext context,
			IErrorReporter errorReporter) {
		metricsProcessors = new HashSet<>();
		boolean statsd = false;
		if (stormConf.containsKey(METRICS_STATSD)) {
			statsd = Boolean.parseBoolean(stormConf.get(METRICS_STATSD).toString());
		}
		if (statsd) {
			String prefix = DEFAULT_TOPOLOGY_METRICS_PREFIX;
			if (stormConf.containsKey(METRICS_PREFIX)) {
				prefix = stormConf.get(METRICS_PREFIX).toString();
			}
			String host = DEFAULT_TOPOLOGY_METRICS_HOST;
			if (stormConf.containsKey(METRICS_HOST)) {
				host = stormConf.get(METRICS_HOST).toString();
			}
			int port = DEFAULT_TOPOLOGY_METRICS_PORT;
			if (stormConf.containsKey(METRICS_PORT)) {
				port = Integer.parseInt(stormConf.get(METRICS_PORT).toString());
			}
			statsDClient = createStatsDClient(prefix, host, port);
			metricsProcessors.add(new GaugeMetricsProcessor(statsDClient));
			metricsProcessors.add(new CountMetricsProcessor(statsDClient));
			metricsProcessors.add(new MultiCountMetricsProcessor(statsDClient));
		} else {
			if (stormConf.containsKey(METRICS_JSON_INSTANCES)) {
				String[] instances = stormConf.get(METRICS_JSON_INSTANCES).toString().split(",");
				SortedMap map = new TreeMap<>(stormConf);
				for (String instance : instances) {
					SortedMap subMap = map.subMap(instance, instance+Character.MAX_VALUE);
					metricsProcessors.add(new JsonMetricProcessor(subMap));
				}
			} else {
				metricsProcessors.add(new JsonMetricProcessor(stormConf));
			}
		}
	}

	StatsDClient createStatsDClient(String prefix, String host, int port) {
		return new NonBlockingStatsDClient(prefix, host, port);
	}

	@Override
	public void handleDataPoints(TaskInfo taskInfo, Collection<DataPoint> dataPoints) {
		for (IMetricsProcessor metricsProcessor : metricsProcessors) {
			metricsProcessor.processDataPoints(dataPoints);
		}
	}

	@Override
	public void cleanup() {
	}

}
