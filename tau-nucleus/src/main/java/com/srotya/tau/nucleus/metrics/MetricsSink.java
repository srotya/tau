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
package com.srotya.tau.nucleus.metrics;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * @author ambudsharma
 */
public class MetricsSink {
	
	private static final MetricsSink sink = new MetricsSink();
	private SortedMap<String, AtomicLong> intMetrics;
	private SortedMap<String, AtomicDouble> floatMetrics;
	
	/**
	 * 
	 */
	private MetricsSink() {
		intMetrics = new TreeMap<>();
		floatMetrics = new TreeMap<>();
	}
	
	/**
	 * @return
	 */
	public static MetricsSink getInstance() {
		return sink;
	}
	
	public void publishIntMetric(String metricName, long value) {
		AtomicLong val = intMetrics.get(metricName);
		if(val == null) {
			val = new AtomicLong(0);
			intMetrics.put(metricName, val);
		}
		val.set(value);
	}
	
	public void publishFloatMetric(String metricName, double value) {
		AtomicDouble val = floatMetrics.get(metricName);
		if(val == null) {
			val = new AtomicDouble(0);
			floatMetrics.put(metricName, val);
		}
		val.set(value);
	}
	
	public long getIntMetric(String metricName) throws Exception {
		AtomicLong val = intMetrics.get(metricName);
		if(val!=null) {
			return val.get();
		}else {
			throw new Exception("Metric '"+metricName+"' not found");
		}
	}
	
	public double getDoubleMetric(String metricName) throws Exception {
		AtomicDouble val = floatMetrics.get(metricName);
		if(val!=null) {
			return val.get();
		}else {
			throw new Exception("Metric '"+metricName+"' not found");
		}
	}
}