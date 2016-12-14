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
package com.srotya.tau.wraith;

import java.io.Serializable;

/**
 * Mutable short for hash value edits
 * 
 * @author ambud_sharma
 */
public class MutableShort implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private short val;
	
	public MutableShort() {
	}
	
	public MutableShort(short val) {
		this.val = val;
	}
	
	public short incrementAndGet() {
		return ++val;
	}

	/**
	 * @return the val
	 */
	public short getVal() {
		return val;
	}

	/**
	 * @param val the val to set
	 */
	public void setVal(short val) {
		this.val = val;
	}
	
	@Override
	public String toString() {
		return String.valueOf(val);
	}
	
}