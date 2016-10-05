package com.srotya.tau.dengine.bolts;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.srotya.tau.dengine.Constants;
import com.srotya.tau.dengine.StormContextUtil;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author ambud_sharma
 */
public class TemplateTranslatorBolt extends BaseRichBolt {

	private static final long serialVersionUID = 1L;
	private transient Logger logger;
	private transient OutputCollector collector;
	private transient Gson gson;
	private transient Type type;

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		this.logger = Logger.getLogger(RuleTranslatorBolt.class.getName());
		this.collector = collector;
		this.gson = new Gson();
		type = new TypeToken<TemplateCommand>() {
		}.getType();
		logger.info("Template Translator Bolt initialized");
	}

	@Override
	public void execute(Tuple input) {
		try {
			logger.info("Translating template command:"+input.getString(0));
			TemplateCommand templateCommandJson = gson.fromJson(input.getString(0), type);
			if (templateCommandJson != null) {
				collector.emit(Constants.SYNC_STREAM_ID, input, new Values(templateCommandJson));
			} else {
				throw new NullPointerException("Template command is null, unable to parse:" + input.getString(0));
			}
		} catch (Exception e) {
			System.err.println("Bad template update");
			StormContextUtil.emitErrorTuple(collector, input, JSONTranslatorBolt.class, "JSON to TemplateWrapper issue",
					input.getString(0), e);
		}
		collector.ack(input);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declareStream(Constants.SYNC_STREAM_ID, new Fields(Constants.FIELD_TEMPLATE_CONTENT));
		StormContextUtil.declareErrorStream(declarer);
	}

}
