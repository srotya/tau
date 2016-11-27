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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.logical.AndCondition;
import com.srotya.tau.wraith.conditions.logical.OrCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanCondition;
import com.srotya.tau.wraith.conditions.relational.GreaterThanEqualToCondition;
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanCondition;
import com.srotya.tau.wraith.conditions.relational.LessThanEqualToCondition;
import com.srotya.tau.wraith.rules.RuleSerializer;

/**
 * Helper utils
 * 
 * @author ambud_sharma
 */
public class Utils {

	public static final Map<Class<? extends Condition>, String> SIMPLE_CONDITIONS = new HashMap<>();
	public static final Map<Class<? extends Condition>, String> COMPLEX_CONDITIONS = new HashMap<>();

	private Utils() {
	}

	static {
		SIMPLE_CONDITIONS.put(EqualsCondition.class, "eq");
		SIMPLE_CONDITIONS.put(GreaterThanEqualToCondition.class, "gte");
		SIMPLE_CONDITIONS.put(LessThanEqualToCondition.class, "lte");
		SIMPLE_CONDITIONS.put(GreaterThanCondition.class, "gt");
		SIMPLE_CONDITIONS.put(LessThanCondition.class, "lt");
		SIMPLE_CONDITIONS.put(JavaRegexCondition.class, "matches");

		COMPLEX_CONDITIONS.put(AndCondition.class, "and");
		COMPLEX_CONDITIONS.put(OrCondition.class, "or");
	}

	@SuppressWarnings("unchecked")
	public static Event stringToEvent(String eventJson) {
		Event event = new UIEvent();
		Gson gson = new Gson();
		event.getHeaders()
				.putAll((Map<String, Object>) gson.fromJson(eventJson, new TypeToken<HashMap<String, Object>>() {
				}.getType()));
		return event;
	}
	
	public static boolean validateStatus(CloseableHttpResponse response) {
		return response.getStatusLine().getStatusCode()>=200 && response.getStatusLine().getStatusCode()<300;
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

	public static class UIEvent implements Event {

		private static final long serialVersionUID = 1L;
		private Map<String, Object> headers;

		public UIEvent() {
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
		public List<Long> getSourceIds() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setSourceIds(List<Long> sourceIds) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Long getEventId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setEventId(Long eventId) {
			// TODO Auto-generated method stub
			
		}

	}
	
	public static String getPrettyRuleJson(String ruleJson) {
		return RuleSerializer.serializeRuleToJSONString(
				RuleSerializer.deserializeJSONStringToRule(ruleJson), true);
	}

	public static void createDatabase(String dbConnectionString, String dbName, String user, String pass, String driver) throws Exception {
		Connection conn = null;
		Statement stmt = null;
		try {
			// STEP 2: Register JDBC driver
			Class.forName(driver);

			// STEP 3: Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(dbConnectionString, user, pass);

			// STEP 4: Execute a query
			System.out.println("Creating database...");
			stmt = conn.createStatement();

			String sql = "CREATE DATABASE IF NOT EXISTS "+dbName;
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
}