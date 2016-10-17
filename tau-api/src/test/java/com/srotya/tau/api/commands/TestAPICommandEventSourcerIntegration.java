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
package com.srotya.tau.api.commands;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.dao.alertreceiver.DatabaseResource;
import com.srotya.tau.nucleus.NucleusConfig;
import com.srotya.tau.nucleus.NucleusServer;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;

import io.dropwizard.testing.junit.DropwizardAppRule;

/**
 * Integration tests for {@link APICommandEventSourcer}
 * 
 * @author ambudsharma
 */
@RunWith(MockitoJUnitRunner.class)
public class TestAPICommandEventSourcerIntegration {
	
	@ClassRule
	public static DatabaseResource resource = new DatabaseResource();
	@ClassRule
	public static DropwizardAppRule<NucleusConfig> nucleus = new DropwizardAppRule<>(NucleusServer.class,
			"src/test/resources/nucleus.yml");
	private APICommandEventSourcer sourcer;

	@Mock
	private ApplicationManager am;
	

	@Before
	public void setup() throws Exception {
		sourcer = new APICommandEventSourcer();
		sourcer.setApplicationManager(am);
		when(am.getConfig()).thenReturn(new Properties());
		sourcer.init();
	}

	@Test
	public void testRuleCommandSourcing() throws IOException {
		Rule rule = new SimpleRule((short) 1, "Test", true, new EqualsCondition("host", "host"),
				new Action[] { new TemplatedAlertAction((short) 1, (short) 2) });
		sourcer.sendRule(false, "all", RuleSerializer.serializeRuleToJSONString(rule, false));
	}

	@Test
	public void testTemplateCommandSourcing() throws IOException {
		AlertTemplate template = new AlertTemplate();
		template.setBody("test");
		template.setDestination("test@srotya.com");
		template.setMedia("mail");
		template.setSubject("test");
		template.setTemplateId((short) 1);
		template.setTemplateName("test");
		template.setThrottleDuration(200);
		template.setThrottleLimit(1);
		sourcer.sendTemplate(false, "all", AlertTemplateSerializer.serialize(template, false));
	}
}
