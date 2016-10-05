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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.dengine.Utils;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * Ability to monitor suppression states for templates in real-time i.e. if a
 * template is being suppressed then suppression state will be true else it will
 * be false.
 * 
 * @author ambud_sharma
 */
public class SuppressionMonitorBolt extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private static final String UI_ENDPOINT = "ui.endpoint.av";
	private transient OutputCollector collector;
	private transient String uiEndpoint;
	private transient CloseableHttpClient client;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		if (stormConf.get(UI_ENDPOINT) != null) {
			this.uiEndpoint = stormConf.get(UI_ENDPOINT).toString();
		} else {
			this.uiEndpoint = "http://localhost:8080/ROOT/api/suppression/";
		}
	}

	@Override
	public void execute(Tuple tuple) {
		try {
			client = Utils.buildClient(this.uiEndpoint, 3000, 3000);
			HttpPut put = new HttpPut(this.uiEndpoint + "/" + tuple.getShortByField(Constants.FIELD_ALERT_TEMPLATE_ID)
					+ "/" + tuple.getBooleanByField(Constants.SUPRESSION_STATE));
			client.execute(put);
			client.close();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			collector.reportError(e);
		}
		collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		StormContextUtil.declareErrorStream(declarer);
	}

}
