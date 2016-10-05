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
package com.srotya.tau.alerts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.dengine.UnifiedFactory;
import com.srotya.tau.dengine.Utils;
import com.srotya.tau.wraith.MutableBoolean;
import com.srotya.tau.wraith.MutableInt;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.store.StoreFactory;
import com.srotya.tau.wraith.store.TemplateStore;

import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * This bolt suppresses alerts by throttling policies enforced on templates. It
 * actively tracks trigger counts by template for a given tumbling time window
 * (measured in seconds).
 * 
 * @author ambud_sharma
 */
public class SuppressionBolt extends BaseRichBolt {

	private static final Logger logger = Logger.getLogger(SuppressionBolt.class.getName());
	private static final long serialVersionUID = 1L;
	private transient OutputCollector collector;
	private transient StoreFactory storeFactory;
	private transient Map<Short, AlertTemplate> templateMap;
	private transient Map<Short, MutableInt> counter;
	private transient Map<Short, MutableBoolean> stateMap;
	private transient long globalCounter = 1;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public final void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.templateMap = new HashMap<>();
		this.counter = new HashMap<>();
		this.stateMap = new HashMap<>();
		this.storeFactory = new UnifiedFactory();
		try {
			initTemplates(stormConf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		logger.info("Suppression bolt initialized");
	}

	/**
	 * Initialize templates
	 * 
	 * @param conf
	 * @throws Exception
	 */
	public void initTemplates(Map<String, String> conf) throws Exception {
		TemplateStore store = null;
		try {
			store = storeFactory.getTemplateStore(conf.get(Constants.TSTORE_TYPE), conf);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw e;
		}
		try {
			store.connect();
			Map<Short, AlertTemplate> temp = store.getAllTemplates();
			if (temp != null) {
				this.templateMap.putAll(temp);
			}
			logger.info("Fetched " + templateMap.size() + " alert templates from the store");
			store.disconnect();
		} catch (IOException e) {
			throw e;
		}
	}

	@Override
	public final void execute(Tuple tuple) {
		if (tuple.contains(Constants.FIELD_ALERT)) {
			Alert alert = (Alert) tuple.getValueByField(Constants.FIELD_ALERT);
			AlertTemplate template = templateMap.get(alert.getId());
			if (template != null) {
				MutableInt result = counter.get(alert.getId());
				if (result == null) {
					result = new MutableInt();
					counter.put(alert.getId(), result);
					stateMap.put(alert.getId(), new MutableBoolean());
				}
				if (result.incrementAndGet() <= template.getThrottleLimit() || template.getThrottleLimit() == 0) {
					collector.emit(Constants.DELIVERY_STREAM, tuple, new Values(alert));
				} else {
					// else just drop the alert and notify suppression monitor
					MutableBoolean state = stateMap.get(alert.getId());
					if (!state.isVal()) {
						collector.emit(Constants.SUP_MON_STREAM, tuple, new Values(alert.getId(), true));
						logger.fine("Entering suppression state for template:" + alert.getId());
						state.setVal(true);
					}
					logger.fine("Suppression alert for:" + alert.getId() + ":\t" + alert);
				}
			} else {
				logger.severe("Suppression policy not found for templateid:" + alert.getId());
				StormContextUtil.emitErrorTuple(collector, tuple, SuppressionBolt.class, tuple.toString(),
						"Suppression policy not found for templateid:" + alert.getId(), null);
			}
		} else if (Utils.isTickTuple(tuple)) {
			globalCounter++;
			logger.fine("Received tick tuple, gc:" + globalCounter);
			for (Entry<Short, AlertTemplate> entry : templateMap.entrySet()) {
				if (globalCounter % entry.getValue().getThrottleDuration() == 0
						&& counter.containsKey(entry.getKey())) {
					counter.get(entry.getKey()).setVal(0);
					MutableBoolean res = stateMap.get(entry.getKey());
					if (res.isVal()) {
						collector.emit(Constants.SUP_MON_STREAM, tuple, new Values(entry.getKey(), false));
						logger.fine("Leaving suppression state for template:" + entry.getKey());
						res.setVal(false);
					}
					logger.fine("Resetting suppression counters for:" + entry.getKey());
				}
			}
		} else if (Utils.isTemplateSyncTuple(tuple)) {
			logger.info(
					"Attempting to apply template update:" + tuple.getValueByField(Constants.FIELD_TEMPLATE_CONTENT));
			TemplateCommand templateCommand = (TemplateCommand) tuple.getValueByField(Constants.FIELD_TEMPLATE_CONTENT);
			try {
				logger.info("Received template tuple with template content:" + templateCommand.getTemplateContent());
				updateTemplate(templateCommand.getRuleGroup(), templateCommand.getTemplateContent(),
						templateCommand.isDelete());
				logger.info("Applied template update with template content:" + templateCommand.getTemplateContent());
			} catch (Exception e) {
				// failed to update rule
				StormContextUtil.emitErrorTuple(collector, tuple, SuppressionBolt.class, tuple.toString(),
						"Failed to apply rule update", e);
			}
		}
		collector.ack(tuple);
	}

	/**
	 * Update templates
	 * 
	 * @param ruleGroup
	 * @param templateJson
	 * @param delete
	 */
	public void updateTemplate(String ruleGroup, String templateJson, boolean delete) {
		try {
			AlertTemplate template = AlertTemplateSerializer.deserialize(templateJson);
			if (delete) {
				templateMap.remove(template.getTemplateId());
				logger.info("Deleted template:"+template.getTemplateId());
			} else {
				templateMap.put(template.getTemplateId(), template);
			}
		} catch (Exception e) {
			 logger.log(Level.SEVERE, "Alert template error", e);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream(Constants.SUP_MON_STREAM,
				new Fields(Constants.FIELD_ALERT_TEMPLATE_ID, Constants.SUPRESSION_STATE));
		declarer.declareStream(Constants.DELIVERY_STREAM, new Fields(Constants.FIELD_ALERT));
		StormContextUtil.declareErrorStream(declarer);
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
	 * @return the templateMap
	 */
	protected Map<Short, AlertTemplate> getTemplateMap() {
		return templateMap;
	}

	/**
	 * @return the counter
	 */
	protected Map<Short, MutableInt> getCounter() {
		return counter;
	}

	/**
	 * @return the globalCounter
	 */
	protected long getGlobalCounter() {
		return globalCounter;
	}

}
