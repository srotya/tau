package com.srotya.tau.api.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.rules.RuleCommand;

public class FileCommandEventSourcer implements CommandEventSourcer {
	
	private static final String TAU_RULE_UPDATES_TXT = "~/tau/rule-updates.txt";
	private static final String TAU_TEMPLATE_UPDATES_TXT = "~/tau/template-updates.txt";

	@Override
	public void sendRule(boolean delete, String ruleGroupId, String rule) throws IOException {
		RuleCommand cmd = new RuleCommand(ruleGroupId, delete, rule);
		PrintWriter pr = new PrintWriter(
				new FileWriter(TAU_RULE_UPDATES_TXT.replaceFirst("^~", System.getProperty("user.home")), true));
		String cmdJson = new Gson().toJson(cmd);
		pr.println(cmdJson);
		pr.close();
	}

	@Override
	public void sendTemplate(boolean delete, String ruleGroupId, String template) throws IOException {
		TemplateCommand cmd = new TemplateCommand(ruleGroupId, delete, template);
		PrintWriter pr = new PrintWriter(
				new FileWriter(TAU_TEMPLATE_UPDATES_TXT.replaceFirst("^~", System.getProperty("user.home")), true));
		String cmdJson = new Gson().toJson(cmd);
		pr.println(cmdJson);
		pr.close();
	}

	@Override
	public void init() throws Exception {
	}

	@Override
	public void setApplicationManager(ApplicationManager am) {
	}

}
