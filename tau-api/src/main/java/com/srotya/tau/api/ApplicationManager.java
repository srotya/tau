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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.validation.ValidationFeature;

import com.srotya.tau.api.commands.APICommandEventSourcer;
import com.srotya.tau.api.commands.CommandEventSourcer;
import com.srotya.tau.api.dao.PerformanceMonitor;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.dao.alertreceiver.EventViewer;
import com.srotya.tau.api.hc.DBHealthCheck;
import com.srotya.tau.api.rest.PerfMonEndpoint;
import com.srotya.tau.api.rest.RestReceiver;
import com.srotya.tau.api.rest.RuleGroupEndpoint;
import com.srotya.tau.api.rest.RulesEndpoint;
import com.srotya.tau.api.rest.TemplateEndpoint;
import com.srotya.tau.api.security.BapiAuthorizationFilter;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.api.validations.VelocityValidator;
import com.srotya.tau.omega.ScriptValidator;
import com.srotya.tau.wraith.rules.StatelessRulesEngine;
import com.srotya.tau.wraith.rules.validator.RuleValidator;
import com.srotya.tau.wraith.rules.validator.Validator;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Entry point class for Dropwizard app
 * 
 * @author ambud_sharma
 */
public class ApplicationManager extends Application<AppConfig>implements Daemon {

	private static final String EVENTSOURCER_CLASS = "eventsourcer.class";
	public static final boolean LOCAL = Boolean.parseBoolean(System.getProperty("local", "false"));
	private static final String EVENT_VIEWER_TOPIC = "alertrcv.topic.name";
	private static final String IGNITE_DISOVERY_ADDRESS = "ignite.discovery.address";
	private static final String JAVAX_PERSISTENCE_JDBC_URL = "javax.persistence.jdbc.url";
	private static final String JAVAX_PERSISTENCE_JDBC_DRIVER = "javax.persistence.jdbc.driver";
	private static final String JAVAX_PERSISTENCE_JDBC_PASSWORD = "javax.persistence.jdbc.password";
	private static final String JAVAX_PERSISTENCE_JDBC_USER = "javax.persistence.jdbc.user";
	private static final String JAVAX_PERSISTENCE_JDBC_DB = "javax.persistence.jdbc.db";
	private Properties config;
	private EntityManagerFactory factory;
	private KafkaConsumer<String, String> consumer;
	private String[] args;
	private PerformanceMonitor perfMonitor;
	private Ignite ignite;
	private EventViewer alertReceiver;
	private AppConfig configuration;
	private CommandEventSourcer sourcer;

