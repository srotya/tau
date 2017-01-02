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

import java.util.BitSet;
import java.util.List;
import java.util.Random;

import fi.iki.yak.ts.compression.gorilla.Compressor;
import fi.iki.yak.ts.compression.gorilla.Decompressor;

/**
 * @author ambud
 *
 */
public class CompressedTimeSeries {

	private String seriesName;
	private Compressor compressor;
	private Decompressor decompressor;
	
	public CompressedTimeSeries(String seriesName) {
		this.seriesName = seriesName;
	}
	
	public void addDataPoint(long timestamp, double value) {
	}
	
	public List<DataPoint> getSeries() {
		return null;
	}
	
	public static void main(String[] args) {
		CompressedTimeSeries series = new CompressedTimeSeries("mySeries");
		long ts = System.currentTimeMillis();
		for(int i=0;i<100;i++) {
			series.addDataPoint(ts+i, ts+i);
		}
		
	}
	
	public static class DataPoint {
		
		private long timestamp;
		private double value;
		
	}
	
}
