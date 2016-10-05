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
package com.srotya.tau.nucleus.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.srotya.tau.wraith.Event;

/**
 * @author ambudsharma
 *
 */
public class CopyTranslator implements EventTranslatorOneArg<Event, Event>{

	@Override
	public void translateTo(Event outputEvent, long sequence, Event inputEvent) {
		outputEvent.getHeaders().clear();
		outputEvent.setEventId(inputEvent.getEventId());
		outputEvent.getHeaders().putAll(inputEvent.getHeaders());
		outputEvent.setBody(inputEvent.getBody());
	}

}
