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
package com.srotya.tau.dengine.bolts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.dengine.UnifiedFactory;
import com.srotya.tau.dengine.Utils;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.aggregations.StateTrackingEngine;
import com.srotya.tau.wraith.aggregators.AggregationRejectException;

import backtype.storm.Config;
import backtype.storm.metric.api.MultiCountMetric;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Bolt implementation of {@link StateTrackingEngine}
 * 
 * @author ambud_sharma
 */
public class StateTrackingBolt extends BaseRichBolt {

	private static final String STATE_FLUSH_TIMEOUT = "state.flush.timeout";
	private static final String _METRIC_STATE_HIT = "mcm.state.hit";
	private static final int DEFAULT_STATE_FLUSH_BUFFER_SIZE = 1000;
	public static final String STATE_FLUSH_BUFFER_SIZE = "state.flush.buffer.size";
	private static final long serialVersionUID = 1L;
	private Logger logger;
	private transient StateTrackingEngine stateTrackingEngine;
	private transient OutputCollector collector;
	private transient List<Tuple> buffer;
	private transient int bufferSize;
	private transient UnifiedFactory unifiedFactory;
	private transient MultiCountMetric stateHit;
	private transient long bufferTickCounter;
	private transient int flushTimeout;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.logger = Logger.getLogger(StateTrackingBolt.class.getName());
		this.collector = collector;
		if (stormConf.containsKey(STATE_FLUSH_BUFFER_SIZE)) {
			bufferSize = Integer.parseInt(stormConf.get(STATE_FLUSH_BUFFER_SIZE).toString());
		} else {
			bufferSize = DEFAULT_STATE_FLUSH_BUFFER_SIZE;
		}
		this.buffer = new ArrayList<>(bufferSize);
		this.unifiedFactory = new UnifiedFactory();
		this.stateTrackingEngine = new StateTrackingEngine(unifiedFactory, unifiedFactory);
		int taskId = context.getThisTaskIndex();
		try {
			stateTrackingEngine.initialize(stormConf, taskId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		stateHit = new MultiCountMetric();
		if (context != null) {
			context.registerMetric(_METRIC_STATE_HIT, stateHit, Constants.METRICS_FREQUENCY);
		}
		this.flushTimeout = 30;
		if (stormConf.containsKey(STATE_FLUSH_TIMEOUT)) {
			this.flushTimeout = Integer.parseInt(stormConf.get(STATE_FLUSH_TIMEOUT).toString());
		}
	}

	@Override
	public void execute(Tuple tuple) {
		if (Utils.isStateTrackingTuple(tuple)) {
			trackState(tuple);
		} else if (Utils.isWraithTickTuple(tuple)) {
			performEmits(tuple);
		} else if (Utils.isTickTuple(tuple)) {
			checkAndPerformTimeBasedFlush(tuple);
		}
	}

	protected void trackState(Tuple tuple) {
		try {
			stateHit.scope(Utils.separateRuleActionId(tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID))
					.getKey().toString()).incr();
			if (tuple.getBooleanByField(Constants.FIELD_STATE_TRACK)) {
				logger.fine("State tracking true:" + tuple);
				stateTrackingEngine.track(tuple.getLongByField(Constants.FIELD_TIMESTAMP),
						tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW),
						tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID),
						tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY));
			} else {
				logger.fine("State tracking false:" + tuple);
				stateTrackingEngine.untrack(tuple.getLongByField(Constants.FIELD_TIMESTAMP),
						tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW),
						tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID),
						tuple.getStringByField(Constants.FIELD_AGGREGATION_KEY));
			}
			buffer.add(tuple);
			if (buffer.size() >= bufferSize) {
				flushAckAndClearBuffer();
			}
		} catch (AggregationRejectException e) {
			StormContextUtil.emitErrorTuple(collector, tuple, StateTrackingBolt.class, "",
					"State tracking rejected", e);
			collector.ack(tuple);
		} catch (IOException e) {
			failAndClearBuffer();
			StormContextUtil.emitErrorTuple(collector, tuple, StateTrackingBolt.class, "",
					"State tracking flush failed", e);
		}
	}

	protected void performEmits(Tuple tuple) {
		String ruleActionId = tuple.getStringByField(Constants.FIELD_RULE_ACTION_ID);
		String ruleGroup = tuple.getStringByField(Constants.FIELD_RULE_GROUP);
		Entry<Short, Short> ruleActionIdSeparates = Utils.separateRuleActionId(ruleActionId);
		try {
			List<Event> aggregateHeaders = new ArrayList<>();
			emitAndResetAggregates((int) tuple.getIntegerByField(Constants.FIELD_AGGREGATION_WINDOW), ruleActionId,
					aggregateHeaders);
			if (!aggregateHeaders.isEmpty()) {
				for (Event event : aggregateHeaders) {
					event.getHeaders().put(Constants.FIELD_RULE_GROUP, ruleGroup);
					event.getHeaders().put(Constants.FIELD_RULE_ID, ruleActionIdSeparates.getKey());
					event.getHeaders().put(Constants.FIELD_ACTION_ID, ruleActionIdSeparates.getValue());
					collector.emit(Constants.AGGREGATION_OUTPUT_STREAM, tuple, new Values(event));
				}
			} else {
				logger.warning("No state aggregations to emit:" + stateTrackingEngine.getAggregationMap());
			}
		} catch (Exception e) {
			// throw e;
		}
		collector.ack(tuple);
	}

	protected void checkAndPerformTimeBasedFlush(Tuple tuple) {
		bufferTickCounter++;
		if (bufferTickCounter % flushTimeout == 0) {
			try {
				flushAckAndClearBuffer();
			} catch (IOException e) {
				failAndClearBuffer();
				StormContextUtil.emitErrorTuple(collector, tuple, StateTrackingBolt.class, "",
						"State tracking flush failed", e);
			}
		}
		collector.ack(tuple);
	}

	/**
	 * Fail all tuples in buffer and clear buffer
	 */
	protected void failAndClearBuffer() {
		for (Tuple t : buffer) {
			collector.fail(t);
		}
		buffer.clear();
	}

	/**
	 * Flush and ack all tuples in buffer and clear buffer 
	 * @throws IOException
	 */
	protected void flushAckAndClearBuffer() throws IOException {
		stateTrackingEngine.flush();
		for (Tuple t : buffer) {
			collector.ack(t);
		}
		buffer.clear();
	}

	/**
	 * @param ruleActionId
	 * @param aggregateHeaders
	 * @throws IOException
	 */
	public void emitAndResetAggregates(int aggregationWindow, String ruleActionId,
			List<Event> aggregateHeaders) throws IOException {
		if (stateTrackingEngine.containsRuleActionId(ruleActionId)) {
			stateTrackingEngine.emit(aggregationWindow, ruleActionId, aggregateHeaders);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream(Constants.AGGREGATION_OUTPUT_STREAM, new Fields(Constants.FIELD_EVENT));
		StormContextUtil.declareErrorStream(declarer);
	}

	@Override
	public void cleanup() {
		try {
			stateTrackingEngine.cleanup();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		Config conf = new Config();
		// send tick tuples every second
		conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, 1);
		return conf;
	}

	/**
	 * @return the collector
	 */
	protected OutputCollector getCollector() {
		return collector;
	}

	/**
	 * @return engine
	 */
	protected StateTrackingEngine getStateTrackingEngine() {
		return stateTrackingEngine;
	}

}
