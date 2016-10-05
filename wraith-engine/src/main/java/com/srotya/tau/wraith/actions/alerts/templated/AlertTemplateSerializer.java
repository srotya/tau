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
package com.srotya.tau.wraith.actions.alerts.templated;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author ambud_sharma
 */
public class AlertTemplateSerializer {

	private AlertTemplateSerializer() {
	}

	/**
	 * @param template
	 * @return
	 */
	public static String serialize(AlertTemplate template, boolean pretty) {
		GsonBuilder builder = new GsonBuilder();
		if (pretty) {
			builder.setPrettyPrinting();
		}
		return builder.create().toJson(template);
	}

	/**
	 * @param json
	 * @return
	 */
	public static AlertTemplate deserialize(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AlertTemplate.class);
	}

	/**
	 * @param template
	 * @return
	 */
	public static String serialize(AlertTemplate[] template) {
		Gson gson = new Gson();
		return gson.toJson(template);
	}

	public static String serialize(List<AlertTemplate> template, boolean pretty) {
		GsonBuilder builder = new GsonBuilder();
		if (pretty) {
			builder.setPrettyPrinting();
		}
		return builder.create().toJson(template);
	}

	/**
	 * @param json
	 * @return
	 */
	public static AlertTemplate[] deserializeArray(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, AlertTemplate[].class);
	}

	public static void main(String[] args) {
		System.out.println(AlertTemplateSerializer.serialize(new AlertTemplate[] {
				new AlertTemplate((short) 1, "test name1", "test@gmail.com", "http", "test", "$host", 120, 1),
				new AlertTemplate((short) 2, "test name1", "test@gmail.com", "http", "test", "$host", 120, 1) }));
	}
}
