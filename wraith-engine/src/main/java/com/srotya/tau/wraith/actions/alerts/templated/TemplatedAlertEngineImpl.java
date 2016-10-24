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
package com.srotya.tau.wraith.actions.alerts.templated;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.velocity.tools.generic.DateTool;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.store.StoreFactory;
import com.srotya.tau.wraith.store.TemplateStore;

/**
 * @author ambudsharma
 */
public class TemplatedAlertEngineImpl implements TemplatedAlertEngine {

	private static final Logger logger = Logger.getLogger(TemplatedAlertEngine.class.getName());
	private static final String VELOCITY_VAR_DATE = "date";
	private Map<Short, VelocityAlertTemplate> templateMap;
	private StoreFactory storeFactory;
	private RuntimeServices runtimeServices;

	public TemplatedAlertEngineImpl(StoreFactory storeFactory) {
		this.storeFactory = storeFactory;
		this.templateMap = new HashMap<>();
	}

	@Override
	public Alert materialize(Map<String, Object> eventHeader, String ruleGroup, short ruleId, short actionId, String ruleName,
			short templateId, long timestamp) {
		Alert alert = new Alert();
		VelocityAlertTemplate template = templateMap.get(templateId);
		if (template != null) {
			long time = System.nanoTime();
			VelocityContext ctx = new VelocityContext();
			for (Entry<String, Object> entry : eventHeader.entrySet()) {
				ctx.put(entry.getKey(), entry.getValue());
			}
			ctx.put(VELOCITY_VAR_DATE, new DateTool());
			StringWriter writer = new StringWriter(1000);
			template.getVelocityBodyTemplate().merge(ctx, writer);
			alert.setBody(writer.toString());
			if (template.getSubject() == null) {
				alert.setSubject(ruleName);
			} else {
				writer = new StringWriter(1000);
				template.getVelocitySubjectTemplate().merge(ctx, writer);
				alert.setSubject(writer.toString());
			}
			alert.setTarget(template.getDestination());
			alert.setMedia(template.getMedia());
			alert.setId(template.getTemplateId());
			alert.setRuleId(ruleId);
			alert.setTimestamp(timestamp);
			alert.setRuleGroup(ruleGroup);
			time = System.nanoTime() - time;
			return alert;
		} else {
			return null;
		}
	}

	@Override
	public void updateTemplate(String ruleGroup, String templateJson, boolean delete) {
		try {
			AlertTemplate template = AlertTemplateSerializer.deserialize(templateJson);
			if (delete) {
				templateMap.remove(template.getTemplateId());
			} else {
				buildTemplateMap(runtimeServices, templateMap, template);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Alert template error", e);
		}
	}

	@Override
	public void initialize(Map<String, String> conf) throws Exception {
		this.runtimeServices = RuntimeSingleton.getRuntimeServices();
		try {
			initializeTemplates(runtimeServices, templateMap, storeFactory, conf);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Connects to {@link TemplateStore} and fetches all the templates and loads
	 * them into the memory {@link Map} for quick lookups
	 * 
	 * @param conf
	 * @throws Exception
	 */
	public static void initializeTemplates(RuntimeServices runtimeServices,
			Map<Short, VelocityAlertTemplate> templateMap, StoreFactory storeFactory, Map<String, String> conf)
			throws Exception {
		Properties props = new Properties();
		props.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogChute");

		Velocity.init(props);
		TemplateStore store = null;
		try {
			System.out.println(conf.get(Constants.TSTORE_TYPE));
			store = storeFactory.getTemplateStore(conf.get(Constants.TSTORE_TYPE), conf);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw e;
		}
		try {
			store.connect();
			Map<Short, AlertTemplate> templates = store.getAllTemplates();
			logger.info("Fetched " + templates.size() + " alert templates from the store");
			for (AlertTemplate template : templates.values()) {
				try {
					buildTemplateMap(runtimeServices, templateMap, template);
				} catch (ParseException e) {
					// TODO log ignore template
				}
			}
			store.disconnect();
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Builds {@link AlertTemplate} {@link Map} used for lookups.
	 * 
	 * @param template
	 * @throws ParseException
	 */
	public static void buildTemplateMap(RuntimeServices runtimeServices, Map<Short, VelocityAlertTemplate> templateMap,
			AlertTemplate template) throws ParseException {
		StringReader reader = new StringReader(template.getBody());
		SimpleNode node = runtimeServices.parse(reader, String.valueOf(template.getTemplateId()) + "_body");
		Template velocityTemplate = new Template();
		velocityTemplate.setRuntimeServices(runtimeServices);
		velocityTemplate.setData(node);
		velocityTemplate.initDocument();
		VelocityAlertTemplate alertTemplate = new VelocityAlertTemplate(template);
		alertTemplate.setVelocityBodyTemplate(velocityTemplate);

		if (template.getSubject() != null) {
			reader = new StringReader(template.getSubject());
			node = runtimeServices.parse(reader, String.valueOf(template.getTemplateId()) + "_subject");
			velocityTemplate = new Template();
			velocityTemplate.setRuntimeServices(runtimeServices);
			velocityTemplate.setData(node);
			velocityTemplate.initDocument();
			alertTemplate.setVelocitySubjectTemplate(velocityTemplate);
		}

		templateMap.put(template.getTemplateId(), alertTemplate);
	}

}
