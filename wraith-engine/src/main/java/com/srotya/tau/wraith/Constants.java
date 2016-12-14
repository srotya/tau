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
package com.srotya.tau.wraith;

/**
 * Central file for all the constants
 * 
 * @author ambud_sharma
 */
public abstract class Constants {

	public static final String KEY_SEPARATOR = "_";
	public static final String FIELD_TIMESTAMP = "_t";
	public static final String FIELD_EVENT_ID = "_i";
	public static final String FIELD_RULE_ID = "_r";
	public static final String FIELD_RULE_NAME = "_rn";
	public static final String FIELD_SPOUT_NAME = "_sn";
	public static final String FIELD_ACTION_ID = "_ai";
	public static final String FIELD_STATE_TRACK = "_st";
	public static final String FIELD_AGGREGATION_KEY = "_a";
	public static final String FIELD_AGGREGATION_TYPE = "_at";
	public static final String FIELD_AGGREGATION_VALUE = "_v";
	public static final String FIELD_ANOMALY_VALUE = "_nv";
	public static final String FIELD_ANOMALY_SERIES = "_ns";
	public static final String FIELD_RULE_CONTENT = "_rc";
	public static final String FIELD_TEMPLATE_CONTENT = "_tc";
	public static final String FIELD_EVENT = "_e";
	public static final String FIELD_EVENT_TYPE = "_y";
	public static final String FIELD_ALERT_TARGET = "target";
	public static final String FIELD_ALERT_MEDIA = "media";
	public static final String FIELD_ALERT_TEMPLATE_ID = "_tid";
	public static final String FIELD_ALERT_BODY = "body";
	public static final String FIELD_ALERT_SUBJECT = "subject";
	public static final String FIELD_ALERT = "alert";
	public static final String FIELD_ID = "_id";
	public static final String FIELD_RULE_GROUP = "_rg";
	
	public static final String FIELD_DESTINATION_TASK_ID = "_dtid";
	public static final String FIELD_DESTINATION_WORKER_ID = "_dwid";
	
	// public static final String FIELD_AGGREGATION_EMIT = "age";
	public static final String FIELD_AGGREGATION_WINDOW = "_agw";
	public static final String FIELD_RULE_ACTION_ID = "_ri";
	public static final String FIELD_RULE_ACTION = "_ra";
	public static final String FIELD_RULE_DELETE = "rule.delete";
	public static final String FIELD_TEMPLATE_DELETE = "template.delete";
	
	public static final String EVENT_TYPE_RULE_UPDATE = "1";
	public static final String EVENT_TYPE_TEMPLATE_UPDATE = "1";
	public static final String EVENT_TYPE_EMISSION = "2";

	public static final String ERROR_STREAM_ID = "st_err";

	public static final String ASTORE_TYPE = "astore.type";
	public static final String RSTORE_TYPE = "rstore.type";
	public static final String TSTORE_TYPE = "tstore.type";
	public static final String STORE_PASSWORD = "store.password";
	public static final String STORE_USERNAME = "store.username";

	public static final String RULE_HASH_INIT_SIZE = "rule.hash.init.size";
	public static final String DEFAULT_RULE_HASH_SIZE = "1000";
	public static final String ACTION_FAIL = "act_fail";

	public static final String FALSE = "false";
	public static final String TRUE = "true";

	public static final String AGGREGATIONS_SET_LIMIT = "aggregation.set.limit";
	public static final String AGGREGATIONS_FCOUNT_LIMIT = "aggregations.fcount.limit";
	public static final String DEFAULT_AGGREGATION_SET_LIMIT = "5000";
	public static final String DEFAULT_AGGREGATION_FCOUNT_LIMIT = "5000";
	public static final String AGGREGATION_HASH_INIT_SIZE = "aggregation.hash.init.size";
	public static final String DEFAULT_AGGREGATION_HASH_SIZE = "1000";
	public static final String AGGREGATOR_SET = "aggregator.set";
	public static final String COUNTER_TYPE = "counter.type";
	public static final float HASHSET_LOAD_FACTOR = 0.8f;
	public static final double SET_CAPACITY_AMPLIFICATION = 1.3;
	public static final String AGGREGATOR_TYPE = "aggregator.type";
	public static final String AGGREGATION_JITTER_TOLERANCE = "aggregation.jitter.tolerance";

	public static final String DEFAULT_JITTER_TOLERANCE = "10";
	public static final String HEADER_STORE_EVENT = "store";
	public static final String HEADER_EVENT_TYPE = "_t";
	public static final String HEADER_EVENT_ERROR_TYPE = "_et";
	public static final String HEADER_EVENT_ERROR_FIELD = "_ef";
	public static final String HEADER_EVENT_ERROR_VALUE = "_ev";
	public static final String ERROR_EVENT_TYPE = "e";
	public static final String NEXT_PROCESSOR = "_np";
	public static final String FIELD_TASK_ID = "_tid";
}
