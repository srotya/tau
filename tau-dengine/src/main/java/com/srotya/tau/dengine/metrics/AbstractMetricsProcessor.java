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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.timgroup.statsd.StatsDClient;

import backtype.storm.metric.api.IMetricsConsumer;

/**
 * Filters all metrics by prefix in metric name. Then process it if matches.
 */
public abstract class AbstractMetricsProcessor implements IMetricsProcessor {
	
	protected final StatsDClient statsDClient;
	private final Set<String> metricsPrefixes;

	protected AbstractMetricsProcessor(StatsDClient statsDClient, String... metricsPrefixes) {
		this.statsDClient = statsDClient;
		this.metricsPrefixes = new HashSet<>();
		Collections.addAll(this.metricsPrefixes, metricsPrefixes);
	}

	public void processDataPoints(Collection<IMetricsConsumer.DataPoint> dataPoints) {
		for (IMetricsConsumer.DataPoint dataPoint : dataPoints) {
			if (isHandledMetric(dataPoint.name)) {
				try {
					processDataPoint(dataPoint);
					getLogger().debug(
							"Metric " + dataPoint.name + " with value " + dataPoint.value + "was sent to statsd");
				} catch (Exception e) {
					getLogger().error("Unable to process metric " + dataPoint.name, e);
				}
			}
		}
	}

	protected abstract Logger getLogger();

	protected abstract void processDataPoint(IMetricsConsumer.DataPoint dataPoint);

	private boolean isHandledMetric(String dataPointName) {
		// empty case
		if (dataPointName == null) {
			return false;
		}
		// check if we need to process this specific gauge metric
		for (String prefix : metricsPrefixes) {
			if (dataPointName.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
	
}
