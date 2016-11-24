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

import java.util.Map;
import java.util.logging.Logger;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.ShuffleHandler;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertEngine;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertEngineImpl;

/**
 * @author ambudsharma
 */
public class AlertingProcessor extends AbstractProcessor {

	private static final Logger logger = Logger.getLogger(AlertingProcessor.class.getName());

	public AlertingProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize, Map<String, String> conf,
			AbstractProcessor... outputProcessors) {
		super(factory, parallelism, bufferSize, conf, outputProcessors);
	}

	public static class AlertActionHandler extends ShuffleHandler {

		private TemplatedAlertEngine engine;
		private AbstractProcessor outputProcessor;
		private DisruptorUnifiedFactory factory;

		public AlertActionHandler(int taskId, MutableInt taskCount, DisruptorUnifiedFactory factory,
				AbstractProcessor outputProcessor) {
			super(taskId, taskCount);
			this.factory = factory;
			this.outputProcessor = outputProcessor;
			this.engine = new TemplatedAlertEngineImpl(factory);
		}

		public void init(Map<String, String> conf) throws Exception {
			engine.initialize(conf);
		}

		@Override
		public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type != null && type.equals(Constants.EVENT_TYPE_RULE_UPDATE)) {
				engine.updateTemplate(event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						event.getHeaders().get(Constants.FIELD_TEMPLATE_CONTENT).toString(),
						((Boolean) event.getHeaders().get(Constants.FIELD_TEMPLATE_DELETE)));
				logger.info(
						"Processed template update:" + event.getHeaders().get(Constants.FIELD_TEMPLATE_CONTENT).toString());
			} else {
				@SuppressWarnings("unchecked")
				Alert alert = engine.materialize((Map<String, Object>) event.getHeaders().get(Constants.FIELD_EVENT),
						event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						((Number) event.getHeaders().get(Constants.FIELD_RULE_ID)).shortValue(),
						((Number) event.getHeaders().get(Constants.FIELD_ACTION_ID)).shortValue(),
						event.getHeaders().get(Constants.FIELD_RULE_NAME).toString(),
						((Number) event.getHeaders().get(Constants.FIELD_ALERT_TEMPLATE_ID)).shortValue(),
						((Number) event.getHeaders().get(Constants.FIELD_TIMESTAMP)).longValue());
				if (alert != null) {
					Event outputEvent = factory.buildEvent();
					outputEvent.setEventId(event.getEventId());
					outputEvent.getHeaders().put(Constants.FIELD_ALERT, alert);
					outputEvent.getHeaders().put(Constants.FIELD_AGGREGATION_KEY, alert.getId());
					outputProcessor.processEventWaled(outputEvent);
					logger.info("Emitting alert to transmitter:"+outputEvent);
				}
			}
		}

	}

	@Override
	public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
			Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
		AlertActionHandler handler = new AlertActionHandler(taskId, parallelism, factory, getOutputProcessors()[0]);
		handler.init(conf);
		return handler;
	}

	@Override
	public String getConfigPrefix() {
		return "alerts";
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}
