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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

/**
 * Spout to read log files and stream them downstream. This is a replacement for
 * KafkaSpout and to be used for local testing purposes only.
 * 
 * @author ambud_sharma
 */
public class FileLogReaderSpout extends BaseRichSpout {

	public static final String OFFSET = "offset";
	public static final String MESSAGE = "message";
	public static final String LOG_DIR = "log.dir";
	private static final long serialVersionUID = 1L;
	private static final String SLOW = "log.slow";
	private transient SpoutOutputCollector collector;
	private transient long eventCounter;
	private transient int i = 0;
	private transient File[] listFiles;
	private transient BufferedReader reader;
	private transient int slow;
	private String location;
	
	public FileLogReaderSpout(String logDirectory) {
		this.location = logDirectory.replaceFirst("^~",System.getProperty("user.home"));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector;
		if (conf.get(SLOW) != null) {
			slow = Integer.parseInt(conf.get(SLOW).toString());
			System.err.println("Setting slow log to:"+slow);
		}else {
			slow = -1;
		}
		this.listFiles = new File(location).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".gz");
			}
		});
		System.err.println("File location:" + location);
		if (this.listFiles == null) {
			throw new RuntimeException("Log location not initialized:" + location);
		}
		Arrays.sort(listFiles);
	}

	@Override
	public void nextTuple() {
		if (i < listFiles.length) {
			if (reader == null) {
				try {
					reader = new BufferedReader(
							new InputStreamReader(new GZIPInputStream(new FileInputStream(listFiles[i]))));
					System.err.println("Reading logs from:" + listFiles[i]);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				String line = reader.readLine();
				if (line == null) {
					reader.close();
					reader = null;
					i++;
				} else {
					eventCounter++;
					collector.emit(new Values(line, String.valueOf(eventCounter)));
					if (eventCounter % 100000 == 0) {
						System.out.println(new Date() + "\t" + eventCounter);
					}
					if (slow > -1) {
						Thread.sleep(slow);
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields(MESSAGE, OFFSET));
	}

}
