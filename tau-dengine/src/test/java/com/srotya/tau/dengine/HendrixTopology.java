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
package com.srotya.tau.dengine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.srotya.tau.dengine.AlertTupleMapper;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.bolts.ErrorBolt;
import com.srotya.tau.dengine.bolts.InterceptionBolt;
import com.srotya.tau.dengine.bolts.JSONTranslatorBolt;
import com.srotya.tau.dengine.bolts.RuleTranslatorBolt;
import com.srotya.tau.dengine.bolts.RulesEngineBolt;
import com.srotya.tau.dengine.bolts.StateTrackingBolt;
import com.srotya.tau.dengine.bolts.TemplateTranslatorBolt;
import com.srotya.tau.dengine.bolts.TemplatedAlertingEngineBolt;
import com.srotya.tau.dengine.bolts.helpers.AlertViewerBolt;
import com.srotya.tau.dengine.bolts.helpers.FileLogReaderSpout;
import com.srotya.tau.dengine.bolts.helpers.FileWriterBolt;
import com.srotya.tau.dengine.bolts.helpers.SpoolingFileSpout;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.AuthorizationException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.selector.DefaultTopicSelector;

/**
 * Wiring of Hendrix Storm Topology
 * 
 * @author ambud_sharma
 */
public class HendrixTopology {

	public static final String LOCAL = "local";
	private static final Logger logger = Logger.getLogger(HendrixTopology.class.getName());
	private static final String KAFKA_ALERT_TOPIC = "kafka.error.topic";
	private TopologyBuilder builder;
	private Properties config;
	private StormTopology topology;
	private String topologyName;

	/**
	 * Attach and configure Kafka Spouts
	 */
	public void attachAndConfigureKafkaSpouts() {
	}

	/**
	 * Attach and configure File Spouts for local testing
	 */
	public void attachAndConfigureFileSpouts() {
		logger.info("Running in local mode");
		builder.setSpout(Constants.TOPOLOGY_KAFKA_SPOUT + Constants.DEFAULT_TOPIC_NAME,
				new FileLogReaderSpout("~/hendrix/test-data")).setMaxTaskParallelism(1);
		builder.setSpout(Constants.TOPOLOGY_RULE_SYNC_SPOUT, new SpoolingFileSpout("/tmp/rule-updates.txt"))
				.setMaxTaskParallelism(1);
		builder.setSpout(Constants.TOPOLOGY_TEMPLATE_SYNC_SPOUT, new SpoolingFileSpout("/tmp/template-updates.txt"))
				.setMaxTaskParallelism(1);
	}

