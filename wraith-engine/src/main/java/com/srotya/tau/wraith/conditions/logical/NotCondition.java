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
package com.srotya.tau.wraith.conditions.logical;

import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.conditions.Condition;

/**
 * Not condition i.e. negates the results of a condition
 * 
 * @author ambud_sharma
 */
public class NotCondition implements Condition {

	private static final long serialVersionUID = 1L;
	private Condition condition;
	
	public NotCondition(Condition condition) {
		this.condition = condition;
	}

	@Override
	public boolean matches(Event event) {
		return !condition.matches(event);
	}

}
