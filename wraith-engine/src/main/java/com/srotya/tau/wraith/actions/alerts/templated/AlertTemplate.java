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

import java.io.Serializable;

import com.srotya.tau.wraith.Required;

/**
 * Pojo for alert templates
 * 
 * @author ambud_sharma
 */
public class AlertTemplate implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Required
	private short templateId = -1;
	@Required
	private String templateName;
	@Required
	private String destination;
	@Required
	private String media;
	private String subject;
	@Required
	private String body;
	// Default to 1 alert per 5 mins
	private int throttleDuration = 300;
	private int throttleLimit = 1;
	
	public AlertTemplate() {
	}
	
	public AlertTemplate(short templateId) {
		this.templateId = templateId;
	}

	public AlertTemplate(short templateId, String templateName, String destination, String media, String subject, String body,
			int throttleDuration, int throttleLimit) {
		this.templateId = templateId;
		this.templateName = templateName;
		this.destination = destination;
		this.media = media;
		this.subject = subject;
		this.body = body;
		this.throttleDuration = throttleDuration;
		this.throttleLimit = throttleLimit;
	}
	
	public AlertTemplate(short templateId, String templateName, String destination, String media, String subject, String body) {
		this.templateId = templateId;
		this.templateName = templateName;
		this.destination = destination;
		this.media = media;
		this.subject = subject;
		this.body = body;
	}
	/**
	 * @return the templateId
	 */
	public short getTemplateId() {
		return templateId;
	}
	/**
	 * @param templateId the templateId to set
	 */
	public void setTemplateId(short templateId) {
		this.templateId = templateId;
	}
	/**
	 * @return the templateName
	 */
	public String getTemplateName() {
		return templateName;
	}

	/**
	 * @param templateName the templateName to set
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}
	/**
	 * @param destination the destination to set
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}
	/**
	 * @return the media
	 */
	public String getMedia() {
		return media;
	}
	/**
	 * @param media the media to set
	 */
	public void setMedia(String media) {
		this.media = media;
	}
	/**
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}
	/**
	 * @param subject the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}
	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}
	/**
	 * @param body the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}
	/**
	 * @return the throttleDuration
	 */
	public int getThrottleDuration() {
		return throttleDuration;
	}
	/**
	 * @param throttleDuration the throttleDuration to set
	 */
	public void setThrottleDuration(int throttleDuration) {
		this.throttleDuration = throttleDuration;
	}
	/**
	 * @return the throttleLimit
	 */
	public int getThrottleLimit() {
		return throttleLimit;
	}
	/**
	 * @param throttleLimit the throttleLimit to set
	 */
	public void setThrottleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AlertTemplate [templateId=" + templateId + ", templateName=" + templateName + ", destination="
				+ destination + ", media=" + media + ", subject=" + subject + ", body=" + body + ", throttleDuration="
				+ throttleDuration + ", throttleLimit=" + throttleLimit + "]";
	}
	
}
