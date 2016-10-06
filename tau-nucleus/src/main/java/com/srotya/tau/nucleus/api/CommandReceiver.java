package com.srotya.tau.nucleus.api;

import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import com.srotya.tau.nucleus.DisruptorUnifiedFactory;
import com.srotya.tau.nucleus.processor.AbstractProcessor;
import com.srotya.tau.nucleus.processor.EmissionProcessor;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.rules.RuleCommand;

@Path("/commands")
public class CommandReceiver {

	private static final Logger logger = Logger.getLogger(CommandReceiver.class.getName());
	private AbstractProcessor ruleProcessor;
	private DisruptorUnifiedFactory factory;
	private AbstractProcessor alertProcessor;
	private EmissionProcessor emissionProcessor;

	public CommandReceiver(DisruptorUnifiedFactory factory, AbstractProcessor ruleProcessor,
			AbstractProcessor alertProcessor, EmissionProcessor emissionProcessor) {
		this.factory = factory;
		this.ruleProcessor = ruleProcessor;
		this.alertProcessor = alertProcessor;
		this.emissionProcessor = emissionProcessor;
	}

	@Path("/rules")
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	public void updateRule(RuleCommand command) {
		if (command.getRuleGroup() == null || command.getRuleGroup().isEmpty()) {
			throw new BadRequestException("Rule command must have a rule group");
		}
		if (command.getRuleContent() == null) {
			throw new BadRequestException("Rule content can't be null");
		}
		Event event = factory.buildEvent();
		event.getHeaders().put(Constants.FIELD_RULE_GROUP, command.getRuleGroup());
		event.getHeaders().put(Constants.FIELD_RULE_CONTENT, command.getRuleContent());
		event.getHeaders().put(Constants.FIELD_EVENT_TYPE, Constants.EVENT_TYPE_RULE_UPDATE);
		event.getHeaders().put(Constants.FIELD_RULE_DELETE, command.isDelete());
		logger.info("Rule command received:" + event);
		ruleProcessor.processEventNonWaled(event);
		emissionProcessor.processEventNonWaled(event);
	}

	@Path("/templates")
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	public void updateTemplate(TemplateCommand command) {
		if (command.getRuleGroup() == null || command.getRuleGroup().isEmpty()) {
			throw new BadRequestException("Template command must have a rule group");
		}
		if (command.getTemplateContent() == null) {
			throw new BadRequestException("Template content can't be null");
		}
		Event event = factory.buildEvent();
		event.getHeaders().put(Constants.FIELD_RULE_GROUP, command.getRuleGroup());
		event.getHeaders().put(Constants.FIELD_TEMPLATE_CONTENT, command.getTemplateContent());
		event.getHeaders().put(Constants.FIELD_EVENT_TYPE, Constants.EVENT_TYPE_TEMPLATE_UPDATE);
		event.getHeaders().put(Constants.FIELD_TEMPLATE_DELETE, command.isDelete());
		logger.info("Template command received:" + event);
		alertProcessor.processEventNonWaled(event);
	}

}
