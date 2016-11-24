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
package com.srotya.tau.nucleus.processor.alerts;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.srotya.tau.nucleus.Utils;
import com.srotya.tau.wraith.actions.alerts.Alert;

/**
 * @author ambud_sharma
 */
public class HttpService {

	private AlertDeliveryException exception = new AlertDeliveryException("Non 200 status code returned");

	/**
	 * @param alert
	 * @throws AlertDeliveryException
	 */
	public void sendHttpCallback(Alert alert) throws AlertDeliveryException {
		try {
			sendHttpCallback(alert.getTarget(), alert.getBody());
		} catch (AlertDeliveryException e) {
			throw exception;
		}
	}
	
	/**
	 * @param destination
	 * @param bodyContent
	 * @throws AlertDeliveryException
	 */
	public void sendHttpCallback(String destination, String bodyContent) throws AlertDeliveryException {
		try {
			CloseableHttpClient client = Utils.buildClient(destination, 3000, 3000);
			HttpPost request = new HttpPost(destination);
			StringEntity body = new StringEntity(bodyContent, ContentType.APPLICATION_JSON);
			request.addHeader("content-type", "application/json");
			request.setEntity(body);
			HttpResponse response = client.execute(request);
			EntityUtils.consume(response.getEntity());
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode < 200 && statusCode >= 300) {
				throw exception;
			}
			client.close();
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException |AlertDeliveryException e) {
			throw exception;
		}
	}

}
