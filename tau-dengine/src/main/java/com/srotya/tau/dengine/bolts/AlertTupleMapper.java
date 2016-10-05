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

import com.srotya.tau.dengine.Constants;

import backtype.storm.tuple.Tuple;
import storm.kafka.bolt.mapper.TupleToKafkaMapper;

/**
 * @author ambud_sharma
 */
public class AlertTupleMapper implements TupleToKafkaMapper<String, String> {

	private static final long serialVersionUID = 1L;

	@Override
	public String getKeyFromTuple(Tuple tuple) {
		return null;
	}

	@Override
	public String getMessageFromTuple(Tuple tuple) {
		return tuple.getStringByField(Constants.FIELD_ALERT);
	}

}
