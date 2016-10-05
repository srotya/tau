package com.srotya.tau.api.commands;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.gson.Gson;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.Utils;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.rules.RuleCommand;

public class APICommandEventSourcer implements CommandEventSourcer {
	
	private static final String RULE_URL = "/rules";
	private static final String TEMPLATE_URL = "/templates";
	private static final String DEFAULT_COMMAND_RECEIVER_URL = "http://localhost:8080/commands";
	private static final String COMMAN_RECEIVER_URL = "comman.receiver.url";
	private ApplicationManager am;
	private String url;

	public APICommandEventSourcer() {
	}

	@Override
	public void sendRule(boolean delete, String ruleGroupId, String ruleJson) throws IOException {
		try {
			CloseableHttpClient client = Utils.buildClient(url+RULE_URL, 5000, 10000);
			HttpPut put = new HttpPut(url+RULE_URL);
			RuleCommand cmd = new RuleCommand(ruleGroupId, delete, ruleJson);
			String json = new Gson().toJson(cmd);
			put.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
			CloseableHttpResponse response = client.execute(put);
			if(response.getStatusLine().getStatusCode()>=200 && response.getStatusLine().getStatusCode()<300) {
				response.close();	
			}else {
				throw new IOException("Rule not accepted by engine:"+response.getStatusLine().getStatusCode()+"\tURL:"+url+RULE_URL);
			}
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void sendTemplate(boolean delete, String ruleGroupId, String templateJson) throws IOException {
		try {
			CloseableHttpClient client = Utils.buildClient(url+TEMPLATE_URL, 5000, 10000);
			HttpPut put = new HttpPut(url+TEMPLATE_URL);
			TemplateCommand cmd = new TemplateCommand(ruleGroupId, delete, templateJson);
			String json = new Gson().toJson(cmd);
			put.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
			CloseableHttpResponse response = client.execute(put);
			if(response.getStatusLine().getStatusCode()>=200 && response.getStatusLine().getStatusCode()<300) {
				response.close();	
			}else {
				throw new IOException("Template not accepted by engine:"+response.getStatusLine().getStatusCode());
			}
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void init() throws Exception {
		url = am.getConfig().getOrDefault(COMMAN_RECEIVER_URL, DEFAULT_COMMAND_RECEIVER_URL).toString();
	}

	@Override
	public void setApplicationManager(ApplicationManager am) {
		this.am = am;
	}

}
