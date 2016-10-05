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
package com.srotya.tau.wraith.actions.alerts;

import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.Required;
import com.srotya.tau.wraith.actions.Action;

/**
 * {@link Action} to generate Alerts
 * 
 * @author ambud_sharma
 */
public class AlertAction implements Action {

	private static final long serialVersionUID = 1L;
	@Required
	private short actionId;
	@Required
	private String target;
	@Required
	private String media;
	@Required
	private String body;
	
	public AlertAction(short actionId, String target, String media, String body) {
		this.actionId = actionId;
		this.target = target;
		this.media = media;
		this.body = body;
	}
	
	@Override
	public Event actOnEvent(Event inputEvent) {
		inputEvent.getHeaders().put(Constants.FIELD_ALERT_TARGET, target);
		inputEvent.getHeaders().put(Constants.FIELD_ALERT_MEDIA, media);
		return inputEvent;
	}
	
	@Override
	public ACTION_TYPE getActionType() {
		return ACTION_TYPE.RAW_ALERT;
	}

	@Override
	public short getActionId() {
		return actionId;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(short actionId) {
		this.actionId = actionId;
	}

	/**
	 * @return the target
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(String target) {
		this.target = target;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "AlertAction [actionId=" + actionId + ", target=" + target + ", media=" + media + ", body=" + body + "]";
	}

}
