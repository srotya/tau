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

import static org.junit.Assert.assertEquals;

import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.UnifiedFactory;

import oi.thekraken.grok.api.exception.GrokException;

public class TestGrokEventTranslator {

	@Test
	public void testApacheHTTPMessage() throws GrokException, NoSuchAlgorithmException {
		GrokEventTranslator translator = new GrokEventTranslator(new UnifiedFactory());
		String msg = "abc.xyz.com - - [28/Aug/1995:00:00:40 -0400] \"GET /pub/eurocent/tiny2.gif HTTP/1.0\" 200 1152";
		Event event = translator.translateTo(msg);
		assertEquals("abc.xyz.com", event.getHeaders().get("clientip").toString());
	}
	
}
