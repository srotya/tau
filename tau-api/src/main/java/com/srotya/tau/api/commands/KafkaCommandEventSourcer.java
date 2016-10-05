package com.srotya.tau.api.commands;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.gson.Gson;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.rules.RuleCommand;

public class KafkaCommandEventSourcer implements CommandEventSourcer {

	private static final String TEMPLATE_TOPIC_NAME = "template.topic.name";
	private static final String RULE_TOPIC_NAME = "rule.topic.name";
	private String ruleTopicName;
	private String templateTopicName;
	private KafkaProducer<String, String> producer;
	private ApplicationManager am;

	public KafkaCommandEventSourcer() {
	}

	@Override
	public void sendRule(boolean delete, String ruleGroupId, String rule) throws IOException {
		RuleCommand cmd = new RuleCommand(ruleGroupId, delete, rule);
		String cmdJson = new Gson().toJson(cmd);
		try {
			producer.send(new ProducerRecord<String, String>(ruleTopicName, cmdJson)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void sendTemplate(boolean delete, String ruleGroupId, String template) throws IOException {
		TemplateCommand cmd = new TemplateCommand(ruleGroupId, delete, template);
		String cmdJson = new Gson().toJson(cmd);
		try {
			producer.send(new ProducerRecord<String, String>(templateTopicName, cmdJson)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException(e);
		}
	}

	/**
	 * @param ruleTopicName
	 *            the ruleTopicName to set
	 */
	public void setRuleTopicName(String ruleTopicName) {
		this.ruleTopicName = ruleTopicName;
	}

	/**
	 * @param templateTopicName
	 *            the templateTopicName to set
	 */
	public void setTemplateTopicName(String templateTopicName) {
		this.templateTopicName = templateTopicName;
	}

	/**
	 * @param producer
	 *            the producer to set
	 */
	public void setProducer(KafkaProducer<String, String> producer) {
		this.producer = producer;
	}

	@Override
	public void init() throws Exception {
		ruleTopicName = am.getConfig().getProperty(RULE_TOPIC_NAME, "ruleTopic");
		templateTopicName = am.getConfig().getProperty(TEMPLATE_TOPIC_NAME, "templateTopic");
		producer = new KafkaProducer<>(am.getConfig());
	}

	@Override
	public void setApplicationManager(ApplicationManager am) {
		this.am = am;
	}

}
