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
package com.srotya.tau.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.srotya.tau.api.security.BapiAuthorizationFilter;

import io.dropwizard.Configuration;

/**
 * Application configuration
 * 
 * @author ambud_sharma
 */
public class AppConfig extends Configuration {

	@JsonProperty
	private String name;
	@JsonProperty
	private boolean enableAuthorization = false;
	@JsonProperty
	private String authorizationFilter = BapiAuthorizationFilter.class.getCanonicalName();
	@JsonProperty
	private String tauConfig;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the enableAuthorization
	 */
	public boolean isEnableAuthorization() {
		return enableAuthorization;
	}

	/**
	 * @param enableAuthorization the enableAuthorization to set
	 */
	public void setEnableAuthorization(boolean enableAuthorization) {
		this.enableAuthorization = enableAuthorization;
	}

	/**
	 * @return the authorizationFilter
	 */
	public String getAuthorizationFilter() {
		return authorizationFilter;
	}

	/**
	 * @param authorizationFilter the authorizationFilter to set
	 */
	public void setAuthorizationFilter(String authorizationFilter) {
		this.authorizationFilter = authorizationFilter;
	}

	/**
	 * @return the tauConfig
	 */
	public String getTauConfig() {
		return tauConfig;
	}

	/**
	 * @param tauConfig the tauConfig to set
	 */
	public void setTauConfig(String tauConfig) {
		this.tauConfig = tauConfig;
	}

}