	/**
	 * @param appConfiguration
	 */
	public void init(AppConfig appConfiguration) {
		config = new Properties(System.getProperties());
		if (appConfiguration.getTauConfig() != null) {
			try {
				config.load(new FileInputStream(appConfiguration.getTauConfig()));
			} catch (IOException e) {
				throw new RuntimeException("Configuration file not loaded", e);
			}
		} else {
			try {
				config.load(ApplicationManager.class.getClassLoader().getResourceAsStream("default-config.properties"));
			} catch (IOException e) {
				throw new RuntimeException("Default configuration file not loaded", e);
			}
		}
		try {
			Utils.createDatabase(config.getProperty(JAVAX_PERSISTENCE_JDBC_URL),
					config.getProperty(JAVAX_PERSISTENCE_JDBC_DB, "tau"),
					config.getProperty(JAVAX_PERSISTENCE_JDBC_USER),
					config.getProperty(JAVAX_PERSISTENCE_JDBC_PASSWORD),
					config.getProperty(JAVAX_PERSISTENCE_JDBC_DRIVER));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		config.setProperty(JAVAX_PERSISTENCE_JDBC_URL, config.getProperty(JAVAX_PERSISTENCE_JDBC_URL)
				+ config.getProperty(JAVAX_PERSISTENCE_JDBC_DB, "tau") + "?useSSL=false");
		factory = Persistence.createEntityManagerFactory("tau", config);
		EntityManager em = factory.createEntityManager();
		em.close();
		initializeEventSourcer();
		checkAndCreateGlobalRuleGroup();
	}

	private void initializeEventSourcer() {
		try {
			sourcer = (CommandEventSourcer) (Class.forName(config.getProperty(EVENTSOURCER_CLASS, APICommandEventSourcer.class.getName()))).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("Invalid EventSourcer class", e);
		}
		sourcer.setApplicationManager(this);
		try {
			sourcer.init();
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize EventSourcer", e);
		}
	}

	/**
	 * 
	 */
	protected void checkAndCreateGlobalRuleGroup() {
		EntityManager em = getEM();
		RuleGroupManager mgr = RuleGroupManager.getInstance();
		try {
			mgr.getRuleGroup(em, StatelessRulesEngine.ALL_RULEGROUP);
		} catch (Exception e) {
			System.err.println("ALL rule group doesn't exist, creating");
			RuleGroup ruleGroup = new RuleGroup();
			ruleGroup.setRuleGroupId(StatelessRulesEngine.ALL_RULEGROUP);
			ruleGroup.setRuleGroupName(StatelessRulesEngine.ALL_RULEGROUP);
			try {
				mgr.createRuleGroup(em, ruleGroup);
			} catch (Exception e1) {
				throw new RuntimeException("Failed to create ALL rule group", e);
			}
		}
	}

	/**
	 * @param config
	 */
	public void addRuleValidators(Properties config) {
		List<Validator<?>> validators = Arrays.asList(new VelocityValidator(), new ScriptValidator());
		RuleValidator.getInstance().configure(validators);
	}

	/**
	 * 
	 */
	public void initKafkaConnection() {
		config.put("group.id", "tauAR_"+Utils.getHostName());
		consumer = new KafkaConsumer<>(config);
		consumer.subscribe(config.getProperty(EVENT_VIEWER_TOPIC, "eventViewerTopic"));
	}

	/**
	 * @return new entity manager instance
	 */
	public EntityManager getEM() {
		return factory.createEntityManager();
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new ApplicationManager().run(args);
	}

	@Override
	public void initialize(Bootstrap<AppConfig> bootstrap) {
		super.initialize(bootstrap);
	}

	@Override
	public void run(AppConfig configuration, Environment environment) throws Exception {
		this.configuration = configuration;
		init(configuration);
		environment.jersey().property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, false);
		environment.jersey().register(ValidationFeature.class);
		if (configuration.isEnableAuthorization()) {
			environment.jersey().register(new BapiAuthorizationFilter());
			environment.jersey().register(RolesAllowedDynamicFeature.class);
		}
		configureIgnite(configuration, environment);
		perfMonitor = new PerformanceMonitor(this);
		environment.lifecycle().manage(perfMonitor);
		alertReceiver = new EventViewer(this);
		environment.lifecycle().manage(alertReceiver);
		environment.jersey().register(new RulesEndpoint(this));
		environment.jersey().register(new RuleGroupEndpoint(this));
		environment.jersey().register(new TemplateEndpoint(this));
		environment.jersey().register(new RestReceiver(this));
		environment.jersey().register(new PerfMonEndpoint(this));
		environment.healthChecks().register("dbHC", new DBHealthCheck(this));
		FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

		// Configure CORS parameters
		cors.setInitParameter("allowedOrigins", "*");
		cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
		cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

		// Add URL mapping
		cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
	}

	/**
	 * @param configuration
	 * @param environment
	 */
	protected void configureIgnite(AppConfig configuration, Environment environment) {
		Ignition.setClientMode(false);
		IgniteConfiguration cfg = new IgniteConfiguration();
		cfg.setGridName("tau");
		cfg.setClientMode(false);
		cfg.setClockSyncFrequency(2000);
		TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
		discoSpi.setAckTimeout(3000);
		discoSpi.setHeartbeatFrequency(2000);
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		ipFinder.setAddresses(Arrays.asList(config.getProperty(IGNITE_DISOVERY_ADDRESS, "localhost")));
		discoSpi.setIpFinder(ipFinder);
		cfg.setDiscoverySpi(discoSpi);
		
		cfg.setIncludeEventTypes(org.apache.ignite.events.EventType.EVTS_CACHE);
		
		ignite = Ignition.start(cfg);
		System.err.println("\n\nIgnite using TCP static IP based discovery with address:"
				+ config.getProperty(IGNITE_DISOVERY_ADDRESS, "localhost") + "\n\n");
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		args = context.getArguments();
	}

	@Override
	public void start() throws Exception {
		List<String> arguments = new ArrayList<>();
		if (args.length >= 1) {
			for (String arg : args) {
				arguments.add(arg);
			}
		}
		arguments.add(0, "server");
		main(arguments.toArray(new String[1]));
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	public void destroy() {
	}

	/**
	 * @return
	 */
	public PerformanceMonitor getPerfMonitor() {
		return perfMonitor;
	}

	/**
	 * @return
	 */
	public Ignite getIgnite() {
		return ignite;
	}

	/**
	 * @return
	 */
	public EventViewer getAlertReceiver() {
		return alertReceiver;
	}

	/**
	 * @return the configuration
	 */
	public AppConfig getConfiguration() {
		return configuration;
	}

	/**
	 * @return the consumer
	 */
	public KafkaConsumer<String, String> getConsumer() {
		return consumer;
	}

	/**
	 * @return
	 */
	public CommandEventSourcer getSourcer() {
		return sourcer;
	}
	
	/**
	 * @return
	 */
	public Properties getConfig() {
		return config;
	}
}
