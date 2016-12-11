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
package com.srotya.tau.api;

import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.rules.RuleSerializer;

/**
 * Utilities for mapping, event checks etc.
 * 
 * @author ambud_sharma
 */
public class Utils {

	private Utils() {
	}

	@SuppressWarnings("unchecked")
	public static Event stringToEvent(String eventJson) {
		Event event = new WebEvent();
		Gson gson = new Gson();
		event.getHeaders()
				.putAll((Map<String, Object>) gson.fromJson(eventJson, new TypeToken<HashMap<String, Object>>() {
				}.getType()));
		return event;
	}

	public static boolean isCharsetMisInterpreted(String input, String encoding) {
		CharsetDecoder decoder = Charset.forName("ascii").newDecoder();
		CharsetEncoder encoder = Charset.forName(encoding).newEncoder();
		ByteBuffer tmp;
		try {
			tmp = encoder.encode(CharBuffer.wrap(input));
		} catch (CharacterCodingException e) {
			return false;
		}

		try {
			decoder.decode(tmp);
			return true;
		} catch (CharacterCodingException e) {
			return false;
		}
	}

	public static JsonObject buildEvent(String operation, String tenantId, String apiKey) {
		JsonObject event = new JsonObject();
		event.addProperty("apikey", apiKey);
		event.addProperty("tenant_id", tenantId);
		event.addProperty("@timestamp", System.currentTimeMillis());
		event.addProperty("operation", operation);
		return event;
	}

	public static class WebEvent implements Event {

		private static final long serialVersionUID = 1L;
		private Map<String, Object> headers;
		private Long eventId;

		public WebEvent() {
			headers = new HashMap<>();
		}

		@Override
		public Map<String, Object> getHeaders() {
			return headers;
		}

		@Override
		public void setHeaders(Map<String, Object> headers) {
			this.headers = headers;
		}

		@Override
		public byte[] getBody() {
			return null;
		}

		@Override
		public void setBody(byte[] body) {
		}

		@Override
		public Long getEventId() {
			return eventId;
		}

		@Override
		public void setEventId(Long eventId) {
			this.eventId = eventId;
		}

		@Override
		public List<Long> getSourceIds() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setSourceIds(List<Long> sourceIds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long getOriginEventId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setOriginEventId(Long eventId) {
			// TODO Auto-generated method stub
			
		}

	}

	public static String getPrettyRuleJson(String ruleJson) {
		return RuleSerializer.serializeRuleToJSONString(RuleSerializer.deserializeJSONStringToRule(ruleJson), true);
	}

	public static String getPrettyTemplateJson(String templateJson) {
		return AlertTemplateSerializer.serialize(AlertTemplateSerializer.deserialize(templateJson), true);
	}

	public static void createDatabase(String dbConnectionString, String dbName, String user, String pass, String driver)
			throws Exception {
		Connection conn = null;
		Statement stmt = null;
		try {
			// STEP 2: Register JDBC driver
			Class.forName(driver);

			// STEP 3: Open a connection
			System.out.println("Connecting to database with connection string:" + dbConnectionString);
			conn = DriverManager.getConnection(dbConnectionString, user, pass);

			// STEP 4: Execute a query
			System.out.println("Creating database...");
			stmt = conn.createStatement();

			String sql = "CREATE DATABASE IF NOT EXISTS " + dbName;
			stmt.executeUpdate(sql);
			System.out.println("Database created successfully...");
		} finally {
			// finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
			} // nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} // end finally try
		} // end try
	}

	public static String getHostName() {
		String value = "localhost";
		try {
			value = Inet4Address.getLocalHost().getHostName();
		} catch (Exception e) {
		}
		return value;
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

}