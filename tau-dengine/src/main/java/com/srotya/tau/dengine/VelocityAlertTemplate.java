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
package com.srotya.tau.dengine;

import org.apache.velocity.Template;

import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;

/**
 * 
 * 
 * @author ambud_sharma
 */
public class VelocityAlertTemplate extends AlertTemplate {

	private static final long serialVersionUID = 1L;
	
	private Template velocitySubjectTemplate;
	private Template velocityBodyTemplate;
	
	public VelocityAlertTemplate() {
	}
	
	public VelocityAlertTemplate(short templateId, String templateName, String destination, String media, String subject, String body,
			int throttleDuration, int throttleLimit) {
		super(templateId, templateName, destination, media, subject, body, throttleDuration, throttleLimit);
	}
	
	public VelocityAlertTemplate(AlertTemplate template) {
		setBody(template.getBody());
		setDestination(template.getDestination());
		setMedia(template.getMedia());
		setSubject(template.getSubject());
		setTemplateId(template.getTemplateId());
		setThrottleDuration(template.getThrottleDuration());
		setThrottleLimit(template.getThrottleLimit());
	}
	/**
	 * @return the velocitySubjectTemplate
	 */
	public Template getVelocitySubjectTemplate() {
		return velocitySubjectTemplate;
	}

	/**
	 * @param velocitySubjectTemplate the velocitySubjectTemplate to set
	 */
	public void setVelocitySubjectTemplate(Template velocitySubjectTemplate) {
		this.velocitySubjectTemplate = velocitySubjectTemplate;
	}

	/**
	 * @return the velocityBodyTemplate
	 */
	public Template getVelocityBodyTemplate() {
		return velocityBodyTemplate;
	}

	/**
	 * @param velocityBodyTemplate the velocityBodyTemplate to set
	 */
	public void setVelocityBodyTemplate(Template velocityBodyTemplate) {
		this.velocityBodyTemplate = velocityBodyTemplate;
	}

}
