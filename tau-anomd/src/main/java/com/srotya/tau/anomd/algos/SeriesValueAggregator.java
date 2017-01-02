/**
 * Copyright 2017 Ambud Sharma
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
package com.srotya.tau.anomd.algos;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.srotya.tau.wraith.aggregators.Aggregator;

/**
 * 
 * @author ambud
 */
public abstract class SeriesValueAggregator implements Aggregator {

	private static final long serialVersionUID = 1L;
	private Set<SeriesValue> series;
	
	@Override
	public void initialize(Map<String, String> conf) {
		series = new HashSet<>();
	}

	@Override
	public boolean add(Long timestamp, Object aggregationValue) {
		return series.add(new SeriesValue(timestamp, (Double)aggregationValue));
	}
	
	/**
	 * @return min timestamp in the series
	 */
	public long minTimestamp() {
		long minTs = Long.MAX_VALUE;
		for(SeriesValue value: series) {
			if(value.getTimestamp()<minTs) {
				minTs = value.getTimestamp();
			}
		}
		return minTs;
	}
	
	/**
	 * @return max timestamp in the series
	 */
	public long maxTimestamp() {
		long minTs = Long.MIN_VALUE;
		for(SeriesValue value: series) {
			if(value.getTimestamp()>minTs) {
				minTs = value.getTimestamp();
			}
		}
		return minTs;
	}
	
	@Override
	public long size() {
		return series.size();
	}
	
	public static class SeriesValue implements Serializable {
		
		private static final long serialVersionUID = 1L;
		private Long timestamp;
		private Double value;
		
		public SeriesValue() {
		}
		
		public SeriesValue(Long timestamp, Double value) {
			this.timestamp = timestamp;
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if(obj instanceof SeriesValue) {
				return timestamp == ((SeriesValue)obj).timestamp;
			}else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return Long.hashCode(timestamp);
		}

		/**
		 * @return the timestamp
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * @param timestamp the timestamp to set
		 */
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		/**
		 * @return the value
		 */
		public double getValue() {
			return value;
		}

		/**
		 * @param value the value to set
		 */
		public void setValue(double value) {
			this.value = value;
		}
		
	}
}
