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
package com.srotya.tau.alerts.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import com.google.gson.Gson;
import com.srotya.tau.wraith.actions.alerts.Alert;

/**
 * To generate some test data
 * 
 * @author ambud_sharma
 */
public class DummyAlertGenerator {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Gson gson = new Gson();
		PrintWriter pr = new PrintWriter(
				new GZIPOutputStream(
						new FileOutputStream(new File("target/alerts.gz")
								)
						));
		for(int i=0;i<10;i++) {
			Alert alert = new Alert();
			alert.setId((short)2);
			alert.setBody("test2");
			alert.setMedia("mail");
			alert.setSubject("hellow world");
			alert.setTarget("test@zyx.com");
			alert.setRuleGroup("34234234");
			alert.setTimestamp(System.currentTimeMillis());
			pr.println(gson.toJson(alert));
		}
		pr.close();
	}
	
}
