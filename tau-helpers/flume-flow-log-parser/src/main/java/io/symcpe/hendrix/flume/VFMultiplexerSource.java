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
package io.symcpe.hendrix.flume;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flume.ChannelSelector;
import org.apache.flume.Event;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.kafka.KafkaSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.srotya.tau.interceptors.InterceptException;

import io.symcpe.hendrix.interceptor.aws.VPCFlowLogParser;

/**
 * This is an Apache Flume interceptor to parse AWS VPC Flow Logs sent to Lambda
 * from Cloud Watch.
 * 
 * This parser amplifies the events by breaking them down from single multi-line
 * event to multiple events i.e. 1 event per Flow Record
 * 
 * @author ambud_sharma
 */
public class VFMultiplexerSource extends KafkaSource {

	@Override
	public synchronized ChannelProcessor getChannelProcessor() {
		return new MultiplexingProcessor(super.getChannelProcessor());
	}

	public static class MultiplexingProcessor extends ChannelProcessor {

		private static final Logger logger = LoggerFactory.getLogger(MultiplexingProcessor.class);;
		private VPCFlowLogParser parser = new VPCFlowLogParser();
		private ChannelProcessor processor;

		public MultiplexingProcessor(ChannelSelector selector) {
			super(selector);
		}

		public MultiplexingProcessor(ChannelProcessor processor) {
			super(null);
			this.processor = processor;
		}

		@Override
		public void processEvent(Event event) {
			if(event==null) {
				return;
			}
			try {
				if (event.getBody() != null) {
					List<Map<String, Object>> events = parser.parseFlowLogMap(new String(event.getBody()));
					for (Map<String, Object> map : events) {
						event = new SimpleEvent();
						for (Entry<String, Object> entry : map.entrySet()) {
							event.getHeaders().put(entry.getKey(), entry.getValue().toString());
						}
						processor.processEvent(event);
					}
				}
			} catch (InterceptException e) {
				logger.error("\nFailed to parse event", e);
			}
		}

		@Override
		public void processEventBatch(List<Event> events) {
			for (Event event : events) {
				processEvent(event);
			}
		}

	}

}
