package com.srotya.tau.api.dao.alertreceiver;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;

import com.srotya.tau.api.DerbyUtil;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.storage.RuleGroup;

public class DatabaseResource extends ExternalResource {
	
	private static final String CONNECTION_STRING = "jdbc:derby:target/tau.db;create=true";
	private static final String TARGET_RULES_DB = "target/tau.db";
	private EntityManagerFactory emf;
	
	static {
		System.setProperty("org.jboss.logging.provider", "jdk");
		System.setProperty("derby.stream.error.field", DerbyUtil.class.getCanonicalName() + ".DEV_NULL");
		System.setProperty("local", "false");
	}
	
	@Override
	protected void before() throws Throwable {
		java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
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
		ruleGroup.setRuleGroupId("all");
		ruleGroup.setRuleGroupName("all");
		RuleGroupManager.getInstance().createRuleGroup(em, ruleGroup);
	}
	
	
	@Override
	protected void after() {
		emf.close();
	}

}
