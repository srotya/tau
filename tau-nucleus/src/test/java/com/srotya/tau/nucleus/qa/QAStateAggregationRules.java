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
package com.srotya.tau.nucleus.qa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import com.srotya.tau.nucleus.Utils;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.StateAggregationAction;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.conditions.logical.AndCondition;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.RuleCommand;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.StatelessRulesEngine;

/**
 * @author ambudsharma
 */
public class QAStateAggregationRules {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(54321);

	@Test
	public void testAStateAggregations() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			IOException, InterruptedException {
		CloseableHttpClient client = null;
		// Create a template for alerting and upload it
		client = Utils.buildClient("http://localhost:8080/commands/templates", 2000, 2000);
		HttpPut templateUpload = new HttpPut("http://localhost:8080/commands/templates");
		String template = AlertTemplateSerializer.serialize(new AlertTemplate((short) 22, "test_template",
				"alertAggregation@srotya.com", "mail", "state tracking", "state tracking", 30, 1), false);
		templateUpload.addHeader("content-type", "application/json");
		templateUpload.setEntity(new StringEntity(new Gson().toJson(new TemplateCommand("all", false, template))));
		CloseableHttpResponse response = client.execute(templateUpload);
		response.close();
		assertTrue(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);

		// Create aggregation rule and upload it
		HttpPut ruleUpload = null;
		String rule = null;
		client = Utils.buildClient("http://localhost:8080/commands/rules", 2000, 2000);
		ruleUpload = new HttpPut("http://localhost:8080/commands/rules");
		ruleUpload.addHeader("content-type", "application/json");
		rule = RuleSerializer.serializeRuleToJSONString(
				new SimpleRule((short) 23, "SimpleStateTrackingRule", true, new EqualsCondition("value", 9.0),
						new Action[] {
								new StateAggregationAction((short) 0, "host", 10, new EqualsCondition("value", 9.0)) }),
				false);
		ruleUpload.setEntity(
				new StringEntity(new Gson().toJson(new RuleCommand(StatelessRulesEngine.ALL_RULEGROUP, false, rule))));
		response = client.execute(ruleUpload);
		response.close();
		assertTrue(response.getStatusLine().getReasonPhrase(),
				response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);

		// Create alert rule for aggregation rule's output and upload it
		client = Utils.buildClient("http://localhost:8080/commands/rules", 2000, 2000);
		ruleUpload = new HttpPut("http://localhost:8080/commands/rules");
		rule = RuleSerializer.serializeRuleToJSONString(new SimpleRule((short) 25, "SimpleStateAlertRule", true,
				new AndCondition(Arrays.asList(new EqualsCondition(Constants.FIELD_RULE_ID, 23),
						new EqualsCondition(Constants.FIELD_ACTION_ID, 0))),
				new Action[] { new TemplatedAlertAction((short) 0, (short) 22) }), false);
		ruleUpload.addHeader("content-type", "application/json");
		ruleUpload.setEntity(
				new StringEntity(new Gson().toJson(new RuleCommand(StatelessRulesEngine.ALL_RULEGROUP, false, rule))));
		response = client.execute(ruleUpload);
		response.close();
		assertTrue(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);

		for (int i = 0; i < 5; i++) {
			client = Utils.buildClient("http://localhost:8080/events", 2000, 2000);
			Map<String, Object> eventHeaders = new HashMap<>();
			eventHeaders.put("clientip", i);
			eventHeaders.put("host", "host1");
			eventHeaders.put("value", 9.0);
			eventHeaders.put("@timestamp", "2014-04-23T13:40:2" + i + ".000Z");
			eventHeaders.put(Constants.FIELD_EVENT_ID, 1200 + String.valueOf(i));

			HttpPost eventUpload = new HttpPost("http://localhost:8080/events");
			eventUpload.addHeader("content-type", "application/json");
			eventUpload.setEntity(new StringEntity(new Gson().toJson(eventHeaders)));
			response = client.execute(eventUpload);
			response.close();
			assertTrue(response.getStatusLine().getReasonPhrase(),
					response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);
		}
		int size = 0;
		while ((size = AllQATests.getSmtpServer().getReceivedEmailSize()) <= 4) {
			System.out.println("Waiting on aggregation window to close; email count:" + size);
			Thread.sleep(10000);
		}
		assertEquals(5, size);
	}
}
