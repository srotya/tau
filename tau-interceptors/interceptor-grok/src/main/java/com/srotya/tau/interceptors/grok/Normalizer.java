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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.UnifiedFactory;

import oi.thekraken.grok.api.exception.GrokException;

/**
 * @author ambudsharma
 */
public class Normalizer {

	public static void main(String[] args) throws GrokException, IOException, NoSuchAlgorithmException {
		File[] files = new File(args[0]).listFiles();
		new File("target/data").mkdirs();
		GrokEventTranslator translator = new GrokEventTranslator(new UnifiedFactory());
		Kryo kryo = new Kryo();
		BufferedReader reader = null;
		int i = 0 ;
		int c = 0;
		Output fio = new Output(new FileOutputStream("target/data/f1.kryo"));
		for(File file:files) {
			 reader = new BufferedReader(new FileReader(file));
			 String temp = null;
			 while((temp=reader.readLine())!=null) {
				 Event event = translator.translateTo(temp);
				 kryo.writeObject(fio, event);
				 c++;
				 if(c%400000==0) {
					 fio.close();
					 i++;
					 fio = new Output(new FileOutputStream("target/data/f"+i+".kryo"));
				 }
			 }
			 reader.close();
		}
		if(reader!=null) {
			reader.close();
			fio.close();
		}
	}

}
