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

import com.srotya.tau.alerts.media.HttpService;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.Alert;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * @author ambud_sharma
 */
public class HttpBolt extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;
	private transient HttpService httpService;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.httpService = new HttpService();
	}

	@Override
	public void execute(Tuple tuple) {
		Alert alert = (Alert) tuple.getValueByField(Constants.FIELD_ALERT);
		try {
			httpService.sendHttpCallback(alert);
		} catch (AlertDeliveryException e) {
			StormContextUtil.emitErrorTuple(collector, tuple, HttpBolt.class, alert.toString(),
					"Failed to make http callback", e);
		}
		collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		StormContextUtil.declareErrorStream(declarer);
	}

	/**
	 * @return the httpService
	 */
	protected HttpService getHttpService() {
		return httpService;
	}

	/**
	 * @param httpService the httpService to set
	 */
	protected void setHttpService(HttpService httpService) {
		this.httpService = httpService;
	}

}
