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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.dumbster.smtp.SimpleSmtpServer;
import com.srotya.tau.nucleus.NucleusConfig;
import com.srotya.tau.nucleus.NucleusServer;

import io.dropwizard.testing.junit.DropwizardAppRule;

@RunWith(Suite.class)
@SuiteClasses({ TestAlertRules.class, TestCountingAggregationRules.class, TestOmegaRules.class,
		TestStateAggregationRules.class, TestMultiStageAggregationRules.class })
public class AllQATests {

	@ClassRule
	public static DropwizardAppRule<NucleusConfig> nucleus = new DropwizardAppRule<>(NucleusServer.class,
			"src/test/resources/nucleus.yml");
	
	public static SimpleSmtpServer smtpServer;
	
	@BeforeClass
	public static void beforeClass() {
		smtpServer = SimpleSmtpServer.start();
	}
	
	@AfterClass
	public static void afterClass() {
		smtpServer.stop();
	}

	public static DropwizardAppRule<NucleusConfig> getNucleus() {
		return nucleus;
	}
	
	public static SimpleSmtpServer getSmtpServer() {
		return smtpServer;
	}
}
