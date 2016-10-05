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
package com.srotya.tau.wraith;

import java.util.HashMap;
import java.util.Map;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;
import com.srotya.tau.wraith.store.AggregationStore;
import com.srotya.tau.wraith.store.RulesStore;
import com.srotya.tau.wraith.store.StoreFactory;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * Unified factory implementation for Tau
 * 
 * @author ambudsharma
 */
public class UnifiedFactory implements StoreFactory, EventFactory {
	
	private static final String STORE_PROP_PREFIX = "store.";

	@Override
	public RulesStore getRulesStore(String type, Map<String, String> stormConf) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Map<String, String> conf = getSubmap(STORE_PROP_PREFIX, stormConf);
		RulesStore store = (RulesStore) Class.forName(type).newInstance();
		store.initialize(conf);
		return store;
	}

	@Override
	public Event buildEvent() {
		return new TauEvent();
	}
	
	/**
	 * @param headers
	 * @return
	 */
	public Event buildEvent(Map<String, Object> headers) {
		return new TauEvent(headers);
	}
	
	@Override
	public TemplateStore getTemplateStore(String type, Map<String, String> stormConf) throws Exception {
		Map<String, String> conf = getSubmap(STORE_PROP_PREFIX, stormConf);
		TemplateStore store = (TemplateStore) Class.forName(type).newInstance();
		store.initialize(conf);
		return store;
	}
	
	public static Map<String, String> getSubmap(String contains, Map<String, String> stormConf) {
		Map<String, String> conf = new HashMap<>();
		for(Object key:stormConf.keySet()) {
			if(key.toString().contains(contains)) {
				conf.put(key.toString(), stormConf.get(key).toString());
			}
		}
		return conf;
	}

	@Override
	public AggregationStore getAggregationStore(String type, Map<String, String> stormConf) throws Exception {
		Map<String, String> conf = getSubmap(STORE_PROP_PREFIX, stormConf);
		AggregationStore store = (AggregationStore) Class.forName(type).newInstance();
		store.initialize(conf);
		return store;
	}

}