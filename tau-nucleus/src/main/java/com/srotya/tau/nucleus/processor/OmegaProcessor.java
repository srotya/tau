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

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.GroupByHandler;
import com.srotya.tau.omega.executors.OmegaScriptExecutor;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;

/**
 * @author ambudsharma
 */
public class OmegaProcessor extends AbstractProcessor {

	private static final Logger logger = Logger.getLogger(OmegaProcessor.class.getName());

	public OmegaProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize, Map<String, String> conf,
			AbstractProcessor[] outputProcessors) {
		super(factory, parallelism, bufferSize, conf, outputProcessors);
	}

	@Override
	public List<EventHandler<Event>> getInitializedHandlers(MutableInt parallelism, Map<String, String> conf,
			DisruptorUnifiedFactory factory) throws Exception {
		List<EventHandler<Event>> handlers = new ArrayList<>();
		for (int i = 0; i < parallelism.getVal(); i++) {
			OmegaHandler handler = new OmegaHandler(this, i, parallelism, factory);
			handler.init(conf);
			handlers.add(handler);
		}
		return handlers;
	}

	@Override
	public String getConfigPrefix() {
		return "omega";
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	/**
	 * @author ambudsharma
	 */
	public static class OmegaHandler extends GroupByHandler {

		private OmegaScriptExecutor executor;
		private AbstractProcessor caller;

		public OmegaHandler(AbstractProcessor caller, int taskId, MutableInt taskCount, DisruptorUnifiedFactory factory) {
			super(taskId, taskCount);
			this.caller = caller;
			executor = new OmegaScriptExecutor(factory);
		}
		
		public void init(Map<String, String> conf) throws Exception {
			executor.initialize(conf);
		}

		@Override
		public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type != null && type.equals(Constants.EVENT_TYPE_RULE_UPDATE)) {
				executor.updateRule(event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						event.getHeaders().get(Constants.FIELD_RULE_CONTENT).toString(),
						((Boolean) event.getHeaders().get(Constants.FIELD_RULE_DELETE)));
				logger.info("Processed rule update:" + event.getHeaders().get(Constants.FIELD_RULE_CONTENT).toString());
			} else {
				// logger.info("Saw event:"+event);
				executor.executeScript(event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						(Short) event.getHeaders().get(Constants.FIELD_RULE_ID),
						(Short) event.getHeaders().get(Constants.FIELD_ACTION_ID), (Event) event.getHeaders().get(Constants.FIELD_EVENT));
				caller.ackEvent(event.getEventId());
			}
		}

	}

}
