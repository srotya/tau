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
package com.srotya.tau.api.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.DerbyUtil;
import com.srotya.tau.api.commands.KafkaCommandEventSourcer;
import com.srotya.tau.api.dao.RulesManager;
import com.srotya.tau.api.dao.TemplateManager;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.storage.AlertTemplates;
import com.srotya.tau.api.storage.Rules;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;
import com.srotya.tau.wraith.conditions.relational.EqualsCondition;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.validator.ValidationException;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTemplateManager {

	private static final String TEST_RULE_GROUP = "test-tenant";
	private static final String RULE_GROUP_ID_1 = "z341mmd3ifaasdjm23midijjiro";
	private static final String CONNECTION_STRING = "jdbc:hsqldb:mem:target/rules.db";
	// private static final String CONNECTION_NC_STRING =
	// "jdbc:derby:target/rules.db;";
	private static final String TARGET_RULES_DB = "target/template.db";
	private static EntityManagerFactory emf;
	private EntityManager em;
	@Mock
	private KafkaProducer<String, String> producer;
	@Mock
	private ApplicationManager am;
	private KafkaCommandEventSourcer kafkaCommandSourcer = new KafkaCommandEventSourcer();
	private static short id;
	private RuleGroup ruleGroup;

	static {
		System.setProperty("org.jboss.logging.provider", "jdk");
		System.setProperty("derby.stream.error.field", DerbyUtil.class.getCanonicalName() + ".DEV_NULL");
		System.setProperty("local", "false");
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		Properties config = new Properties(System.getProperties());
		File db = new File(TARGET_RULES_DB);
		if (db.exists()) {
			FileUtils.deleteDirectory(db);
		}
		config.setProperty("javax.persistence.jdbc.url", CONNECTION_STRING);
		try {
			emf = Persistence.createEntityManagerFactory("tau", config);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		EntityManager em = emf.createEntityManager();

		RuleGroup ruleGroup = new RuleGroup();
		ruleGroup.setRuleGroupId(RULE_GROUP_ID_1);
		ruleGroup.setRuleGroupName(TEST_RULE_GROUP);
		RuleGroupManager.getInstance().createRuleGroup(em, ruleGroup);

		em.close();
	}

	@Before
	public void before() {
		em = emf.createEntityManager();
		when(am.getEM()).thenReturn(em);
		when(am.getSourcer()).thenReturn(kafkaCommandSourcer);
		kafkaCommandSourcer.setProducer(producer);
		kafkaCommandSourcer.setRuleTopicName("ruleTopic");
		kafkaCommandSourcer.setTemplateTopicName("templateTopic");
		when(producer.send(any())).thenReturn(
				CompletableFuture.completedFuture(new RecordMetadata(new TopicPartition("templateTopic", 2), 1, 1)));
	}

	@After
	public void after() {
		em.close();
	}

	@Test
	public void testGetTemplate() throws Exception {
		AlertTemplates templates = new AlertTemplates();
		ruleGroup = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
		id = TemplateManager.getInstance().createNewTemplate(em, templates, ruleGroup);
		AlertTemplates template = TemplateManager.getInstance().getTemplate(em, ruleGroup.getRuleGroupId(), id);
		assertEquals(id, template.getTemplateId());
	}

	@Test
	public void testSaveTemplate() throws Exception {
		AlertTemplate tpl = new AlertTemplate();
		ruleGroup = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
		AlertTemplates template = TemplateManager.getInstance().getTemplate(em, ruleGroup.getRuleGroupId(), id);
		tpl.setTemplateId(template.getTemplateId());
		tpl.setBody("test");
		tpl.setDestination("test@xyz.com");
		tpl.setMedia("mail");
		tpl.setTemplateName("Test");
		tpl.setThrottleDuration(2);
		tpl.setThrottleLimit(2);
		short id = TemplateManager.getInstance().saveTemplate(em, template, template.getRuleGroup(), tpl, am);
		assertEquals(id, template.getTemplateId());
	}

	@Test
	public void testZDeleteTemplate() throws Exception {
		TemplateManager.getInstance().deleteTemplate(em, RULE_GROUP_ID_1, id, am);
		try {
			TemplateManager.getInstance().getTemplate(em, ruleGroup.getRuleGroupId(), id);
			fail("Not reachable");
		} catch (Exception e) {
		}
		TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
	}

	@Test
	public void testBadTemplate() throws Exception {
		AlertTemplate tpl = new AlertTemplate();
		RuleGroup tenant = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
		short id = TemplateManager.getInstance().createNewTemplate(em, new AlertTemplates(), tenant);
		AlertTemplates template = TemplateManager.getInstance().getTemplate(em, tenant.getRuleGroupId(), id);
		tpl.setTemplateId(template.getTemplateId());
		tpl.setBody("test");
		tpl.setDestination("test");
		tpl.setMedia("test");
		tpl.setTemplateName("Test");
		tpl.setThrottleDuration(2);
		tpl.setThrottleLimit(2);
		try {
			id = TemplateManager.getInstance().saveTemplate(em, template, template.getRuleGroup(), tpl, am);
			fail("Not reachable, bad template must be validated");
		} catch (ValidationException e) {
		}
	}

	@Test
	public void testZZDeleteteAllTemplatesBadRequest() throws Exception {
		AlertTemplate tpl = new AlertTemplate();
		RuleGroup tenant = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
		AlertTemplates templates = new AlertTemplates();
		short id = TemplateManager.getInstance().createNewTemplate(em, templates, tenant);
		tpl.setTemplateId(id);
		tpl.setBody("test");
		tpl.setDestination("test@xyz.com");
		tpl.setMedia("mail");
		tpl.setTemplateName("Test");
		tpl.setThrottleDuration(2);
		tpl.setThrottleLimit(2);
		id = TemplateManager.getInstance().saveTemplate(em, templates, templates.getRuleGroup(), tpl, am);
		assertEquals(id, templates.getTemplateId());
		Rule rul = new SimpleRule((short) 0, "simple-rule2", true, new EqualsCondition("host", "symcpe2"),
				new Action[] { new TemplatedAlertAction((short) 0, id) });
		RulesManager.getInstance().saveRule(em, new Rules(), tenant, rul, am);
		try {
			TemplateManager.getInstance().deleteTemplates(em, templates.getRuleGroup(), am);
			fail("Can't reach here this request should fail");
		} catch (Exception e) {
		}
		tenant = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
	}

	@Test
	public void testZZ2DeleteteAllTemplates() throws Exception {
		AlertTemplate tpl = new AlertTemplate();
		RuleGroup tenant = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
		AlertTemplates templates = new AlertTemplates();
		short id = TemplateManager.getInstance().createNewTemplate(em, templates, tenant);
		tpl.setTemplateId(id);
		tpl.setBody("test");
		tpl.setDestination("test@xyz.com");
		tpl.setMedia("mail");
		tpl.setTemplateName("Test");
		tpl.setThrottleDuration(2);
		tpl.setThrottleLimit(2);
		id = TemplateManager.getInstance().saveTemplate(em, templates, templates.getRuleGroup(), tpl, am);
		assertEquals(id, templates.getTemplateId());
		TemplateManager.getInstance().deleteTemplates(em, templates.getRuleGroup(), am);
		try {
			List<AlertTemplates> results = TemplateManager.getInstance().getTemplates(em,
					templates.getRuleGroup().getRuleGroupId());
			assertEquals(0, results.size());
		} catch (Exception e) {
		}
		tenant = TemplateManager.getInstance().getRuleGroup(em, RULE_GROUP_ID_1);
	}
}
