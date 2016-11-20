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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
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
import com.srotya.tau.api.storage.RuleGroup;

/**
 * Unit test for {@link RuleGroupManager}
 * 
 * @author ambud_sharma
 */
@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRuleGroupManager {

	private static final String TENANT_ID = "32342342342";
	private static final String CONNECTION_STRING = "jdbc:hsqldb:mem:target/rules.db";
	private static final String TARGET_RULES_DB = "target/rg.db";
	private static EntityManagerFactory emf;
	private EntityManager em;
	@Mock
	private KafkaProducer<String, String> producer;
	@Mock
	private ApplicationManager am;

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
	}

	@Before
	public void before() {
		em = emf.createEntityManager();
		when(am.getEM()).thenReturn(em);

		when(producer.send(any())).thenReturn(
				CompletableFuture.completedFuture(new RecordMetadata(new TopicPartition("ruleTopic", 2), 1, 1)));
	}

	@Test
	public void testCreateTenant() throws Exception {
		RuleGroup tenant = new RuleGroup();
		tenant.setRuleGroupId(TENANT_ID);
		tenant.setRuleGroupName("simple-tenant");
		RuleGroupManager.getInstance().createRuleGroup(em, tenant);
		tenant = RuleGroupManager.getInstance().getRuleGroup(em, TENANT_ID);
		assertEquals(TENANT_ID, tenant.getRuleGroupId());
	}

	@Test
	public void testCreateTenantNegative() throws Exception {
		RuleGroup tenant = new RuleGroup();
		tenant.setRuleGroupId("32342342342234514322534t34352345234523452342344573657657486784768567956785678234234234");
		tenant.setRuleGroupName("simple-tenant");
		try {
			RuleGroupManager.getInstance().createRuleGroup(em, tenant);
			fail("Can't create this tenant");
		} catch (Exception e) {
		}
	}

	@Test
	public void testCupdateTenant() throws Exception {
		RuleGroupManager.getInstance().updateRuleGroup(em, TENANT_ID, "simple-tenant2");
		RuleGroup tenant = RuleGroupManager.getInstance().getRuleGroup(em, TENANT_ID);
		assertEquals(TENANT_ID, tenant.getRuleGroupId());
		assertEquals("simple-tenant2", tenant.getRuleGroupName());
	}

	@Test
	public void testDeleteTenant() throws Exception {
		RuleGroupManager.getInstance().deleteRuleGroup(em, TENANT_ID, am);
		verify(producer, times(0)).send(any());
	}

}
