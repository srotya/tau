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

import backtype.storm.metric.api.IMetricsConsumer;
import com.timgroup.statsd.StatsDClient;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;

/**
 * Handles only internal storm metrics (for now). Then push it to statsD as
 * gauge.
 */
public class GaugeMetricsProcessor extends AbstractMetricsProcessor {
	
	private static Logger logger = Logger.getLogger(GaugeMetricsProcessor.class);

	public GaugeMetricsProcessor(StatsDClient statsDClient) {
		super(statsDClient, "gm.");
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void processDataPoint(IMetricsConsumer.DataPoint dataPoint) {
		// identify the value type and process depends on that
		if (dataPoint.value instanceof Map) {
			Set<Map.Entry> metrics = ((Map) dataPoint.value).entrySet();
			for (Map.Entry metric : metrics) {
				String metricName = dataPoint.name + "." + metric.getKey();
				// send metric to statsd, expect it has Long value
				processLongValueMetric(metricName, (Long) metric.getValue());
			}
		} else {
			// the rest of gauge metrics we are using - double values. Add the
			// flexibility in case of future metrics updates.
			processDoubleValueMetric(dataPoint.name, (Double) dataPoint.value);
		}
	}

	private void processDoubleValueMetric(String metricName, Double metricValue) {
		statsDClient.gauge(metricName, metricValue);
	}

	private void processLongValueMetric(String metricName, Long metricValue) {
		statsDClient.gauge(metricName, metricValue);
	}
	
}
