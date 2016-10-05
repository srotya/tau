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
package com.srotya.tau.wraith.silo.redis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.aggregators.Aggregator;
import com.srotya.tau.wraith.aggregators.CoarseCountingAggregator;
import com.srotya.tau.wraith.aggregators.FineCountingAggregator;
import com.srotya.tau.wraith.aggregators.SetAggregator;
import com.srotya.tau.wraith.store.AggregationStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @author ambud_sharma
 */
public class RedisAggregationStore implements AggregationStore {

	private static final String PREFIX_STATES = "states_";
	private static final String DEFAULT_REDIS_PORT = "6379";
	private static final String DEFAULT_SENTINEL_PORT = "26379";
	private static final String ASTORE_REDIS_PORT = "astore.redis.port";
	private static final String ASTORE_REDIS_HOST = "astore.redis.host";
	private static final String ASTORE_REDIS_CLUSTER_NAME = "astore.redis.clusterName";
	private static final String ASTORE_REDIS_SENTINEL = "astore.redis.sentinel";
	private static final Logger logger = LoggerFactory.getLogger(RedisAggregationStore.class);
	private JedisSentinelPool sentinel;
	private Jedis redis;
	private boolean isSentinel;
	private String masterName;
	private String host;
	private int port;

	@Override
	public void initialize(Map<String, String> conf) {
		this.isSentinel = Boolean.parseBoolean(conf.getOrDefault(ASTORE_REDIS_SENTINEL, "false").toString());
		this.masterName = isSentinel ? conf.get(ASTORE_REDIS_CLUSTER_NAME).toString() : null;
		this.host = conf.get(ASTORE_REDIS_HOST).toString();
		this.port = Integer.parseInt(conf
				.getOrDefault(ASTORE_REDIS_PORT, isSentinel ? DEFAULT_SENTINEL_PORT : DEFAULT_REDIS_PORT).toString());
	}

	@Override
	public void connect() throws IOException {
		if (isSentinel) {
			String[] vals = host.split(",");
			for (int i = 0; i < vals.length; i++) {
				vals[i] = vals[i] + ":" + port;
			}
			logger.info("Attempting to connect to Redis sentinels:" + Arrays.toString(vals));
			sentinel = new JedisSentinelPool(masterName, new HashSet<>(Arrays.asList(vals)));
		} else {
			redis = new Jedis(host, port);
		}
		logger.info("Successfully connected to Redis");
	}

	@Override
	public void disconnect() throws IOException {
		if (isSentinel && sentinel != null) {
			sentinel.close();
		} else {
			redis.close();
		}
	}

	@Override
	public void putValue(int taskId, long timestamp, String entity, long count) {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		redis.set(buildAggregationKey(taskId, timestamp, entity), String.valueOf(count));
	}

	@Override
	public void putValue(int taskId, long timestamp, String entity, int count) {
		putValue(taskId, timestamp, entity, (long) count);
	}

	public static String buildAggregationKey(int taskId, long timestamp, String entity) {
		return prefixAggregation(taskId) + "_" + Utils.longToString(timestamp);
	}

