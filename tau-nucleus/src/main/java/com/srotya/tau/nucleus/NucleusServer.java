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
package com.srotya.tau.nucleus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.srotya.tau.nucleus.api.CommandReceiver;
import com.srotya.tau.nucleus.api.EventReceiver;
import com.srotya.tau.nucleus.api.QueryPerfStats;
import com.srotya.tau.nucleus.ingress.IngressManager;
import com.srotya.tau.nucleus.ingress.IngressManager.IngresserFactory;
import com.srotya.tau.nucleus.processor.AlertingProcessor;
import com.srotya.tau.nucleus.processor.EmissionProcessor;
import com.srotya.tau.nucleus.processor.RuleProcessor;
import com.srotya.tau.nucleus.processor.StateProcessor;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

/**
 * @author ambudsharma
 */
public class NucleusServer extends Application<NucleusConfig> {

	private static final Logger logger = Logger.getLogger(NucleusServer.class.getName());

	public NucleusServer() {
	}

	@Override
	public void run(NucleusConfig configuration, Environment environment) throws Exception {
		AlertingProcessor alertingProcessor = initializeAlertProcessor(configuration, environment);
		StateProcessor stateProcessor = initializeStateProcessor(configuration, environment);
		EmissionProcessor emissionProcessor = initializeEmissionController(configuration, environment, stateProcessor);
		RuleProcessor ruleProcessor = initializeRuleProcessor(configuration, environment, alertingProcessor, stateProcessor);
		stateProcessor.setOutputProcessors(ruleProcessor);
		initializeIngressManager(configuration, environment, ruleProcessor);
		registerAPIs(environment, ruleProcessor, alertingProcessor, emissionProcessor);
		logger.info("Initialization complete");
	}

	private EmissionProcessor initializeEmissionController(NucleusConfig configuration, Environment environment, StateProcessor stateProcessor) throws IOException {
		Properties reConfig = new Properties();
		if (configuration.isIntegrationTest()) {
			reConfig.load(ClassLoader.getSystemResourceAsStream(configuration.getRuleEngineConfiguration()));
		} else {
			reConfig.load(new FileInputStream(configuration.getRuleEngineConfiguration()));
		}
		Map<String, String> conf = Utils.hashTableTohashMap(reConfig);
		EmissionProcessor emissionProcessor = new EmissionProcessor(new DisruptorUnifiedFactory(), conf, stateProcessor);
		environment.lifecycle().manage(emissionProcessor);
		return emissionProcessor;
	}

	private AlertingProcessor initializeAlertProcessor(NucleusConfig configuration, Environment environment)
			throws FileNotFoundException, IOException {
		Properties reConfig = new Properties();
		if (configuration.isIntegrationTest()) {
			reConfig.load(ClassLoader.getSystemResourceAsStream(configuration.getRuleEngineConfiguration()));
		} else {
			reConfig.load(new FileInputStream(configuration.getRuleEngineConfiguration()));
		}
		Map<String, String> conf = Utils.hashTableTohashMap(reConfig);
		AlertingProcessor alertProcessor = new AlertingProcessor(new DisruptorUnifiedFactory(), configuration.getAlertEngineParallelism(),
				1024 * 2, conf, null);
		environment.lifecycle().manage(alertProcessor);
		return alertProcessor;
	}

	private StateProcessor initializeStateProcessor(NucleusConfig configuration, Environment environment)
			throws FileNotFoundException, IOException {
		Properties reConfig = new Properties();
		if (configuration.isIntegrationTest()) {
			reConfig.load(ClassLoader.getSystemResourceAsStream(configuration.getRuleEngineConfiguration()));
		} else {
			reConfig.load(new FileInputStream(configuration.getRuleEngineConfiguration()));
		}
		Map<String, String> conf = Utils.hashTableTohashMap(reConfig);
		StateProcessor stateProcessor = new StateProcessor(new DisruptorUnifiedFactory(), configuration.getRuleEngineParallelism(),
				1024 * 2, conf, null);
		environment.lifecycle().manage(stateProcessor);
		return stateProcessor;
	}

	private void registerAPIs(Environment environment, RuleProcessor ruleProcessor, AlertingProcessor alertingProcessor, EmissionProcessor emissionProcessor) {
		environment.jersey().register(new QueryPerfStats());
		environment.jersey().register(new EventReceiver(new DisruptorUnifiedFactory(), ruleProcessor));
		environment.jersey()
				.register(new CommandReceiver(new DisruptorUnifiedFactory(), ruleProcessor, alertingProcessor, emissionProcessor));
	}

	private RuleProcessor initializeRuleProcessor(NucleusConfig configuration, Environment environment, AlertingProcessor alertingProcessor, StateProcessor stateProcessor)
			throws IOException, FileNotFoundException {
		Properties reConfig = new Properties();
		if (configuration.isIntegrationTest()) {
			reConfig.load(ClassLoader.getSystemResourceAsStream(configuration.getRuleEngineConfiguration()));
		} else {
			reConfig.load(new FileInputStream(configuration.getRuleEngineConfiguration()));
		}
		RuleProcessor ruleProcessor = new RuleProcessor(new DisruptorUnifiedFactory(), configuration.getRuleEngineParallelism(),
				1024 * 2, Utils.hashTableTohashMap(reConfig), alertingProcessor, stateProcessor);
		environment.lifecycle().manage(ruleProcessor);
		return ruleProcessor;
	}

	private IngressManager initializeIngressManager(NucleusConfig configuration, Environment environment, RuleProcessor ruleProcessor)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException,
			FileNotFoundException {
		Class<?> ingresser = Class.forName(configuration.getIngresserFactoryClass());
		IngresserFactory factory = (IngresserFactory) ingresser.newInstance();
		factory.setIngresserParallelism(configuration.getIngresserParallelism());
		Properties ingresserConf = new Properties();
		ingresserConf.load(new FileInputStream(configuration.getIngresserFactoryConfiguration()));
		factory.setConf(ingresserConf);
		IngressManager ingressManager = new IngressManager(ruleProcessor, factory);
		environment.lifecycle().manage(ingressManager);
		return ingressManager;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new NucleusServer().run(args);
	}

}
