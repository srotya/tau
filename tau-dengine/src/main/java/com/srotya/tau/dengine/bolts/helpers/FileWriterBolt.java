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
package com.srotya.tau.dengine.bolts.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Logger;

import com.srotya.tau.dengine.Constants;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * Bolt for testing and writing results to a file
 * 
 * @author ambud_sharma
 */
public class FileWriterBolt extends BaseRichBolt {

	private static final Logger logger = Logger.getLogger(FileWriterBolt.class.getName());
	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;
	private transient PrintWriter pr;
	private String field;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		String fileName = stormConf.getOrDefault("filewriter.filename", "/tmp/alerts.txt").toString();
		try {
			this.pr = new PrintWriter(new FileOutputStream(new File(fileName), true));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		field = stormConf.getOrDefault("filewriter.fieldname", Constants.FIELD_ALERT).toString();
		logger.info("File Writer Bolt initialized");
	}

	@Override
	public void execute(Tuple input) {
		pr.println(input.getStringByField(field));
		pr.flush();
		collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}

}
