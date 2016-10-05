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
package com.srotya.tau.ui;

import java.util.AbstractMap;
import java.util.Map.Entry;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * @author ambud_sharma
 */
public class BapiLoginDAO {
	
	public static final String HMAC = "HMAC";
	public static final String X_SUBJECT_TOKEN = "X-Subject-Token";
	private static final String PASSWORD = "password";
	private static final String USERNAME = "username";

	public static Entry<String, String> authenticate(String authURL, String username, String password)
			throws Exception {
		CloseableHttpClient client = Utils.buildClient(authURL, 3000, 5000);
		HttpPost authRequest = new HttpPost(authURL);
		Gson gson = new Gson();
		JsonObject obj = new JsonObject();
		obj.addProperty(USERNAME, username);
		obj.addProperty(PASSWORD, password);
		StringEntity entity = new StringEntity(gson.toJson(obj), ContentType.APPLICATION_JSON);
		authRequest.setEntity(entity);
		CloseableHttpResponse response = client.execute(authRequest);
		if (response.getStatusLine().getStatusCode() == 200) {
			String tokenPair = EntityUtils.toString(response.getEntity());
			JsonArray ary = gson.fromJson(tokenPair, JsonArray.class);
			obj = ary.get(0).getAsJsonObject();
			String token = obj.get(X_SUBJECT_TOKEN).getAsString();
			String hmac = obj.get(HMAC).getAsString();
			return new AbstractMap.SimpleEntry<String, String>(token, hmac);
		} else {
			System.err.println("Login failed:"+response.getStatusLine().getStatusCode()+"\t"+response.getStatusLine().getReasonPhrase());
			return null;
		}
	}

}
