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

import java.util.Arrays;
import java.util.Map;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.Alert;

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
public class AlertRouterBolt extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple tuple) {
		Alert alert = (Alert) tuple.getValueByField(Constants.FIELD_ALERT);
		collector.emit(alert.getMedia(), tuple, new Values(alert));
		collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		for (String media : Arrays.asList("mail", "http", "slack")) {
			declarer.declareStream(media, new Fields(Constants.FIELD_ALERT));
		}
	}

}
