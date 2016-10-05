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
package com.srotya.tau.dengine;

import com.srotya.tau.dengine.bolts.ErrorBolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Helper for {@link ErrorBolt} registration and erro event sourcing
 * 
 * @author ambud_sharma
 */
public class StormContextUtil {

	/**
	 * Abstracts sending error messages to {@link ErrorBolt}
	 * @param collector
	 * @param srcTuple
	 * @param currentBolt
	 * @param source
	 * @param message
	 * @param e
	 */
	public static void emitErrorTuple(OutputCollector collector, Tuple srcTuple,
			Class<? extends BaseRichBolt> currentBolt, String source, String message, Throwable e) {
		collector.emit(Constants.ERROR_STREAM, srcTuple, buildErrorTuple(currentBolt, source, message, e));
	}

	/**
	 * Build an error tuple from a list
	 * 
	 * @param currentBolt
	 * @param source
	 * @param message
	 * @param e
	 * @return errorTuple
	 */
	public static Values buildErrorTuple(Class<? extends BaseRichBolt> currentBolt, String source, String message,
			Throwable e) {
		if (e != null) {
			return new Values(System.currentTimeMillis(), currentBolt.getCanonicalName(), source, message, e.getClass().getCanonicalName(),
					e.getMessage());
		} else {
			return new Values(System.currentTimeMillis(), currentBolt.getCanonicalName(), source, message, "na", "na");
		}
	}

	/**
	 * Abstracts declaration of error stream to send errors to {@link ErrorBolt}
	 * @param outputFieldsDeclarer
	 */
	public static void declareErrorStream(OutputFieldsDeclarer outputFieldsDeclarer) {
		outputFieldsDeclarer.declareStream(Constants.ERROR_STREAM,
				new Fields(Constants.ERROR_TIMESTAMP, Constants.ERROR_SOURCE_BOLT, Constants.ERROR_SOURCE,
						Constants.ERROR_MESSAGE, Constants.ERROR_EXCEPTION,
						Constants.ERROR_EXCEPTION_MESSAGE));
	}
	
}
