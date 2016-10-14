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
package com.srotya.tau.omega;

import java.io.Serializable;

/**
 * @author ambudsharma
 */
public class Code implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public static enum CODE_LOCATION {
		S3, URL
	}
	
	private CODE_LOCATION location;
	private String path;
	
	public Code(CODE_LOCATION location, String path) {
		this.location = location;
		this.path = path;
	}
	/**
	 * @return the location
	 */
	public CODE_LOCATION getLocation() {
		return location;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param location the location to set
	 */
	public void setLocation(CODE_LOCATION location) {
		this.location = location;
	}
	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	
}
