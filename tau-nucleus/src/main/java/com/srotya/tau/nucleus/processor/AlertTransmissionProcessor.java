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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.disruptor.GroupByHandler;
import com.srotya.tau.nucleus.processor.alerts.HttpService;
import com.srotya.tau.nucleus.processor.alerts.MailService;
import com.srotya.tau.nucleus.processor.alerts.SlackService;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.MutableInt;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * @author ambudsharma
 */
public class AlertTransmissionProcessor extends AbstractProcessor {

	private static final Logger logger = Logger.getLogger(AlertTransmissionProcessor.class.getName());
	private ScheduledExecutorService es;

	public AlertTransmissionProcessor(DisruptorUnifiedFactory factory, int parallelism, int bufferSize,
			Map<String, String> conf, AbstractProcessor[] outputProcessors) {
		super(factory, parallelism, bufferSize, conf, outputProcessors);
		es = Executors.newScheduledThreadPool(1, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		es.scheduleAtFixedRate(() -> {
			Event event = factory.buildEvent();
			event.getHeaders().put(Constants.FIELD_EVENT_TYPE, "-1");
			processEventNonWaled(event);
		}, 10, 1, TimeUnit.SECONDS);
	}

	@Override
	public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
			Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
		TransmissionHandler handler = new TransmissionHandler(this, taskId, parallelism, factory);
		handler.init(conf);
		return handler;
	}

	@Override
	public String getConfigPrefix() {
		return "trans";
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	public static class TransmissionHandler extends GroupByHandler {

		private AbstractProcessor caller;
		private transient Map<Short, AlertTemplate> templateMap;
		private transient Map<Short, MutableInt> counter;
		private transient Map<Short, MutableBoolean> stateMap;
		private transient long globalCounter = 1;
		private DisruptorUnifiedFactory storeFactory;
		private MailService mailService;
		private HttpService httpService;
		private SlackService slackService;

		public TransmissionHandler(AbstractProcessor caller, int taskId, MutableInt taskCount, DisruptorUnifiedFactory factory) {
			super(taskId, taskCount);
			this.caller = caller;
			this.templateMap = new HashMap<>();
			this.counter = new HashMap<>();
			this.stateMap = new HashMap<>();
			this.storeFactory = factory;
			this.mailService = new MailService();
			this.httpService = new HttpService();
			this.slackService = new SlackService();
		}

		public void init(Map<String, String> conf) throws Exception {
			TemplateStore store = null;
			try {
				store = storeFactory.getTemplateStore(conf.get(Constants.TSTORE_TYPE), conf);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw e;
			}
			try {
				store.connect();
				Map<Short, AlertTemplate> temp = store.getAllTemplates();
				if (temp != null) {
					this.templateMap.putAll(temp);
				}
				logger.info("Fetched " + templateMap.size() + " alert templates from the store");
				store.disconnect();
			} catch (IOException e) {
				throw e;
			}
			mailService.init(conf);
		}
		
		@Override
		public void consumeEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type != null && type.equals(Constants.EVENT_TYPE_RULE_UPDATE)) {
				updateTemplate(event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						event.getHeaders().get(Constants.FIELD_TEMPLATE_CONTENT).toString(),
						((Boolean) event.getHeaders().get(Constants.FIELD_TEMPLATE_DELETE)));
				logger.info(
						"Processed template update:" + event.getHeaders().get(Constants.FIELD_TEMPLATE_CONTENT).toString());
			} else if (event.getHeaders().containsKey(Constants.FIELD_ALERT)) {
				Alert alert = (Alert) event.getHeaders().get(Constants.FIELD_ALERT);
				AlertTemplate template = templateMap.get(alert.getId());
				if (template != null) {
					MutableInt result = counter.get(alert.getId());
					if (result == null) {
						result = new MutableInt();
						counter.put(alert.getId(), result);
						stateMap.put(alert.getId(), new MutableBoolean());
					}
					if (result.incrementAndGet() <= template.getThrottleLimit() || template.getThrottleLimit() == 0) {
						switch (alert.getMedia()) {
						case "mail":// send email
							logger.info("Sending email out:"+alert);
							mailService.sendMail(alert);
							break;
						case "http":// make http request"
							logger.info("Sending http request out:"+alert);
							httpService.sendHttpCallback(alert);
							break;
						case "slack":
							logger.info("Sending slack request out:"+alert);
							slackService.sendHttpCallback(alert);
							break;
						default:// do nothing
							break;
						}
					} else {
						// else just drop the alert and notify suppression
						// monitor
						MutableBoolean state = stateMap.get(alert.getId());
						if (!state.isVal()) {
							logger.info("Entering suppression state for template:" + alert.getId());
							state.setVal(true);
						}
						logger.fine("Suppression alert for:" + alert.getId() + ":\t" + alert);
					}
				} else {
					logger.severe("Suppression policy not found for templateid:" + alert.getId());
				}
				caller.ackEvent(event.getEventId());
			} else {
				globalCounter++;
				logger.fine("Received tick tuple, gc:" + globalCounter);
				for (Entry<Short, AlertTemplate> entry : templateMap.entrySet()) {
					if (globalCounter % entry.getValue().getThrottleDuration() == 0
							&& counter.containsKey(entry.getKey())) {
						counter.get(entry.getKey()).setVal(0);
						MutableBoolean res = stateMap.get(entry.getKey());
						if (res.isVal()) {
							logger.fine("Leaving suppression state for template:" + entry.getKey());
							res.setVal(false);
						}
						logger.fine("Resetting suppression counters for:" + entry.getKey());
					}
				}
			}
		}

		/**
		 * Update templates
		 * 
		 * @param ruleGroup
		 * @param templateJson
		 * @param delete
		 */
		public void updateTemplate(String ruleGroup, String templateJson, boolean delete) {
			try {
				AlertTemplate template = AlertTemplateSerializer.deserialize(templateJson);
				if (delete) {
					templateMap.remove(template.getTemplateId());
					logger.info("Deleted template:" + template.getTemplateId());
				} else {
					templateMap.put(template.getTemplateId(), template);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Alert template error", e);
			}
		}

	}
}
