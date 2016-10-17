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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.TauEvent;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.dengine.UnifiedFactory;
import com.srotya.tau.dengine.Utils;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleCommand;
import com.srotya.tau.wraith.rules.RulesEngineCaller;
import com.srotya.tau.wraith.rules.StatelessRulesEngine;

import backtype.storm.metric.api.MeanReducer;
import backtype.storm.metric.api.MultiCountMetric;
import backtype.storm.metric.api.MultiReducedMetric;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * Bolt implementing {@link StatelessRulesEngine}
 * 
 * @author ambud_sharma
 */
public class RulesEngineBolt extends BaseRichBolt implements RulesEngineCaller<Tuple, OutputCollector> {

	public static final String TENANTID_SEPARATOR = "_";
	private static final long serialVersionUID = 1L;
	public static final String _METRIC_RULE_HIT_COUNT = "mcm.rule.hit.count";
	public static final String _METRIC_RULE_NO_HIT_COUNT = "mcm.rule.nohit.count";
	public static final String _METRIC_CONDITION_EFFICIENCY = "mcm.condition.efficiency";
	public static final String _METRIC_RULE_EFFICIENCY = "mcm.rule.efficiency";
	private transient Logger logger;
	private transient Gson gson;
	private transient StatelessRulesEngine<Tuple, OutputCollector> rulesEngine;
	private transient OutputCollector collector;
	private transient MultiReducedMetric ruleEfficiency;
	private transient MultiReducedMetric conditionEfficiency;
	private transient MultiCountMetric ruleHitCount;
	private transient MultiCountMetric ruleNoHitCount;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.logger = Logger.getLogger(RulesEngineBolt.class.getName());
		UnifiedFactory factory = new UnifiedFactory();
		this.rulesEngine = new StatelessRulesEngine<Tuple, OutputCollector>(this, factory, factory);
		try {
			this.rulesEngine.initialize(stormConf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.gson = new Gson();
		this.collector = collector;
		this.ruleEfficiency = new MultiReducedMetric(new MeanReducer());
		this.conditionEfficiency = new MultiReducedMetric(new MeanReducer());
		this.ruleHitCount = new MultiCountMetric();
		this.ruleNoHitCount = new MultiCountMetric();
		if (context != null) {
			context.registerMetric(_METRIC_RULE_EFFICIENCY, ruleEfficiency, Constants.METRICS_FREQUENCY);
			context.registerMetric(_METRIC_CONDITION_EFFICIENCY, conditionEfficiency, Constants.METRICS_FREQUENCY);
			context.registerMetric(_METRIC_RULE_HIT_COUNT, ruleHitCount, Constants.METRICS_FREQUENCY);
			context.registerMetric(_METRIC_RULE_NO_HIT_COUNT, ruleHitCount, Constants.METRICS_FREQUENCY);
		}
		logger.info("Rules Engine Bolt initialized");
	}

	@Override
	public void execute(Tuple tuple) {
		if (Utils.isRuleSyncTuple(tuple)) {
			logger.info("Attempting to apply rule update:" + tuple.getValueByField(Constants.FIELD_RULE_CONTENT));
			RuleCommand ruleCommand = (RuleCommand) tuple.getValueByField(Constants.FIELD_RULE_CONTENT);
			try {
				logger.info("Received rule tuple with rule content:" + ruleCommand.getRuleContent());
				rulesEngine.updateRule(ruleCommand.getRuleGroup(), ruleCommand.getRuleContent(),
						ruleCommand.isDelete());
				logger.info("Applied rule update with rule content:" + ruleCommand.getRuleContent());
			} catch (Exception e) {
				// failed to update rule
				System.err.println("Failed to apply rule update:" + e.getMessage() + "\t"
						+ tuple.getValueByField(Constants.FIELD_RULE_CONTENT));
				StormContextUtil.emitErrorTuple(collector, tuple, RulesEngineBolt.class, tuple.toString(),
						"Failed to apply rule update", e);
			}
		} else {
			try {
				TauEvent event = (TauEvent) tuple.getValueByField(Constants.FIELD_EVENT);
				// call rules engine to evaluate this event and then trigger
				// appropriate actions
				rulesEngine.evaluateRules(collector, tuple, event);
			} catch (Exception e) {
				// unknown event type
				logger.log(Level.SEVERE, "Unknown event type:" + tuple, e);
			}
		}
		collector.ack(tuple);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// Disabled non-templated alerts
		// declarer.declareStream(Constants.ALERT_STREAM_ID, new
		// Fields(Constants.FIELD_EVENT, Constants.FIELD_RULE_ID,
		// Constants.FIELD_ACTION_ID, Constants.FIELD_ALERT_TARGET,
		// Constants.FIELD_ALERT_MEDIA, Constants.FIELD_RULE_GROUP,
		// Constants.FIELD_TIMESTAMP));
		declarer.declareStream(Constants.ALERT_STREAM_ID,
				new Fields(Constants.FIELD_EVENT, Constants.FIELD_RULE_ID, Constants.FIELD_ACTION_ID,
						Constants.FIELD_RULE_NAME, Constants.FIELD_ALERT_TEMPLATE_ID, Constants.FIELD_RULE_GROUP,
						Constants.FIELD_TIMESTAMP));
		declarer.declareStream(Constants.STATE_STREAM_ID,
				new Fields(Constants.FIELD_STATE_TRACK, Constants.FIELD_TIMESTAMP, Constants.FIELD_AGGREGATION_WINDOW,
						Constants.FIELD_RULE_ACTION_ID, Constants.FIELD_AGGREGATION_KEY));
		StormContextUtil.declareErrorStream(declarer);
	}

	@Override
	public void emitStateTrackingEvent(OutputCollector eventCollector, Tuple eventContainer, Boolean track,
			Event originalEvent, Long timestamp, int windowSize, String ruleActionId, String aggregationKey) {
		eventCollector.emit(Constants.STATE_STREAM_ID, eventContainer,
				new Values(track, timestamp, windowSize, ruleActionId, aggregationKey));
	}

	@Override
	public void emitRawAlert(OutputCollector eventCollector, Tuple eventContainer, Event outputEvent, Short ruleId,
			Short actionId, String target, String mediaType) {
		eventCollector.emit(Constants.ALERT_STREAM_ID, eventContainer,
				new Values(outputEvent, ruleId, actionId, target, mediaType,
						outputEvent.getHeaders().get(Constants.FIELD_RULE_GROUP),
						outputEvent.getHeaders().get(Constants.FIELD_TIMESTAMP)));
	}

	@Override
	public void handleRuleNoMatch(OutputCollector eventCollector, Tuple eventContainer, Event inputEvent, Rule rule) {
		ruleNoHitCount.scope(String.valueOf(rule.getRuleId())).incr();
	}

	@Override
	public void reportConditionEfficiency(String ruleGroup, short ruleId, long executeTime) {
		conditionEfficiency.scope(Utils.concat(ruleGroup, TENANTID_SEPARATOR, String.valueOf(ruleId)))
				.update(executeTime);
	}

	@Override
	public void reportRuleEfficiency(String ruleGroup, short ruleId, long executeTime) {
		ruleEfficiency.scope(Utils.concat(ruleGroup, TENANTID_SEPARATOR, String.valueOf(ruleId))).update(executeTime);
	}

	@Override
	public void reportRuleHit(String ruleGroup, short ruleId) {
		ruleHitCount.scope(Utils.concat(ruleGroup, TENANTID_SEPARATOR, String.valueOf(ruleId))).incr();
	}

	@Override
	public void emitActionErrorEvent(OutputCollector collector, Tuple eventContainer, Event actionErrorEvent) {
		StormContextUtil.emitErrorTuple(collector, eventContainer, RulesEngineBolt.class, gson.toJson(actionErrorEvent),
				"Rule action failed to fire", null);
	}

	/**
	 * @return the rulesEngine
	 */
	public StatelessRulesEngine<Tuple, OutputCollector> getRulesEngine() {
		return rulesEngine;
	}

	@Override
	public void emitAggregationEvent(Class<? extends Action> action, OutputCollector eventCollector,
			Tuple eventContainer, Event originalEvent, Long timestamp, int windowSize, String ruleActionId,
			String aggregationKey, Object aggregationValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void emitNewEvent(OutputCollector eventCollector, Tuple eventContainer, Event originalEvent,
			Event outputEvent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void emitTaggedEvent(OutputCollector eventCollector, Tuple eventContainer, Event outputEvent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void emitAnomalyAction(OutputCollector eventCollector, Tuple eventContainer, String seriesName,
			Number value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void emitTemplatedAlert(OutputCollector eventCollector, Tuple eventContainer, Event outputEvent,
			String ruleGroup, Short ruleId, Short actionId, String ruleName, Short templateId, Long timestamp) {
		eventCollector.emit(Constants.ALERT_STREAM_ID, eventContainer, new Values(outputEvent, ruleId, actionId,
				ruleName, templateId, outputEvent.getHeaders().get(Constants.FIELD_RULE_GROUP), timestamp));
	}

	@Override
	public void emitOmegaActions(OutputCollector eventCollector, Tuple eventContainer, String ruleGroup, long timestamp, short ruleId,
			short s, Event outputEvent) {
		throw new UnsupportedOperationException();
	}

}
