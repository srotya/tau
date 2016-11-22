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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.UnifiedFactory;

import oi.thekraken.grok.api.exception.GrokException;

/**
 * @author ambudsharma
 */
public class Normalizer {

	public static void main(String[] args) throws GrokException, IOException, NoSuchAlgorithmException, InterruptedException {
		File[] files = new File(args[0]).listFiles();
		new File("target/data").mkdirs();

		final AtomicInteger fileNumber = new AtomicInteger(0);

		ExecutorService es = Executors.newCachedThreadPool();

		for (File file : files) {
			es.submit(() -> {
				try {
					GrokEventTranslator translator = new GrokEventTranslator(new UnifiedFactory());
					Kryo kryo = new Kryo();
					int c = 0;
					Output fio = new Output(
							new FileOutputStream("target/data/f" + fileNumber.incrementAndGet() + ".kryo"));
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String temp = null;
					while ((temp = reader.readLine()) != null) {
						Event event = translator.translateTo(temp);
						kryo.writeObject(fio, event);
						c++;
						if (c % 400000 == 0) {
							fio.close();
							System.out.println("Emitted:400K Events");
							fio = new Output(
									new FileOutputStream("target/data/f" + fileNumber.incrementAndGet() + ".kryo"));
						}
					}
					reader.close();
					fio.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		}
		es.shutdown();
		es.awaitTermination(1000, TimeUnit.SECONDS);
	}

}
