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

import java.io.IOException;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.google.gson.JsonObject;
import com.srotya.tau.dengine.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Bolt to handle error message for all topology components
 * 
 * @author ambud_sharma
 */
public class ErrorBolt extends BaseRichBolt {

	public static final String FILE_LOG_LOCATION = "/tmp/hendrix_errors.log";
	private static final String DEFAULT_ERROR_DEBUG = "false";
	private static final String CONF_ERROR_BOLT_DEBUG = "error.bolt.debug";
	private static final long serialVersionUID = 1L;
	private transient Logger logger;
	private transient OutputCollector collector;
	private transient boolean debug;

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		Object val = stormConf.get(CONF_ERROR_BOLT_DEBUG);
		if (val == null) {
			val = DEFAULT_ERROR_DEBUG;
		}
		this.debug = Boolean.parseBoolean(val.toString());
		if (this.debug) {
			this.logger = Logger.getLogger(ErrorBolt.class.getCanonicalName());
			try {
				FileHandler fh = new FileHandler(FILE_LOG_LOCATION);
				this.logger.addHandler(fh);
				fh.setFormatter(new SimpleFormatter());
			} catch (SecurityException | IOException e) {
				logger.log(Level.SEVERE, "Error initializing file handler for ErrorBolt debug logging", e);
			}
		}
	}

	@Override
	public void execute(Tuple input) {
		String errorSourceBolt = input.getStringByField(Constants.ERROR_SOURCE_BOLT);
		JsonObject object = new JsonObject();
		object.addProperty(Constants.ERROR_TIMESTAMP, input.getLongByField(Constants.ERROR_TIMESTAMP));
		object.addProperty(Constants.ERROR_MESSAGE, input.getStringByField(Constants.ERROR_MESSAGE));
		object.addProperty(Constants.ERROR_EXCEPTION, input.getStringByField(Constants.ERROR_EXCEPTION));
		object.addProperty(Constants.ERROR_SOURCE, input.getStringByField(Constants.ERROR_SOURCE));
		object.addProperty(Constants.ERROR_SOURCE_BOLT, errorSourceBolt);
		object.addProperty(Constants.ERROR_EXCEPTION_MESSAGE,
				input.getStringByField(Constants.ERROR_EXCEPTION_MESSAGE));
		if (debug) {
			logger.info(object.toString());
		}
		collector.emit(Constants.KAFKA_ERROR_STREAM, input, new Values(errorSourceBolt, object.toString()));
		collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream(Constants.KAFKA_ERROR_STREAM,
				new Fields(Constants.FIELD_ERROR_KEY, Constants.FIELD_ERROR_VALUE));
	}

}
