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
package com.srotya.tau.ui.alerts;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.srotya.tau.ui.ApplicationManager;
import com.srotya.tau.ui.Utils;

/**
 * Proxy to backend for alert receiver functionality
 * 
 * @author ambud_sharma
 */
public class AlertReceiver implements Serializable {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AlertReceiver.class.getName());
	private static AlertReceiver instance = new AlertReceiver();
	private ApplicationManager am;

	private AlertReceiver() {
	}

	public static AlertReceiver getInstance() {
		return instance;
	}

	public void addChannel(short ruleId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpPost post = new HttpPost(am.getBaseUrl() + "/receive/open/" + ruleId);
		client.execute(post);
		logger.info("Requested channel open");
	}

	/**
	 * @param ruleId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Queue<Map<String, Object>> getChannel(short ruleId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpGet get = new HttpGet(am.getBaseUrl() + "/receive/events/" + ruleId);
		CloseableHttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() < 300) {
			String result = EntityUtils.toString(response.getEntity());
			Gson gson = new Gson();
			Queue<Map<String, Object>> queue = gson.fromJson(result, Queue.class);
			return queue;
		}
		return new LinkedList<>();
	}

	/**
	 * @param ruleId
	 */
	public void closeChannel(short ruleId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpPost post = new HttpPost(am.getBaseUrl() + "/receive/close/" + ruleId);
		client.execute(post);
		logger.info("Requested channel closed");
	}

	/**
	 * @return the am
	 */
	public ApplicationManager getAm() {
		return am;
	}

	/**
	 * @param am
	 *            the am to set
	 */
	public void setAm(ApplicationManager am) {
		this.am = am;
	}

}