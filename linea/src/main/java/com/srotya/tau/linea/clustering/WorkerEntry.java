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
package com.srotya.tau.linea.clustering;

import java.io.Serializable;
import java.net.InetAddress;

/**
 * @author ambud
 */
public class WorkerEntry implements Serializable {

	private static final long serialVersionUID = 1L;
	private InetAddress workerAddress;
	private long lastContactTimestamp;
	
	public WorkerEntry() {
	}
	
	public WorkerEntry(InetAddress workerAddress, long lastContactTimestamp) {
		this.workerAddress = workerAddress;
		this.lastContactTimestamp = lastContactTimestamp;
	}

	/**
	 * @return the workerAddress
	 */
	public InetAddress getWorkerAddress() {
		return workerAddress;
	}

	/**
	 * @param workerAddress the workerAddress to set
	 */
	public void setWorkerAddress(InetAddress workerAddress) {
		this.workerAddress = workerAddress;
	}

	/**
	 * @return the lastContactTimestamp
	 */
	public long getLastContactTimestamp() {
		return lastContactTimestamp;
	}

	/**
	 * @param lastContactTimestamp the lastContactTimestamp to set
	 */
	public void setLastContactTimestamp(long lastContactTimestamp) {
		this.lastContactTimestamp = lastContactTimestamp;
	}
	
}
