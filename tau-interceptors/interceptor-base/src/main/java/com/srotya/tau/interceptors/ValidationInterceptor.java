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
package com.srotya.tau.interceptors;

import java.util.Map;

import com.google.gson.JsonObject;

public abstract class ValidationInterceptor {
	
	protected ValidationInterceptor next;
	
	public ValidationInterceptor() {
	}
	
	public abstract void configure(Map<String, String> config);
	
	public abstract void validate(JsonObject event) throws ValidationException;
	
	public abstract void validate(Map<String, Object> eventHeaders) throws ValidationException;

	/**
	 * @return the next
	 */
	public ValidationInterceptor getNext() {
		return next;
	}

	/**
	 * @param next the next to set
	 */
	public void setNext(ValidationInterceptor next) {
		this.next = next;
	}
	
}
