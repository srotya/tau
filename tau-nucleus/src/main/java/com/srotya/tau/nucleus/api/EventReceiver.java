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
package com.srotya.tau.nucleus.api;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.ingress.IngressManager.PushIngresser;
import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.PerformantException;

/**
 * HTTP Based event receiver, this is an Ingresser however it's not managed by
 * IngressManager since it's follows the push model.
 * 
 * @author ambudsharma
 */
@Path("/events")
public class EventReceiver extends PushIngresser {

	private static final Logger logger = Logger.getLogger(EventReceiver.class.getName());
	private static final BadRequestException BAD_EVENT_EXCEPTION = new BadRequestException("Event ingestion failed");
	private static final PerformantException MISSING_EVENT_ID_EXCEPTION = new PerformantException("Event ID missing");
	private static final PerformantException MISSING_EVENT_TIMESTAMP_EXCEPTION = new PerformantException(
			"Event timestamp missing");
	private Gson gson;
	private Type singleType;
	private static AtomicInteger counter = new AtomicInteger(0);

	static {
		// BGTaskManager.getInstance().schedule(() -> {
		// MetricsSink.getInstance().publishIntMetric("ingress.http.eps",
		// counter.get());
		// logger.info("ingress.http.eps:" + counter.getAndSet(0));
		// }, 0, 1, TimeUnit.SECONDS);
	}

	/**
	 * @param factory
	 * @param nextProcessor
	 */
	public EventReceiver(DisruptorUnifiedFactory factory, AbstractProcessor nextProcessor) {
		super(factory, nextProcessor);
		this.gson = new Gson();
		this.singleType = new TypeToken<HashMap<String, Object>>() {
		}.getType();
	}

	/**
	 * To receive a single event
	 * 
	 * @param eventId
	 * @param eventJson
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	public void receiveEvent(String eventJson) {
		try {
			Map<String, Object> headers = (Map<String, Object>) gson.fromJson(eventJson, singleType);
			DateInterceptor intercept = new DateInterceptor();
			intercept.configure(new HashMap<>());
			intercept.validate(headers);
			validateEvent(headers);
			Event event = getFactory().buildEvent(headers);
			event.setEventId(headers.get(Constants.FIELD_EVENT_ID).toString());
			event.setBody(eventJson.getBytes());
			getProcessor().processEventWaled(event);
			counter.incrementAndGet();
		} catch (Exception e) {
			logger.severe("Event rejected:" + e.getMessage());
			throw BAD_EVENT_EXCEPTION;
		}
	}

	protected void validateEvent(Map<String, Object> headers) throws PerformantException {
		if (headers.get(Constants.FIELD_EVENT_ID) == null) {
			throw MISSING_EVENT_ID_EXCEPTION;
		}
		if (headers.get(Constants.FIELD_TIMESTAMP) == null) {
			throw MISSING_EVENT_TIMESTAMP_EXCEPTION;
		}
	}

}