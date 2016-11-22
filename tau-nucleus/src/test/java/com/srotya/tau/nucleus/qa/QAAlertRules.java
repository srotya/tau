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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import com.google.gson.Gson;
import com.srotya.tau.nucleus.Utils;
import com.srotya.tau.nucleus.processor.alerts.MailService;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.RuleCommand;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.StatelessRulesEngine;

/**
 * @author ambud
 */
public class QAAlertRules {

	@Test
	public void testSMTPServerAvailability() throws UnknownHostException, IOException, MessagingException {
		MimeMessage msg = new MimeMessage(Session.getDefaultInstance(new Properties()));
		msg.setFrom(new InternetAddress("alert@srotya.com"));
		msg.setRecipient(RecipientType.TO, new InternetAddress("alert@srotya.com"));
		msg.setSubject("test mail");
		msg.setContent("Hello", "text/html");
		Transport.send(msg);
		System.err.println("Mail sent");
		MailService ms = new MailService();
		ms.init(new HashMap<>());
		Alert alert = new Alert();
		alert.setBody("test");
		alert.setId((short) 0);
		alert.setMedia("test");
		alert.setSubject("test");
		alert.setTarget("alert@srotya.com");
		ms.sendMail(alert);
		assertEquals(2, AllQATests.getSmtpServer().getReceivedEmailSize());
	}

	@Test
	public void testAlertRules() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException,
			ClientProtocolException, IOException, InterruptedException {
		CloseableHttpClient client = null;
		client = Utils.buildClient("http://localhost:8080/commands/templates", 2000, 2000);
		HttpPut templateUpload = new HttpPut("http://localhost:8080/commands/templates");
		String template = AlertTemplateSerializer.serialize(
				new AlertTemplate((short) 1, "test_template", "alert@srotya.com", "mail", "test", "test", 30, 1),
				false);
		templateUpload.addHeader("content-type", "application/json");
		templateUpload.setEntity(new StringEntity(new Gson().toJson(new TemplateCommand("all", false, template))));
		CloseableHttpResponse response = client.execute(templateUpload);
		response.close();
		assertTrue(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);

		client = Utils.buildClient("http://localhost:8080/commands/rules", 2000, 2000);
		HttpPut ruleUpload = new HttpPut("http://localhost:8080/commands/rules");
		String rule = RuleSerializer.serializeRuleToJSONString(new SimpleRule((short) 2, "SimpleRule", true,
				new EqualsCondition("value", 1.0), new Action[] { new TemplatedAlertAction((short) 0, (short) 1) }),
				false);
		ruleUpload.addHeader("content-type", "application/json");
		ruleUpload.setEntity(
				new StringEntity(new Gson().toJson(new RuleCommand(StatelessRulesEngine.ALL_RULEGROUP, false, rule))));
		response = client.execute(ruleUpload);
		response.close();
		assertTrue(response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);

		client = Utils.buildClient("http://localhost:8080/events", 2000, 2000);
		Map<String, Object> eventHeaders = new HashMap<>();
		eventHeaders.put("value", 1);
		eventHeaders.put("@timestamp", "2014-04-23T13:40:29.000Z");
		eventHeaders.put(Constants.FIELD_EVENT_ID, "1122");

		HttpPost eventUpload = new HttpPost("http://localhost:8080/events");
		eventUpload.addHeader("content-type", "application/json");
		eventUpload.setEntity(new StringEntity(new Gson().toJson(eventHeaders)));
		response = client.execute(eventUpload);
		response.close();
		assertTrue(response.getStatusLine().getReasonPhrase(),
				response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300);
		int size = 0;
		while((size = AllQATests.getSmtpServer().getReceivedEmailSize())<=2) {
			Thread.sleep(100);
		}
		assertEquals(3, size);
	}

}
