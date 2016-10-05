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

/**
 * Set of all constants from Hendrix and Wraith
 * 
 * @author ambud_sharma
 */
public class Constants extends com.srotya.tau.wraith.Constants {

	private Constants() {
	}

	public static final int AVG_EVENT_FIELD_COUNT = Integer.parseInt(System.getProperty("event.field.count", "40"));
	public static final int METRICS_FREQUENCY = 10;
	public static final String PARALLELISM_ONE = "1";
	public static final String ALERT_VIEWER_BOLT = "alertViewerBolt";
	public static final String DEFAULT_TOPIC_NAME = "logTopic";
	public static final String DEFAULT_ZOOKEEPER = "localhost:2181";
	public static final String DEFAULT_RULES_TOPIC = "rulesTopic";
	public static final String SUPRESSION_STATE = "suppressionState";
	public static final String SUP_MON_STREAM = "suppressionStream";
	public static final String DELIVERY_STREAM = "deliveryStream";
	public static final String AGGREGATION_OUTPUT_STREAM = "aggregationOutputStream";

	// public static final String RULE_ENGINE_STREAM_ID = "reStream";
	public static final String RULE_SYNC_COMPONENT = "ruleComponent";
	public static final String TEMPLATE_SYNC_COMPONENT = "templateComponent";
	public static final String SYNC_STREAM_ID = "syncStream";
	public static final String TICK_STREAM_ID = "tickStream";
	public static final String STORE_STREAM_ID = "storeStream";
	public static final String EVENT_STREAM_ID = "eventStream";
	public static final String LOOPBACK_STREAM_ID = "loopbackStream";
	public static final String ALERT_STREAM_ID = "alertStream";
	public static final String STATE_STREAM_ID = "stateStream";

	public static final String TOPOLOGY_RULES_BOLT = "ruleBolt";
	public static final String TOPOLOGY_EVENT_BOLT = "eventBolt";
	public static final String TOPOLOGY_KAFKA_SPOUT = "kafkaSpout_";
	public static final String TOPOLOGY_RULE_SYNC_SPOUT = "rsync";
	public static final String TOPOLOGY_TEMPLATE_SYNC_SPOUT = "tsync";
	public static final String TOPOLOGY_INTERCEPTION_BOLT = "interceptionBolt";
	public static final String TOPOLOGY_ALERT_BOLT = "alertBolt";
	public static final String TOPOLOGY_ALERT_KAFKA_BOLT = "kafkaBolt";
	public static final String RULES_BOLT_PARALLELISM_HINT = "rules.parallelism";
	public static final String ALERT_BOLT_PARALLELISM_HINT = "alerts.parallelism";
	public static final String KAFKA_BOLT_PARALLELISM_HINT = "kafka.parallelism";
	public static final String TOPOLOGY_TRANSLATOR_BOLT = "translatorBolt";
	public static final String TRANSLATOR_BOLT_PARALLELISM_HINT = "translator.parallelism";
	public static final String RULE_TRANSLATOR_BOLT_PARALLELISM_HINT = "rule.translator.parallelism";
	public static final String ENABLE_ALERT_VIEWER = "enable.alert.viewer";
	public static final String KAFKA_RULES_TOPIC_NAME = "rule.topic.name";
	public static final String KAFKA_SPOUT_PARALLELISM = "spout.parallelism";
	public static final String ERROR_STREAM = "errorStream";
	public static final String ERROR_MESSAGE = "message";
	public static final String ERROR_SOURCE = "errorSource";
	public static final String ERROR_SOURCE_BOLT = "sourceBolt";
	public static final String ERROR_EXCEPTION = "errorException";
	public static final String ERROR_EXCEPTION_MESSAGE = "exceptionMessage";
	public static final String ERROR_BOLT = "errorBolt";
	public static final String KAFKA_ERROR_BOLT = "kafkaErrorBolt";
	public static final String DEFAULT_ERROR_TOPIC_NAME = "errorTopic";
	public static final String KAFKA_ERROR_STREAM = "kafkaErrorStream";
	public static final String FIELD_ERROR_VALUE = "eValue";
	public static final String FIELD_ERROR_KEY = "eKey";
	public static final String DEFAULT_KAFKA_ERROR = "false";
	public static final String ERROR_TIMESTAMP = "errorTs";
	public static final String INTERCEPTION_BOLT_PARALLELISM_HINT = "validation.parallelism";

	public static final String TOPOLOGY_NAME = "topology.name";
	public static final String TOPOLOGY_STATE_BOLT = "stateBolt";

}