	/**
	 * Attach and configure bolts
	 */
	public void attachAndConfigureBolts() {
		BoltDeclarer validationBolt = builder.setBolt(Constants.TOPOLOGY_INTERCEPTION_BOLT, new InterceptionBolt())
				.setMaxTaskParallelism(Integer.parseInt(
						config.getProperty(Constants.INTERCEPTION_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		validationBolt.shuffleGrouping(Constants.TOPOLOGY_KAFKA_SPOUT + Constants.DEFAULT_TOPIC_NAME);

		builder.setBolt(Constants.TOPOLOGY_TRANSLATOR_BOLT, new JSONTranslatorBolt())
				.shuffleGrouping(Constants.TOPOLOGY_INTERCEPTION_BOLT).setMaxTaskParallelism(Integer.parseInt(
						config.getProperty(Constants.TRANSLATOR_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		builder.setBolt(Constants.RULE_SYNC_COMPONENT, new RuleTranslatorBolt())
				.shuffleGrouping(Constants.TOPOLOGY_RULE_SYNC_SPOUT).setMaxTaskParallelism(Integer.parseInt(config
						.getProperty(Constants.RULE_TRANSLATOR_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		builder.setBolt(Constants.TEMPLATE_SYNC_COMPONENT, new TemplateTranslatorBolt())
				.shuffleGrouping(Constants.TOPOLOGY_RULE_SYNC_SPOUT).setMaxTaskParallelism(Integer.parseInt(config
						.getProperty(Constants.RULE_TRANSLATOR_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		builder.setBolt(Constants.TOPOLOGY_RULES_BOLT, new RulesEngineBolt())
				.shuffleGrouping(Constants.TOPOLOGY_TRANSLATOR_BOLT)
				.allGrouping(Constants.RULE_SYNC_COMPONENT, Constants.SYNC_STREAM_ID)
				.setMaxTaskParallelism(Integer.parseInt(
						config.getProperty(Constants.RULES_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		builder.setBolt(Constants.TOPOLOGY_STATE_BOLT, new StateTrackingBolt())
				.fieldsGrouping(Constants.TOPOLOGY_RULES_BOLT, Constants.STATE_STREAM_ID,
						new Fields(Constants.FIELD_RULE_ACTION_ID, Constants.FIELD_AGGREGATION_KEY))
				.setMaxTaskParallelism(Integer.parseInt(
						config.getProperty(Constants.RULES_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		if (Boolean.parseBoolean(config.getProperty(Constants.ENABLE_ALERT_VIEWER, Constants.FALSE))) {
			builder.setBolt(Constants.ALERT_VIEWER_BOLT, new AlertViewerBolt())
					.shuffleGrouping(Constants.TOPOLOGY_RULES_BOLT, Constants.ALERT_STREAM_ID)
					.setMaxTaskParallelism(Integer.parseInt(
							config.getProperty(Constants.TRANSLATOR_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));
		}

		builder.setBolt(Constants.TOPOLOGY_ALERT_BOLT, new TemplatedAlertingEngineBolt())
				.allGrouping(Constants.TEMPLATE_SYNC_COMPONENT, Constants.SYNC_STREAM_ID)
				.shuffleGrouping(Constants.TOPOLOGY_RULES_BOLT, Constants.ALERT_STREAM_ID)
				.setMaxTaskParallelism(Integer.parseInt(
						config.getProperty(Constants.ALERT_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		builder.setBolt(Constants.ERROR_BOLT, new ErrorBolt())
				.shuffleGrouping(Constants.TOPOLOGY_TRANSLATOR_BOLT, Constants.ERROR_STREAM)
				.shuffleGrouping(Constants.RULE_SYNC_COMPONENT, Constants.ERROR_STREAM)
				.shuffleGrouping(Constants.TOPOLOGY_RULES_BOLT, Constants.ERROR_STREAM)
				.shuffleGrouping(Constants.TOPOLOGY_ALERT_BOLT, Constants.ERROR_STREAM);

		// send alerts to file if running in local mode, send to kafka in
		// distributed mode
		if (config.getProperty(LOCAL) == null) {
			builder.setBolt(Constants.TOPOLOGY_ALERT_KAFKA_BOLT,
					new KafkaBolt<String, String>()
							.withTopicSelector(
									new DefaultTopicSelector(config.getProperty(KAFKA_ALERT_TOPIC, "alertTopic")))
							.withTupleToKafkaMapper(new AlertTupleMapper()))
					.shuffleGrouping(Constants.TOPOLOGY_ALERT_BOLT, Constants.ALERT_STREAM_ID)
					.setMaxTaskParallelism(Integer.parseInt(
							config.getProperty(Constants.KAFKA_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));

		} else {
			builder.setBolt(Constants.TOPOLOGY_ALERT_KAFKA_BOLT, new FileWriterBolt())
					.shuffleGrouping(Constants.TOPOLOGY_ALERT_BOLT, Constants.ALERT_STREAM_ID)
					.setMaxTaskParallelism(Integer.parseInt(
							config.getProperty(Constants.KAFKA_BOLT_PARALLELISM_HINT, Constants.PARALLELISM_ONE)));
		}

	}

	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length < 1) {
			System.err.println("Must specify topology configuration file path");
			System.exit(-1);
		}
		Properties props = new Properties();
		props.load(new FileInputStream(args[0]));
		HendrixTopology topology = new HendrixTopology();
		topology.setConfiguration(props);
		try {
			topology.initialize();
		} catch (Exception e) {
			System.err.println("Failed to initialize the topology:" + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
		Config conf = new Config();
		for (Entry<Object, Object> entry : props.entrySet()) {
			conf.put(entry.getKey().toString(), entry.getValue());
		}
		if (props.getProperty(LOCAL) != null) {
			if (props.getProperty(FileLogReaderSpout.LOG_DIR) == null) {
				System.out.println("Must have log directory to read data from");
				return;
			}
			LocalCluster localStorm = new LocalCluster();
			localStorm.submitTopology(topology.getTopologyName(), conf, topology.getTopology());
		} else {
			try {
				StormSubmitter.submitTopology(topology.getTopologyName(), conf, topology.getTopology());
			} catch (AlreadyAliveException | InvalidTopologyException | AuthorizationException e) {
				logger.log(Level.SEVERE, "Error submitted the topology", e);
			}
		}
	}

	// @Override
	public void initialize() throws Exception {
		builder = new TopologyBuilder();
		topologyName = config.getProperty(Constants.TOPOLOGY_NAME, "Hendrix");
		if (config.getProperty(LOCAL) != null) {
			attachAndConfigureFileSpouts();
		} else {
			attachAndConfigureKafkaSpouts();
		}
		attachAndConfigureBolts();
		topology = builder.createTopology();
	}

	// @Override
	public void setConfiguration(Properties configuration) {
		this.config = configuration;
	}

	// @Override
	public StormTopology getTopology() {
		return topology;
	}

	/**
	 * @return the topologyName
	 */
	protected String getTopologyName() {
		return topologyName;
	}

	/**
	 * @return the builder
	 */
	public TopologyBuilder getBuilder() {
		return builder;
	}

	/**
	 * @param builder
	 *            the builder to set
	 */
	public void setBuilder(TopologyBuilder builder) {
		this.builder = builder;
	}

}
