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
package com.srotya.tau.nucleus;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.srotya.tau.nucleus.disruptor.KryoByteEventTranslator;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TauEvent;

public class TestByteKryoEventTranslator {

	private static final String HOST = "host";
	private static final String VALUE = "value";
	private static final String TIMESTAMP = "timestamp";

	@Test
	public void testBaseSerDe() {
		KryoByteEventTranslator translator = new KryoByteEventTranslator();
		TauEvent event = new TauEvent();
		event.getHeaders().put(TIMESTAMP, System.currentTimeMillis());
		event.getHeaders().put(VALUE, 10);
		event.getHeaders().put(HOST, "localhost");

		Kryo kryo = new Kryo();
		kryo.register(TauEvent.class);

		byte[] buffer = new byte[100];
		ByteBufferOutput output = new ByteBufferOutput(ByteBuffer.wrap(buffer));
		kryo.writeObject(output, event);
		output.close();

		assertNotNull(buffer);
		assertTrue(buffer.length > 0);

		Event outputEvent = new TauEvent();
		translator.translateTo(outputEvent, 0, buffer);

		for (String key : new String[] { TIMESTAMP, VALUE, HOST }) {
			assertEquals(event.getHeaders().get(key), outputEvent.getHeaders().get(key));
		}

	}

}
