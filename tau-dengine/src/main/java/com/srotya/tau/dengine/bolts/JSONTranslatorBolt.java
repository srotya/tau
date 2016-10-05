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
package com.srotya.tau.dengine.bolts;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.dengine.UnifiedFactory;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.PerformantException;

import backtype.storm.metric.api.CountMetric;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Bolt to translate data from Logstash Json to {@link Event} format
 * 
 * @author ambud_sharma
 */
public class JSONTranslatorBolt extends BaseRichBolt {

	private static final String _METRIC_TRANSLATOR_FAIL = "cm.translator.fail";
	private static final String _METRIC_TRANSLATOR_SUCCESS = "cm.translator.success";
	private static final String DEFAULT_RULE_GROUP_FIELD = "rule_group";
	private static final String DEFAULT_TIMESTAMP = "@timestamp";
	private static final String TRANSLATOR_TIMESTAMP_KEY = "translator.timestampKey";
	private static final String TRANSLATOR_TENAN_ID_KEY = "translator.tenanIdKey";
	private static final long serialVersionUID = 1L;
	private static final PerformantException INVALID_JSON = new PerformantException("Invalid JSON");
	private transient Logger logger;
	private transient Type type;
	private transient UnifiedFactory factory;
	private transient Gson gson;
	private transient OutputCollector collector;
	private transient String timestampKey;
	private transient String ruleGroupField;
	private transient CountMetric sucessMetric;
	private transient CountMetric failMetric;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.logger = Logger.getLogger(JSONTranslatorBolt.class.getName());
		this.collector = collector;
		gson = new GsonBuilder().create();
		type = new TypeToken<HashMap<String, Object>>() {
		}.getType();
		factory = new UnifiedFactory();
		if (stormConf.get(TRANSLATOR_TIMESTAMP_KEY) != null) {
			this.timestampKey = stormConf.get(TRANSLATOR_TIMESTAMP_KEY).toString();
		} else {
			timestampKey = DEFAULT_TIMESTAMP;
		}
		if (stormConf.get(TRANSLATOR_TENAN_ID_KEY) != null) {
			this.timestampKey = stormConf.get(TRANSLATOR_TENAN_ID_KEY).toString();
		} else {
			ruleGroupField = DEFAULT_RULE_GROUP_FIELD;
		}
		sucessMetric = new CountMetric();
		failMetric = new CountMetric();
		if (context != null) {
			context.registerMetric(_METRIC_TRANSLATOR_SUCCESS, sucessMetric, Constants.METRICS_FREQUENCY);
			context.registerMetric(_METRIC_TRANSLATOR_FAIL, failMetric, Constants.METRICS_FREQUENCY);
		}
		logger.info("Translator bolt initialized");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Tuple input) {
		String eventLine = input.getString(0);
		try {
			Event event = factory.buildEvent();
			Map<String, Object> map = (Map<String, Object>) gson.fromJson(eventLine, type);
			if (map != null) {
				event.getHeaders().putAll(map);
				event.getHeaders().put(Constants.FIELD_TIMESTAMP,
						((Double) event.getHeaders().get(timestampKey)).longValue());
				Object id = event.getHeaders().get(ruleGroupField);
				if (id != null) {
					event.getHeaders().put(Constants.FIELD_RULE_GROUP, id);
				}
				collector.emit(input, new Values(event));
			} else {
				throw INVALID_JSON;
			}
			if (sucessMetric != null) {
				sucessMetric.incr();
			}
		} catch (Exception e) {
			// emit error
			if (failMetric != null) {
				failMetric.incr();
			}
			StormContextUtil.emitErrorTuple(collector, input, JSONTranslatorBolt.class, "JSON to Map issue", eventLine,
					e);
		}
		collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(Constants.FIELD_EVENT));
		StormContextUtil.declareErrorStream(declarer);
	}

	/**
	 * @return the type
	 */
	protected Type getType() {
		return type;
	}

	/**
	 * @return the factory
	 */
	protected UnifiedFactory getFactory() {
		return factory;
	}

	/**
	 * @return the gson
	 */
	protected Gson getGson() {
		return gson;
	}

	/**
	 * @return the collector
	 */
	protected OutputCollector getCollector() {
		return collector;
	}

}