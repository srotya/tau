/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.wraith.actions.omega;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.Action;

/**
 * @author ambudsharma
 *
 */
public abstract class OmegaAction implements Action {
	
	public static enum LANGUAGE {
		Java, Javascript, JRuby, Jython
	}
	
	private static final long serialVersionUID = 1L;
	private short actionId;
	private LANGUAGE language;

	public OmegaAction(short actionId, LANGUAGE language) {
		this.actionId = actionId;
		this.language = language;
	}
	
	@Override
	public Event actOnEvent(Event inputEvent) {
		return inputEvent;
	}

	@Override
	public short getActionId() {
		return actionId;
	}
	
	@Override
	public void setActionId(short actionId) {
		this.actionId = actionId;
	}

	@Override
	public ACTION_TYPE getActionType() {
		return ACTION_TYPE.OMEGA;
	}

	/**
	 * @return the language
	 */
	public LANGUAGE getLanguage() {
		return language;
	}

}
