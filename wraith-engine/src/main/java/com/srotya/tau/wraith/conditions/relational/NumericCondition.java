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
package com.srotya.tau.wraith.conditions.relational;

import com.srotya.tau.wraith.Required;
import com.srotya.tau.wraith.conditions.AbstractSimpleCondition;

/**
 * An abstraction for numeric conditions
 * 
 * @author ambud_sharma
 */
public abstract class NumericCondition extends AbstractSimpleCondition {

	private static final long serialVersionUID = 1L;
	@Required
	private Number value = Double.MIN_VALUE;

	public NumericCondition(String headerKey, Number value) {
		super(headerKey);
		this.value = value;
	}

	public abstract boolean compare(Number number, Number val);

	@Override
	public final boolean satisfiesCondition(Object val) {
		if (val instanceof Number) {
			return compare(((Number) val).doubleValue(), getValue());
		}
		return false;
	}

	/**
	 * Getter for value
	 * @return the value
	 */
	public Number getValue() {
		return value;
	}

	/**
	 * Setter for value
	 * @param value
	 */
	public void setValue(Number value) {
		this.value = value;
	}

}
