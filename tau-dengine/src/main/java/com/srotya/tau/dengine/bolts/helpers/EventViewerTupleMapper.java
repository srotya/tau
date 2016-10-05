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
package com.srotya.tau.dengine.bolts.helpers;

import com.google.gson.Gson;
import com.srotya.tau.dengine.TauEvent;
import com.srotya.tau.wraith.Constants;

import backtype.storm.tuple.Tuple;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;

/**
 * @author ambudsharma
 *
 */
public class EventViewerTupleMapper implements TupleToKafkaMapper<String, String> {

	private static final long serialVersionUID = 1L;
	private transient Gson gson;

	public EventViewerTupleMapper() {
	}

	@Override
	public String getKeyFromTuple(Tuple tuple) {
		if (gson == null) {
			gson = new Gson();
		}
		Short ruleId = tuple.getShortByField(Constants.FIELD_RULE_ID);
		return String.valueOf(ruleId);
	}

	@Override
	public String getMessageFromTuple(Tuple tuple) {
		if (gson == null) {
			gson = new Gson();
		}
		TauEvent event = (TauEvent) tuple.getValueByField(Constants.FIELD_EVENT);
		return gson.toJson(event.getHeaders());
	}

}