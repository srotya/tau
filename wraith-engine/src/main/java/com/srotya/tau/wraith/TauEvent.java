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
package com.srotya.tau.wraith;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;

/**
 * Event implementation.
 * 
 * @author ambudsharma
 */
public class TauEvent implements Event {

	private static EthernetAddress RNG_ADDRESS;
	public static final int AVG_EVENT_FIELD_COUNT = Integer.parseInt(System.getProperty("event.field.count", "40"));
	private static final long serialVersionUID = 1L;
	private Long originEventId;
	private List<Long> sourceIds;
	private Long eventId;
	private Map<String, Object> headers;
	private byte[] body;
	
	static{
		try {
			RNG_ADDRESS = EthernetAddress.valueOf(Utils.selectDefaultIPAddress(false).getHardwareAddress());
		} catch (NumberFormatException | SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public TauEvent(String eventId) {
		this.eventId = MurmurHash.hash64(eventId);
		sourceIds = new ArrayList<>();
		headers = new HashMap<>(AVG_EVENT_FIELD_COUNT);
	}

	public TauEvent() {
		eventId = Generators.timeBasedGenerator(RNG_ADDRESS).generate().getMostSignificantBits();//UUID.randomUUID().getMostSignificantBits();
		sourceIds = new ArrayList<>();
		headers = new HashMap<>(AVG_EVENT_FIELD_COUNT);
	}

	TauEvent(Map<String, Object> headers) {
		eventId = Generators.timeBasedGenerator(RNG_ADDRESS).generate().getMostSignificantBits();//UUID.randomUUID().getMostSignificantBits();
		sourceIds = new ArrayList<>();
		this.headers = headers;
	}

	@Override
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Override
	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "TauEvent [eventid:" + eventId + ", sid:" + sourceIds + ",headers=" + headers.toString() + ", body="
				+ Arrays.toString(body) + "]";
	}

	@Override
	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public static Map<String, Object> getMapInstance() {
		return new ConcurrentHashMap<>(AVG_EVENT_FIELD_COUNT);
	}

	@Override
	public Long getEventId() {
		return eventId;
	}

	@Override
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	@Override
	public List<Long> getSourceIds() {
		return sourceIds;
	}

	@Override
	public void setSourceIds(List<Long> sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * @return the originEventId
	 */
	public Long getOriginEventId() {
		return originEventId;
	}

	/**
	 * @param originEventId
	 *            the originEventId to set
	 */
	public void setOriginEventId(Long originEventId) {
		this.originEventId = originEventId;
	}
}
