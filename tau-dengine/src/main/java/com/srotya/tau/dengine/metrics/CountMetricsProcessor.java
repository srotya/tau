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

/**
 * Handles count metric and push it in statsD. Typically this metrics don't
 * belong to particular tenant.
 */
public class CountMetricsProcessor extends AbstractMetricsProcessor {
	
	private Logger logger = Logger.getLogger(CountMetricsProcessor.class);

	public CountMetricsProcessor(StatsDClient statsDClient) {
		super(statsDClient, "cm.");
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void processDataPoint(IMetricsConsumer.DataPoint dataPoint) {
		statsDClient.count(dataPoint.name, (long) dataPoint.value);
	}
	
}
