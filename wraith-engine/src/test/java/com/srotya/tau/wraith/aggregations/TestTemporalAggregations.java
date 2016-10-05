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
package com.srotya.tau.wraith.aggregations;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TestFactory;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregators.AggregationRejectException;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.aggregators.CountingEngine;
import com.srotya.tau.wraith.aggregators.FineCountingAggregator;

public class TestTemporalAggregations {

	private static final String AGGREGATION_KEY = "1233_hello";
	private Map<String, String> conf;
	private TestFactory factory;

	@Before
	public void before() {
		conf = new HashMap<>();
		factory = new TestFactory();
	}

	@Test
	public void testTemporalEmits() throws Exception {
		for (int k = 0; k < 10; k++) {
			CountingEngine aggregationEngine = new CountingEngine(factory, factory, FineCountingAggregator.class.getName());
			conf.put(Constants.COUNTER_TYPE, FineCountingAggregator.class.getName());
			conf.put(Constants.AGGREGATION_JITTER_TOLERANCE, String.valueOf(k * 10));
			aggregationEngine.initialize(conf, 1);
			String ruleActionId = Utils.combineRuleActionId((short) 12, (short) 1233);
			int aggregationWindow = 10;
			long time = 1452452090520L;
			for (int i = 0; i < 100; i++) {
				try {
					long tempTime = time + (i * 1000);
					aggregationEngine.aggregate(tempTime, aggregationWindow, ruleActionId, AGGREGATION_KEY, i);
				} catch (AggregationRejectException e) {
					e.printStackTrace();
				}
			}
			SortedMap<String, Aggregator> map = aggregationEngine.getAggregationMap();
			assertEquals(10, map.size());
			time = 1452452090520L + (100 * 10000);
			for (int i = 100; i < 300; i++) {
				try {
					long tempTime = time + (i * 1000);
					aggregationEngine.aggregate(tempTime, aggregationWindow, ruleActionId, AGGREGATION_KEY, i);
				} catch (AggregationRejectException e) {
					e.printStackTrace();
				}
			}
			assertEquals(30, map.size());
			List<Event> result = new ArrayList<>();
			aggregationEngine.emit(aggregationWindow, ruleActionId, result);
			System.out.println(k + "\t" + result.size() + "\t" + map.size() + "\n"
					+ map.keySet().stream().map(key -> CountingEngine.extractTsFromAggregationKey(key))
							.sorted((i1, i2) -> Integer.compare(i2, i1)).collect(Collectors.toList()));
			assertEquals(29 - k, result.size());
		}
	}

}