	public static String prefixAggregation(int taskId) {
		return "agg_" + taskId + "_";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void persist(int taskId, String entity, Aggregator aggregator) throws IOException {
		if (aggregator.getClass() == FineCountingAggregator.class) {
			mergeSetIntValues(taskId, entity, (Set<Integer>) aggregator.getDatastructure());
		} else if (aggregator.getClass() == SetAggregator.class) {
			mergeSetValues(taskId, entity, (Set<Object>) aggregator.getDatastructure());
		} else if (aggregator.getClass() == CoarseCountingAggregator.class) {
			putValue(taskId, entity, ((ICardinality) aggregator.getDatastructure()));
		}
	}

	@Override
	public Map<String, Aggregator> retrive(int taskId, Aggregator template) throws IOException {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		Map<String, Aggregator> aggregatorMap = new HashMap<>();
		Set<String> smembers = null;
		String prefix = "";
		Set<String> keys = redis.keys(prefix);
		for (String key : keys) {
			Aggregator instance = template.getInstance();
			Object data = null;
			if (template.getClass() == SetAggregator.class) {
				smembers = redis.smembers(key);
			} else if (template.getClass() == FineCountingAggregator.class) {
				smembers = redis.smembers(key);
				instance.initialize(stringSetToInteger(smembers));
			} else if (template.getClass() == CoarseCountingAggregator.class) {
				String card = redis.get(prefixICard(taskId));
				if(card!=null) {
					instance.initialize(Base64.getDecoder().decode(card));
				}
			}
			instance.initialize(data);
		}
		return aggregatorMap;
	}

	public Set<Integer> stringSetToInteger(Set<String> stringSet) {
		Set<Integer> set = new HashSet<>();
		for (String object : stringSet) {
			set.add(Integer.parseInt(object));
		}
		return set;
	}

	@Override
	public void mergeSetValues(int taskId, String entity, Set<Object> values) {
		mergeSetValues(taskId, entity, objectSetToList(values));
	}

	/**
	 * @param entity
	 * @param vals
	 */
	public void mergeSetValues(int taskId, String entity, List<String> vals) {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		redis.sadd(taskId + "_" + entity, vals.toArray(new String[1]));
	}

	public List<String> objectSetToList(Set<Object> values) {
		List<String> vals = new ArrayList<>();
		for (Object val : values) {
			vals.add(val.toString());
		}
		return vals;
	}

	public List<String> integerSetToList(Set<Integer> values) {
		List<String> vals = new ArrayList<>();
		for (Integer val : values) {
			vals.add(Utils.intToString(val));
		}
		return vals;
	}

	@Override
	public void putValue(int taskId, String entity, ICardinality value) throws IOException {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		String key = prefixICardKey(taskId, entity);
		redis.set(key, Base64.getEncoder().encodeToString(value.getBytes()));
	}

	private String prefixICardKey(int taskId, String entity) {
		return prefixICard(taskId) + entity;
	}

	private String prefixICard(int taskId) {
		String key = taskId + "_icard_";
		return key;
	}

	@Override
	public void mergeSetIntValues(int taskId, String entity, Set<Integer> values) {
		mergeSetValues(taskId, entity, integerSetToList(values));
	}

	@Override
	public void persistState(int taskId, String key, MutableBoolean value) throws IOException {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		redis.set(buildStateKey(taskId, key), String.valueOf(value.isVal()));
	}

	@Override
	public Map<String, MutableBoolean> retriveStates(int taskId) throws IOException {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		Map<String, MutableBoolean> states = new HashMap<>();
		String prefix = prefixState(taskId);
		Set<String> keys = redis.keys(prefix);
		for (String key : keys) {
			states.put(key.replace(prefix, ""), new MutableBoolean(Boolean.parseBoolean(redis.get(key))));
		}
		return states;
	}

	@Override
	public void purgeState(int taskId, String key) throws IOException {
		if (isSentinel) {
			redis = sentinel.getResource();
		}
		redis.del(buildStateKey(taskId, key));
	}

	/**
	 * Build state key to store state value into redis
	 * 
	 * @param taskId
	 * @param key
	 * @return key for redis
	 */
	public static String buildStateKey(int taskId, String key) {
		return prefixState(taskId) + key;
	}

	/**
	 * Build prefixes for state saving
	 * 
	 * @param taskId
	 * @return
	 */
	public static String prefixState(int taskId) {
		return PREFIX_STATES + taskId + "_";
	}

	/**
	 * @return the redis
	 */
	protected Jedis getRedis() {
		return redis;
	}

	/**
	 * @param redis
	 *            the redis to set
	 */
	protected void setRedis(Jedis redis) {
		this.redis = redis;
	}

}
