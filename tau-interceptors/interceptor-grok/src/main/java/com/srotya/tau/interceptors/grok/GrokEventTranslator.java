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
package com.srotya.tau.interceptors.grok;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;

import net.iharder.Base64;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;

/**
 * @author ambudsharma
 *
 */
public class GrokEventTranslator {

	private Grok grok;
	private EventFactory factory;
	private MessageDigest md5;

	public GrokEventTranslator(EventFactory factory) throws GrokException, NoSuchAlgorithmException {
		this.factory = factory;
		this.grok = new Grok();
		this.grok.addPatternFromReader(
				new InputStreamReader(ClassLoader.getSystemResourceAsStream("grok"), Charset.forName("utf-8")));
		this.md5 = MessageDigest.getInstance("md5");
		grok.compile("%{COMMONAPACHELOG}");
	}

	public Event translateTo(String data) {
		Event event = factory.buildEvent();
		Match match = grok.match(data);
		match.captures();
		for (Entry<String, Object> entry : match.toMap().entrySet()) {
			if (entry.getValue() != null) {
				event.getHeaders().put(entry.getKey(), entry.getValue());
			}
		}
		event.setBody(data.getBytes());
		event.setEventId(Base64.encodeBytes(md5.digest(data.getBytes())));
		return event;
	}

}
