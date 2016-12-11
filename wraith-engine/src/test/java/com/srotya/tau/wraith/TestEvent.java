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
package com.srotya.tau.wraith;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.srotya.tau.wraith.Event;

/**
 * Dummy event for unit testing
 * 
 * @author ambud_sharma
 */
public class TestEvent implements Event {
	
	private static final long serialVersionUID = 1L;
	private Map<String, Object> headers;
	private byte[] body;
	private Long eventId;

	public TestEvent() {
		this.headers = new HashMap<String, Object>();
	}

	public TestEvent(Map<String, Object> headers) {
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
	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	@Override
	public Long getEventId() {
		return eventId;
	}

	@Override
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TestEvent [headers=" + headers + ", body=" + Arrays.toString(body) + ", eventId=" + eventId + "]";
	}

	@Override
	public List<Long> getSourceIds() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSourceIds(List<Long> sourceIds) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Long getOriginEventId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setOriginEventId(Long eventId) {
		// TODO Auto-generated method stub
		
	}

}
