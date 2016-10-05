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
package com.srotya.tau.dengine;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.rules.Rule;

import backtype.storm.tuple.Tuple;

/**
 * Set of utilities for Storm
 * 
 * @author ambud_sharma
 */
public final class Utils extends com.srotya.tau.wraith.Utils {

	private Utils() {
	}

	/**
	 * Checks if the supplied tuple is a {@link Rule} sync i.e. update tuple
	 * 
	 * @param tuple
	 * @return true if it's a {@link Rule} sync tuple
	 */
	public static boolean isRuleSyncTuple(Tuple tuple) {
		return tuple.getSourceStreamId().equals(Constants.SYNC_STREAM_ID)
				&& tuple.getSourceComponent().equals(Constants.RULE_SYNC_COMPONENT);
	}
	
	/**
	 * Checks if the supplied tuple is a {@link TemplatedAlertAction} sync i.e.
	 * update tuple
	 * 
	 * @param tuple
	 * @return true if it's a {@link TemplatedAlertAction} sync tuple
	 */
	public static boolean isTemplateSyncTuple(Tuple tuple) {
		return tuple.getSourceStreamId().equals(Constants.SYNC_STREAM_ID)
				&& tuple.getSourceComponent().equals(Constants.TEMPLATE_SYNC_COMPONENT);
	}
	
	/**
	 * @param tuple
	 * @return
	 */
	public static boolean isWraithTickTuple(Tuple tuple) {
		return tuple.getSourceStreamId().equals(Constants.TICK_STREAM_ID);
	}

	/**
	 * Check if the supplied tuple is a tick {@link Tuple}
	 * 
	 * @param tuple
	 * @return true if it's a tick tuple
	 */
	public static boolean isTickTuple(Tuple tuple) {
		return tuple.getSourceComponent().equals(backtype.storm.Constants.SYSTEM_COMPONENT_ID)
				&& tuple.getSourceStreamId().equals(backtype.storm.Constants.SYSTEM_TICK_STREAM_ID);
	}
	
	/**
	 * @param tuple
	 * @return
	 */
	public static boolean isStateTrackingTuple(Tuple tuple) {
		return tuple.getSourceStreamId().equals(Constants.STATE_STREAM_ID);
	}

	/**
	 * Normalize and format Zookeeper connection string
	 * 
	 * @param zkHosts
	 * @param zkPort
	 * @return zookeeper connection string
	 */
	public static String getZkConnectionString(String zkHosts, int zkPort) {
		String[] hosts = zkHosts.split(",");
		StringBuilder builder = new StringBuilder();
		Arrays.asList(hosts).stream().map(host -> {
			if (host.contains(":")) {
				return host.substring(0, host.indexOf(":"));
			} else {
				return host;
			}
		}).map(host -> host + ":" + zkPort).forEach(host -> builder.append(host));
		return builder.toString();
	}

	/**
	 * Build a {@link CloseableHttpClient}
	 * 
	 * @param baseURL
	 * @param connectTimeout
	 * @param requestTimeout
	 * @return http client
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 */
	public static CloseableHttpClient buildClient(String baseURL, int connectTimeout, int requestTimeout)
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		HttpClientBuilder clientBuilder = HttpClients.custom();
		RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
				.setConnectionRequestTimeout(requestTimeout).build();

		return clientBuilder.setDefaultRequestConfig(config).build();
	}

	/**
	 * Get client
	 * 
	 * @param baseURL
	 * @param connectTimeout
	 * @param requestTimeout
	 * @return client
	 */
	public static CloseableHttpClient getClient(String baseURL, int connectTimeout, int requestTimeout) {
		try {
			return buildClient(baseURL, connectTimeout, requestTimeout);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			return null;
		}
	}

//	/**
//	 * Combine rule action id for bucketing
//	 * 
//	 * @param ruleId
//	 * @param actionId
//	 * @return ruleActionId
//	 */
//	public static String combineRuleActionId(short ruleId, short actionId) {
//		return ruleId + "_" + actionId;
//	}

}