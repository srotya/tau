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
package com.srotya.tau.api.storage;

import java.io.Serializable;

/**
 * @author ambud_sharma
 */
public class Point implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private Long key;
	private Number value;
	
	public Point() {
	}

	public Point(Long key, Number value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return the key
	 */
	public Long getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(Long key) {
		this.key = key;
	}

	/**
	 * @return the value
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Number value) {
		this.value = value;
	}

}
