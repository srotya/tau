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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Before;

import com.srotya.tau.dengine.bolts.TestAlertingEngineBolt;

import backtype.storm.Config;
import backtype.storm.ILocalCluster;
import backtype.storm.Testing;
import backtype.storm.testing.CompleteTopologyParam;
import backtype.storm.testing.MkClusterParam;
import backtype.storm.testing.MockedSources;
import backtype.storm.testing.TestJob;
import backtype.storm.tuple.Values;

/**
 * Tests for topology
 * 
 * @author ambud_sharma
 */
public class TestHendrixTopologyRuleGroups {

	private Properties properties = new Properties();

	@Before
	public void before() throws IOException {
		properties.clear();
	}

//	@Test
	public void testHendrixTopology() throws Exception {
		for (String file : new String[] { "topology-test1" }) {

			List<String> rules = Utils.readAllLinesFromStream(
					TestHendrixTopologyRuleGroups.class.getClassLoader().getResourceAsStream(file + ".rules"));
			
			StringBuilder builder = new StringBuilder();
			for (String rule : rules) {
				builder.append(rule);
			}
			MkClusterParam mkClusterParam = new MkClusterParam();
			properties.load(TestHendrixTopologyRuleGroups.class.getClassLoader().getResourceAsStream(file + ".props"));
			properties.put("rule.group.active", "true");
//			properties.put(Constants.ASTORE_TYPE, "io.symcpe.wraith.silo.redis.RedisAggregationStore");
			properties.put(TestAlertingEngineBolt.RULES_CONTENT, builder.toString());
			List<String> templates = Utils.readAllLinesFromStream(
					TestHendrixTopologyRuleGroups.class.getClassLoader().getResourceAsStream(file + ".template"));
			builder = new StringBuilder();
			for (String template : templates) {
				builder.append(template);
			}
			properties.put(TestAlertingEngineBolt.TEMPLATE_CONTENT, builder.toString());

			mkClusterParam.setSupervisors(1);
			Config daemonConf = new Config();
			daemonConf.put(Config.STORM_LOCAL_MODE_ZMQ, false);
			daemonConf.put(Config.TOPOLOGY_ENABLE_MESSAGE_TIMEOUTS, false);
			daemonConf.put(Config.TOPOLOGY_MESSAGE_TIMEOUT_SECS, 180);
			mkClusterParam.setDaemonConf(daemonConf);

			Testing.withSimulatedTimeLocalCluster(mkClusterParam, new TestJob() {

				@SuppressWarnings("rawtypes")
				@Override
				public void run(ILocalCluster cluster) throws Exception {
					HendrixTopology hendrix = new HendrixTopology();
					hendrix.setConfiguration(properties);
					hendrix.initialize();

					// prepare the mock data
					MockedSources mockedSources = new MockedSources();

					List<Values> test1 = new ArrayList<>();

					List<String> lines = Utils.readAllLinesFromStream(
							TestHendrixTopologyRuleGroups.class.getClassLoader().getResourceAsStream(file + ".json"));
					for (int i = 0; i < lines.size(); i++) {
						String line = lines.get(i);
						test1.add(new Values(line, String.valueOf(i)));
					}

					mockedSources.addMockData(Constants.TOPOLOGY_KAFKA_SPOUT + Constants.DEFAULT_TOPIC_NAME,
							test1.toArray(new Values[1]));
					mockedSources.addMockData(Constants.TOPOLOGY_RULE_SYNC_SPOUT, new Values(""));
					mockedSources.addMockData(Constants.TOPOLOGY_TEMPLATE_SYNC_SPOUT, new Values(""));

					// prepare the config
					Config conf = new Config();
					conf.setNumWorkers(2);
					conf.setDebug(false);
					for (Entry<Object, Object> entry : properties.entrySet()) {
						conf.put((String) entry.getKey(), entry.getValue());
					}

					CompleteTopologyParam completeTopologyParam = new CompleteTopologyParam();
					completeTopologyParam.setMockedSources(mockedSources);
					completeTopologyParam.setStormConf(conf);

					Map result = Testing.completeTopology(cluster, hendrix.getTopology(), completeTopologyParam);

					for (Object object : result.entrySet()) {
						System.err.println("Result:" + object);
					}

					// check whether the result is right
					assertEquals(test1.size(), Testing.readTuples(result, Constants.TOPOLOGY_TRANSLATOR_BOLT).size());
					assertEquals(2, Testing.readTuples(result, Constants.TOPOLOGY_RULES_BOLT, Constants.ALERT_STREAM_ID)
							.size());
					assertEquals(2, Testing.readTuples(result, Constants.TOPOLOGY_ALERT_BOLT, Constants.ALERT_STREAM_ID)
							.size());
				}
			});
		}
	}

}