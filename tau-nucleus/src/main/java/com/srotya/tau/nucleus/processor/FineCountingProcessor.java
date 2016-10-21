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
package com.srotya.tau.nucleus.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.GroupByHandler;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;
import com.srotya.tau.wraith.aggregations.MarkovianAggregationEngineImpl;
import com.srotya.tau.wraith.aggregators.AggregationRejectException;
import com.srotya.tau.wraith.aggregators.FineCountingAggregator;

/**
 * Fine counting processors integrates the {@link MarkovianAggregationEngineImpl} with {@link FineCountingAggregator} to Nucleus
 * 
 * @author ambudsharma
 */
public class FineCountingProcessor extends AbstractProcessor {

	private static final Logger logger = Logger.getLogger(FineCountingProcessor.class.getName());

	public FineCountingProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize,
			Map<String, String> conf, AbstractProcessor[] outputProcessors) {
		super(factory, parallelism, bufferSize, conf, outputProcessors);
	}

	@Override
	public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
			Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
		FineCountingHandler handler = new FineCountingHandler(this, taskId, parallelism, factory,
				Integer.parseInt(conf.getOrDefault("aggregation.batch.size", "1000")), getOutputProcessors()[0]);
		handler.init(conf);
		return handler;
	}

	@Override
	public String getConfigPrefix() {
		return "fcagg";
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @author ambudsharma
	 */
	public static class FineCountingHandler extends GroupByHandler {

		private AbstractProcessor outputProcessor;
		private AbstractProcessor caller;
		private MarkovianAggregationEngineImpl engine;
		private Gson gson;
		private List<String> batchEventIds;
		private int batchSize;

		public FineCountingHandler(AbstractProcessor caller, int taskId, MutableInt taskCount,
				DisruptorUnifiedFactory factory, int batchSize, AbstractProcessor outputProcessor) {
			super(taskId, taskCount);
			this.caller = caller;
			this.outputProcessor = outputProcessor;
			this.engine = new MarkovianAggregationEngineImpl(factory, factory,
					FineCountingAggregator.class.getCanonicalName());
			this.batchSize = batchSize;
			this.gson = new Gson();
			this.batchEventIds = new ArrayList<>();
		}

		/**
		 * @param conf
		 * @throws Exception
		 */
		public void init(Map<String, String> conf) throws Exception {
			this.engine.initialize(conf, getTaskId());
		}

		@Override
		public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type != null && type.equals(Constants.EVENT_TYPE_EMISSION)) {
				List<Event> events = new ArrayList<>();
				engine.emit((Integer) event.getHeaders().get(Constants.FIELD_AGGREGATION_WINDOW),
						event.getHeaders().get(Constants.FIELD_RULE_ACTION_ID).toString(), events);
				for (Event out : events) {
					out.getHeaders().put(Constants.FIELD_RULE_GROUP,
							event.getHeaders().get(Constants.FIELD_RULE_GROUP));
					out.setBody(gson.toJson(out.getHeaders()).getBytes());
					outputProcessor.processEventWaled(out);
					logger.info("Fine counting event forwarded:" + out);
				}
				if (!events.isEmpty()) {
					ackEventBatch();
				}
			} else {
				try {
					engine.aggregate((Long) event.getHeaders().get(Constants.FIELD_TIMESTAMP),
							(Integer) event.getHeaders().get(Constants.FIELD_AGGREGATION_WINDOW),
							event.getHeaders().get(Constants.FIELD_RULE_ACTION_ID).toString(),
							event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY).toString(),
							event.getHeaders().get(Constants.FIELD_AGGREGATION_VALUE));
				} catch (AggregationRejectException exception) {
					//
				}
				batchEventIds.add(event.getEventId());
				if (batchEventIds.size() >= batchSize) {
					engine.flush();
					ackEventBatch();
				}
			}
		}

		private void ackEventBatch() throws IOException {
			for (String id : batchEventIds) {
				caller.ackEvent(id);
			}
			if (batchEventIds.size() > 0) {
				logger.info("Acked batch:" + batchEventIds.size());
			}
			batchEventIds.clear();
		}

	}
}
