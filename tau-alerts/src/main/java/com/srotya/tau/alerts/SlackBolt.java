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

import java.io.Serializable;
import java.util.Map;

import com.google.gson.Gson;
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
 * Adding slack message capabilities for alert delivery
 * 
 * @author ambud_sharma
 */
public class SlackBolt extends BaseRichBolt {

	public static final String SLACK_URL = "https://hooks.slack.com/services/";
	public static final String SLACK_EMOJI = ":ghost:";
	public static final String SLACK_USERNAME = "hendrix";
	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;
	private transient HttpService httpService;
	private transient Gson gson;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.httpService = new HttpService();
		this.gson = new Gson();
	}

	@Override
	public void execute(Tuple tuple) {
		Alert alert = (Alert) tuple.getValueByField(Constants.FIELD_ALERT);
		String[] split = alert.getTarget().split("@");
		String json = gson.toJson(new SlackMessage("#"+split[1], SLACK_USERNAME, alert.getBody(), SLACK_EMOJI));
		try {
			httpService.sendHttpCallback(SLACK_URL + split[0], json);
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

	public static class SlackMessage implements Serializable {

		// {"channel": "#hendrix-slack-test", "username": "hendrix", "text":
		// "This is posted to channel comes from a bot named webhookbot.",
		// "icon_emoji": ":ghost:"}
		private static final long serialVersionUID = 1L;
		private String channel;
		private String username;
		private String text;
		private String icon_emoji;

		public SlackMessage() {
		}

		public SlackMessage(String channel, String username, String text, String icon_emoji) {
			this.channel = channel;
			this.username = username;
			this.text = text;
			this.icon_emoji = icon_emoji;
		}

		/**
		 * @return the channel
		 */
		public String getChannel() {
			return channel;
		}

		/**
		 * @param channel
		 *            the channel to set
		 */
		public void setChannel(String channel) {
			this.channel = channel;
		}

		/**
		 * @return the username
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * @param username
		 *            the username to set
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * @return the text
		 */
		public String getText() {
			return text;
		}

		/**
		 * @param text
		 *            the text to set
		 */
		public void setText(String text) {
			this.text = text;
		}

		/**
		 * @return the icon_emoji
		 */
		public String getIcon_emoji() {
			return icon_emoji;
		}

		/**
		 * @param icon_emoji
		 *            the icon_emoji to set
		 */
		public void setIcon_emoji(String icon_emoji) {
			this.icon_emoji = icon_emoji;
		}

	}

}
