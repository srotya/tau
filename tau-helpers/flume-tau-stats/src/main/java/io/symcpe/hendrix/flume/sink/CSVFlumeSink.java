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
package io.symcpe.hendrix.flume.sink;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 */
public class CSVFlumeSink extends AbstractSink implements Configurable {

	private Gson gson;
	private Map<String, PrintWriter> outputCache;
	private File directory;

	public Status process() throws EventDeliveryException {
		Status status = null;
		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		System.out.println("Starting transaction");
		Event event = null;
		try {
			txn.begin();
			// This try clause includes whatever Channel operations you want to
			// do
			for (int i = 0; i < 10; i++) {
				event = ch.take();
				if (event == null) {
					continue;
				}
				String timestamp = event.getHeaders().get("timestamp");
				String body = new String(event.getBody(), "utf-8");
				body = body.substring(body.indexOf('{'));
				// Send the Event to the external repository.
				JsonElement b = gson.fromJson(body, JsonElement.class);
				JsonObject obj = b.getAsJsonObject();
				String fileName = obj.get("name").getAsString();
				PrintWriter pr = outputCache.get(fileName);
				if (pr == null) {
					pr = new PrintWriter(new File(directory, fileName + ".csv"));
					outputCache.put(fileName, pr);
					pr.println("timestamp, value");
					pr.flush();
				}
				pr.println(timestamp + "," + obj.get("value").getAsNumber());
				pr.flush();
			}
			txn.commit();
			txn.close();
			status = Status.READY;
		} catch (Throwable t) {
			try {
				String body = new String(event.getBody());
				body = body.substring(body.indexOf('{'));
				System.err.println(body);
			} catch (Exception e) {
			}

			t.printStackTrace(System.out);
			txn.rollback();
			txn.close();
			// Log exception, handle individual exceptions as needed
			status = Status.BACKOFF;
			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error) t;
			}
		}
		return status;
	}

	public void configure(Context context) {
		String dir = context.getString("dir");
		directory = new File(dir);
		if (!directory.exists()) {
			directory.mkdirs();
		}
		gson = new Gson();
		outputCache = new HashMap<String, PrintWriter>();
	}

	@Override
	public synchronized void stop() {
		for (Entry<String, PrintWriter> entry : outputCache.entrySet()) {
			entry.getValue().close();
		}
		super.stop();
	}

}
