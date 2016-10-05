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
package com.srotya.tau.alerts;

import java.util.Map;

import com.google.gson.Gson;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.rules.validator.AlertValidator;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
public class AlertTranslatorBolt extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;
	private transient Gson gson;
	private transient AlertValidator validator;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.gson = new Gson();
		this.validator = new AlertValidator();
	}

	@Override
	public void execute(Tuple tuple) {
		try {
			Alert alert = gson.fromJson(tuple.getString(0), Alert.class);
			validator.validate(alert);
			collector.emit(tuple, new Values(alert.getId(), alert));
		} catch (Exception e) {
			StormContextUtil.emitErrorTuple(collector, tuple, AlertTranslatorBolt.class, tuple.getString(0),
					"Failed to parse alert json:", e);
		}
		collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(Constants.FIELD_ID, Constants.FIELD_ALERT));
		StormContextUtil.declareErrorStream(declarer);
	}

	/**
	 * @return the collector
	 */
	protected OutputCollector getCollector() {
		return collector;
	}

}
