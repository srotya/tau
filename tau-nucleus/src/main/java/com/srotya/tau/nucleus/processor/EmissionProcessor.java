package com.srotya.tau.nucleus.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.MutableInt;
import com.srotya.tau.wraith.PerformantException;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.AggregationAction;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.StatelessRulesEngine;
import com.srotya.tau.wraith.store.RulesStore;

public class EmissionProcessor extends AbstractProcessor {

	private static final Logger logger = Logger.getLogger(EmissionProcessor.class.getName());
	private ScheduledExecutorService es;

	/**
	 * @param outputProcessors
	 *            0 - State, 1 - Fine Count Aggregation, 2 - Coarse Count
	 *            Aggregation
	 */
	public EmissionProcessor(DisruptorUnifiedFactory factory, Map<String, String> conf,
			AbstractProcessor... outputProcessors) {
		super(factory, 1, 128, conf, outputProcessors);
	}

	public static class EmissionHandler implements EventHandler<Event> {

		private Map<String, Map<Short, Rule>> ruleGroupMap;
		private int tickCounter;
		private DisruptorUnifiedFactory factory;
		private AbstractProcessor[] processors;

		public EmissionHandler(DisruptorUnifiedFactory factory, AbstractProcessor[] processors) {
			this.factory = factory;
			this.processors = processors;
		}

		public void initialize(Map<String, String> conf) throws Exception {
			int hashSize = Integer
					.parseInt(conf.getOrDefault(Constants.RULE_HASH_INIT_SIZE, Constants.DEFAULT_RULE_HASH_SIZE));
			this.ruleGroupMap = new HashMap<>(hashSize);
			RulesStore store = null;
			try {
				store = factory.getRulesStore(conf.get(Constants.RSTORE_TYPE), conf);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				throw e;
			}
			try {
				store.connect();
				this.ruleGroupMap.putAll(store.listGroupedRules());
				store.disconnect();
			} catch (IOException e) {
				logger.severe("Failed to load rules from store, reason:" + e.getMessage());
				throw e;
			}
		}

		@Override
		public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
			Object type = event.getHeaders().get(Constants.FIELD_EVENT_TYPE);
			if (type != null && type.equals(Constants.EVENT_TYPE_RULE_UPDATE)) {
				updateRule(event.getHeaders().get(Constants.FIELD_RULE_GROUP).toString(),
						event.getHeaders().get(Constants.FIELD_RULE_CONTENT).toString(),
						((Boolean) event.getHeaders().get(Constants.FIELD_RULE_DELETE)));
				logger.info("Processed rule update:" + event.getHeaders().get(Constants.FIELD_RULE_CONTENT).toString());
			} else {
				tickCounter++;
				for (String ruleGroup : ruleGroupMap.keySet()) {
					Map<Short, Rule> map = ruleGroupMap.get(ruleGroup);
					for (Rule rule : map.values()) {
						sendEmissionsForRule(ruleGroup, rule);
					}
				}
			}
		}

		public void sendEmissionsForRule(String ruleGroup, Rule rule) {
			List<AggregationAction> aggregationActions = filterAggregationActions(rule);
			if (aggregationActions != null) {
				for (AggregationAction action : aggregationActions) {
					if (tickCounter % action.getTimeWindow() == 0) {
						String ruleActionId = Utils.combineRuleActionId(rule.getRuleId(), action.getActionId());
						Event event = factory.buildEvent();
						event.getHeaders().put(Constants.FIELD_EVENT_TYPE, Constants.EVENT_TYPE_EMISSION);
						event.getHeaders().put(Constants.FIELD_AGGREGATION_WINDOW, action.getTimeWindow());
						event.getHeaders().put(Constants.FIELD_RULE_GROUP, ruleGroup);
						event.getHeaders().put(Constants.FIELD_RULE_ACTION_ID, ruleActionId);
						switch (action.getClass().getSimpleName()) {
						case "StateAggregationAction":
							processors[0].processEventNonWaled(event);
							break;
						case "FineCountingAggregationAction":
							processors[1].processEventNonWaled(event);
							break;
						}
					}
				}
			}
		}

		public static List<AggregationAction> filterAggregationActions(Rule rule) {
			List<AggregationAction> actions = new ArrayList<>();
			for (Action action : rule.getActions()) {
				if (action instanceof AggregationAction) {
					actions.add((AggregationAction) action);
				}
			}
			return actions;
		}

		/**
		 * Update rule and trigger emissions for that rule so there are no
		 * orphan entries in aggregation maps since rule update may change the
		 * rule configuration.
		 * 
		 * @param tuple
		 * @param ruleGroup
		 * @param ruleJson
		 * @param delete
		 * @throws Exception
		 */
		public void updateRule(String ruleGroup, String ruleJson, boolean delete) throws Exception {
			if (ruleGroup == null) {
				throw new PerformantException("Supplied rule group is null");
			}
			Map<Short, Rule> ruleMap = ruleGroupMap.get(ruleGroup);
			if (ruleMap == null) {
				ruleMap = new LinkedHashMap<>(10);
				ruleGroupMap.put(ruleGroup, ruleMap);
			}
			Rule rule = StatelessRulesEngine.updateRuleMap(ruleMap, ruleJson, delete);
			sendEmissionsForRule(ruleGroup, rule);
		}
	}

	@Override
	public EventHandler<Event> instantiateAndInitializeHandler(int taskId, MutableInt parallelism,
			Map<String, String> conf, DisruptorUnifiedFactory factory) throws Exception {
		es = Executors.newScheduledThreadPool(1, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setDaemon(true);
				return thread;
			}
		});
		es.scheduleAtFixedRate(() -> {
			Event event = factory.buildEvent();
			event.getHeaders().put(Constants.FIELD_EVENT_TYPE, "-1");
			processEventNonWaled(event);
		}, 10, 1, TimeUnit.SECONDS);
		EmissionHandler handler = new EmissionHandler(factory, getOutputProcessors());
		handler.initialize(conf);
		return handler;
	}

	@Override
	public String getConfigPrefix() {
		return null;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
