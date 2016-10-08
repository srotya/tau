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
import com.srotya.tau.wraith.aggregations.StateTrackingEngine;

public class StateProcessor extends AbstractProcessor {
	
	private static final Logger logger = Logger.getLogger(StateProcessor.class.getName());

	public StateProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize, Map<String, String> conf,
			AbstractProcessor[] outputProcessors) {
		super(factory, parallelism, bufferSize, conf, outputProcessors);
	}

	@Override
	public List<EventHandler<Event>> getInitializedHandlers(MutableInt parallelism, Map<String, String> conf,
			DisruptorUnifiedFactory factory) throws Exception {
		List<EventHandler<Event>> handlers = new ArrayList<>();
		for (int i = 0; i < parallelism.getVal(); i++) {
			StateTrackingEngineHandler handler = new StateTrackingEngineHandler(this, i, parallelism, factory,
					Integer.parseInt(conf.getOrDefault("state.batch.size", "1000")), getOutputProcessors()[0]);
			handler.init(conf);
			handlers.add(handler);
		}
		return handlers;
	}

	@Override
	public String getConfigPrefix() {
		return "state";
	}

	public static class StateTrackingEngineHandler extends GroupByHandler {

		private static final Logger logger = Logger.getLogger(StateTrackingEngine.class.getName());
		private StateTrackingEngine stateTrackingEngine;
		private int taskId;
		private StateProcessor caller;
		private AbstractProcessor outputProcessor;
		private Gson gson;
		private List<String> batchEventIds;
		private int batchSize;

		public StateTrackingEngineHandler(StateProcessor caller, int taskId, MutableInt taskCount,
				DisruptorUnifiedFactory factory, int batchSize, AbstractProcessor outputProcessor) {
			super(taskId, taskCount);
			this.caller = caller;
			this.taskId = taskId;
			this.batchSize = batchSize;
			this.outputProcessor = outputProcessor;
			this.stateTrackingEngine = new StateTrackingEngine(factory, factory);
			this.gson = new Gson();
			this.batchEventIds = new ArrayList<>();
		}

		public void init(Map<String, String> conf) throws Exception {
			stateTrackingEngine.initialize(conf, taskId);
		}

		@Override
		public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			// if this is emit event
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type!=null && type.equals(Constants.EVENT_TYPE_EMISSION)) {
				List<Event> events = new ArrayList<>();
				stateTrackingEngine.emit((Integer) event.getHeaders().get(Constants.FIELD_AGGREGATION_WINDOW),
						event.getHeaders().get(Constants.FIELD_RULE_ACTION_ID).toString(), events);
				for (Event out : events) {
					out.getHeaders().put(Constants.FIELD_RULE_GROUP, event.getHeaders().get(Constants.FIELD_RULE_GROUP));
					out.setBody(gson.toJson(out.getHeaders()).getBytes());
					outputProcessor.processEventWaled(out);
					logger.info("State tracking event forwarded:"+out);
				}
			} else {
				if ((Boolean) event.getHeaders().get(Constants.FIELD_STATE_TRACK)) {
					// if this is track
					stateTrackingEngine.track((Long) event.getHeaders().get(Constants.FIELD_TIMESTAMP),
							(Integer) event.getHeaders().get(Constants.FIELD_AGGREGATION_WINDOW),
							event.getHeaders().get(Constants.FIELD_RULE_ACTION_ID).toString(),
							event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY).toString());
					logger.info("Received tracking event"+event);
				} else {
					// if this is untrack
					stateTrackingEngine.untrack((Long) event.getHeaders().get(Constants.FIELD_TIMESTAMP),
							(Integer) event.getHeaders().get(Constants.FIELD_AGGREGATION_WINDOW),
							event.getHeaders().get(Constants.FIELD_RULE_ACTION_ID).toString(),
							event.getHeaders().get(Constants.FIELD_AGGREGATION_KEY).toString());
					logger.info("Received untracking event"+event);
				}
				batchEventIds.add(event.getEventId());
				if (batchEventIds.size() >= batchSize) {
					for (String id : batchEventIds) {
						caller.ackEvent(id);
					}
				}
			}
		}

	}
	
	@Override
	public Logger getLogger() {
		return logger;
	}
}
