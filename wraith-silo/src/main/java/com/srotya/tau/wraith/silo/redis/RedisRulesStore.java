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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.store.RulesStore;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * {@link RulesStore} implementation backed by Redis and key pattern rule:&lt;ruleId&gt;
 * 
 * @author ambud_sharma
 */
public class RedisRulesStore implements RulesStore {
	
	public static final String RULE_KEY_PATTERN = "rule:*";
	private static final Logger logger = LoggerFactory.getLogger(RedisRulesStore.class);
	private JedisSentinelPool sentinel;
	private Jedis redis;
	private boolean isSentinel;
	private String masterName;
	private String host;
	private int port;
	
	public RedisRulesStore() {
	}
	
	@Override
	public void initialize(Map<String, String> conf) {
		this.isSentinel = Boolean.parseBoolean(conf.getOrDefault("rstore.redis.sentinel", "false").toString());;
		this.masterName = isSentinel?conf.get("rstore.redis.masterName").toString():null;
		this.host = conf.get("rstore.redis.host");
		this.port = Integer.parseInt(conf.getOrDefault("rstore.redis.port", isSentinel?"26379":"6379").toString());
	}

	@Override
	public void connect() throws IOException {
		if(isSentinel) {
			sentinel = new JedisSentinelPool(masterName, new HashSet<>(Arrays.asList(host.split(","))));
		}else {
			redis = new Jedis(host, port);
		}
	}

	@Override
	public void disconnect() throws IOException {
		// do nothing for direct Redis connection, close connection for Sentinel
		if(isSentinel && sentinel!=null) {
			sentinel.close();
		}
	}

	@Override
	public Map<String, Map<Short, Rule>> listGroupedRules() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
