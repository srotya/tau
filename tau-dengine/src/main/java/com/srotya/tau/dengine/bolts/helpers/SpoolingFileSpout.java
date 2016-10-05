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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;

import backtype.storm.spout.ISpout;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * {@link ISpout} to spool a file and keeping tailing it for changes, any time
 * new data is read at the end of the file, events are emitted. A Thread.sleep()
 * is added to slow down the poll frequency for detecting changes.
 * 
 * @author ambud_sharma
 */
public class SpoolingFileSpout extends BaseRichSpout {

	public static final String LINE = "line";
	private static final long serialVersionUID = 1L;
	private transient SpoutOutputCollector collector;
	private transient File sourceFile;
	private transient BufferedReader reader;
	private transient String temp;
	private String filePath;
	
	public SpoolingFileSpout(String filePath) {
		this.filePath = filePath.replaceFirst("^~", System.getProperty("user.home"));
	}

	@SuppressWarnings({ "rawtypes", "resource" })
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
		this.sourceFile = new File(filePath);
		if (conf.get("spooling.auto.truncate") != null) {
			FileChannel outChan = null;
			try {
				outChan = new FileOutputStream(this.sourceFile, true).getChannel();
				outChan.truncate(0);
			} catch (Exception e) {

			} finally {
				try {
					outChan.close();
				} catch (IOException e) {
				}
			}
		}
		if (!this.sourceFile.exists()) {
			try {
				this.sourceFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void nextTuple() {
		if (reader == null) {
			try {
				reader = new BufferedReader(new FileReader(sourceFile));
			} catch (FileNotFoundException e) {
				collector.reportError(e);
			}
		}
		try {
			if ((temp = reader.readLine()) != null) {
				System.err.println("Reading rule update:" + temp);
				collector.emit(new Values(temp));
			} else {
				Thread.sleep(2000);
			}
		} catch (IOException | InterruptedException e) {
			collector.reportError(e);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(LINE));
	}

	@Override
	public void close() {
		super.close();
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
