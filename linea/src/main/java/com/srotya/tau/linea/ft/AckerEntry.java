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
package com.srotya.tau.linea.ft;

import java.io.Serializable;

/**
 * 
 * 
 * @author ambud
 */
public class AckerEntry implements Serializable {

	private static final long serialVersionUID = 1L;
	private String sourceSpout;
	private Integer sourceTaskId;
	private Long createTime;
	private Long value;

	public AckerEntry() {
		createTime = System.currentTimeMillis();
	}

	public AckerEntry(String sourceSpout, Integer sourceTaskId, Long value) {
		this();
		this.sourceSpout = sourceSpout;
		this.sourceTaskId = sourceTaskId;
		this.value = value;
	}

	/**
	 * @return the sourceSpout
	 */
	public String getSourceSpout() {
		return sourceSpout;
	}

	/**
	 * @param sourceSpout
	 *            the sourceSpout to set
	 */
	public void setSourceSpout(String sourceSpout) {
		this.sourceSpout = sourceSpout;
	}

	/**
	 * @return the sourceTaskId
	 */
	public Integer getSourceTaskId() {
		return sourceTaskId;
	}

	/**
	 * @param sourceTaskId
	 *            the sourceTaskId to set
	 */
	public void setSourceTaskId(Integer sourceTaskId) {
		this.sourceTaskId = sourceTaskId;
	}

	/**
	 * @return the value
	 */
	public Long getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(Long value) {
		this.value = value;
	}

	/**
	 * @return the createTime
	 */
	public Long getCreateTime() {
		return createTime;
	}

	/**
	 * @return
	 */
	public boolean isComplete() {
		return value == 0;
	}

	@Override
	public String toString() {
		return "{" + value + "," + createTime + "}";
	}
}
