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

import com.srotya.tau.wraith.conditions.AbstractSimpleCondition;

/**
 * An {@link AbstractSimpleCondition} to see if header value is greater than
 * equal to the rule value. <br>
 * <br>
 * Only numeric types are allowed.
 * 
 * @author ambud_sharma
 *
 */
public class GreaterThanEqualToCondition extends NumericCondition {

	private static final long serialVersionUID = 1L;

	public GreaterThanEqualToCondition(String header, double value) {
		super(header, value);
	}

	@Override
	public boolean compare(double number, double val) {
		return number >= val;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getkey() + " >= " + getValue();
	}

}
