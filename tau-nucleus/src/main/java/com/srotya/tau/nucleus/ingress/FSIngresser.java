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
package com.srotya.tau.nucleus.ingress;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.srotya.tau.nucleus.ingress.IngressManager.PullIngresser;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TauEvent;

/**
 * @author ambudsharma
 *
 */
public class FSIngresser implements PullIngresser {

	private Input reader;
	private File[] files;
	private int i;
	private Kryo kryo;

	public FSIngresser(File[] files) throws IOException {
		this.files = files;
		this.i = 0;
		this.reader = new Input(new BufferedInputStream(new FileInputStream(files[i])));
		this.kryo = new Kryo();
	}

	@Override
	public Event produce() throws IOException {
		TauEvent event = (TauEvent) kryo.readObject(reader, TauEvent.class);
		if (reader.eof()) {
			reader.close();
			i++;
			if (i < files.length) {
				reader = new Input(new FileInputStream(files[i]));
			} else {
				reader = null;
				return null;
			}
		}
		return event;
	}

	@Override
	public void ack(Event event) {

	}

}
