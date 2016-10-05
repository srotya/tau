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
package com.srotya.tau.wraith.silo.ignite;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;

import com.clearspring.analytics.stream.cardinality.HyperLogLogPlus;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.store.AggregationStore;

/**
 * @author ambud_sharma
 */
public class IgniteAggregationStore implements AggregationStore {

	private Ignite ignite;
	private IgniteConfiguration cfg;

	@Override
	public void initialize(Map<String, String> conf) {
		cfg = new IgniteConfiguration();
	}

	@Override
	public void connect() throws IOException {
		ignite = Ignition.start(cfg);
	}

	@Override
	public void disconnect() throws IOException {
		ignite.close();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void persist(int taskId, String entity, Aggregator aggregator) throws IOException {
		if(aggregator.getDatastructure() instanceof Set) {
			IgniteCache<String, Set> cache = ignite.cache("");
			Set val = (Set)aggregator.getDatastructure();
			if(cache.containsKey(entity)) {
				Set set = cache.get(entity);
				set.addAll(val);
			}else {
				cache.put(entity, val);
			}
		}else if(aggregator.getDatastructure() instanceof ICardinality) {
			IgniteCache<String, byte[]> cache = ignite.cache("");
			HyperLogLogPlus card = (HyperLogLogPlus) aggregator.getDatastructure();
			if(cache.containsKey(entity)) {
				HyperLogLogPlus hll = HyperLogLogPlus.Builder.build(cache.get(entity));
				try {
					hll.addAll(card);
				} catch (Exception e) {
					throw new IOException(e);
				}
			}else {
				cache.put(entity, card.getBytes());
			}
		}
	}

	@Override
	public void putValue(int taskId, long timestamp, String entity, long count) throws IOException {
	}

	@Override
	public void putValue(int taskId, long timestamp, String entity, int count) throws IOException {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void mergeSetValues(int taskId, String entity, Set<Object> values) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void mergeSetIntValues(int taskId, String entity, Set<Integer> values) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void putValue(int taskId, String entity, ICardinality value) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void persistState(int taskId, String key, MutableBoolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, MutableBoolean> retriveStates(int taskId) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void purgeState(int taskId, String key) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Aggregator> retrive(int taskId, Aggregator aggregator) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
