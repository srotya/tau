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
package com.srotya.tau.wraith.actions;

import java.io.Serializable;

import com.srotya.tau.wraith.Event;

/**
 * Blueprints for action the engine can take after an event matches condition
 * 
 * @author ambud_sharma
 */
public interface Action extends Serializable {
	
	public static enum ACTION_TYPE {
		AGGREGATION,
		STATE,
		TAG,
		NEW,
		RAW_ALERT,
		TEMPLATED_ALERT,
		OMEGA,
		ANOMD
	}

	/**
	 * Performs the action of the supplied event and returns this or another event 
	 * depending on the logic on the action
	 * 
	 * @param inputEvent
	 * @return event or null
	 */
	public Event actOnEvent(final Event inputEvent);
	
	/**
	 * @return type of this action
	 */
	public ACTION_TYPE getActionType();
	
	/**
	 * Getter for unique id of the action within a rule
	 * @return actionId
	 */
	public short getActionId();
	
	/**
	 * Setter for actionId
	 * @param actionId
	 */
	public void setActionId(short actionId);

}
