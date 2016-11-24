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
 * An {@link AbstractSimpleCondition} that is true if the numeric header field
 * is less than the specified value
 * 
 * @author ambud_sharma
 */
public class LessThanCondition extends NumericCondition {

	private static final long serialVersionUID = 1L;

	public LessThanCondition(String header, double value) {
		super(header, value);
	}

	@Override
	public boolean compare(Number number, Number val) {
		return number.doubleValue() < val.doubleValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getkey() + " < " + getValue();
	}

}
