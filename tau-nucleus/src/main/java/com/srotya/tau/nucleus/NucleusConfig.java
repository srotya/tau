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
package com.srotya.tau.nucleus;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

/**
 * @author ambudsharma
 */
public class NucleusConfig extends Configuration {
	
	@JsonProperty
	private boolean integrationTest = false;
	@JsonProperty
	@NotNull
	private String ingresserFactoryClass = "com.srotya.tau.nucleus.ingress.FSIngresserFactory";
	@JsonProperty
	@NotNull
	private String ingresserFactoryConfiguration;
	@JsonProperty
	private int ingresserParallelism = 1;
	@JsonProperty
	@NotNull
	private String ruleEngineConfiguration;
	@JsonProperty
	private int ruleEngineParallelism = 1;
	private int alertEngineParallelism = 1;
	
	public NucleusConfig() {
	}

	/**
	 * @return the ingresserFactoryClass
	 */
	public String getIngresserFactoryClass() {
		return ingresserFactoryClass;
	}

	/**
	 * @param ingresserFactoryClass the ingresserFactoryClass to set
	 */
	public void setIngresserFactoryClass(String ingresserFactoryClass) {
		this.ingresserFactoryClass = ingresserFactoryClass;
	}

	/**
	 * @return the ingresserFactoryConfiguration
	 */
	public String getIngresserFactoryConfiguration() {
		return ingresserFactoryConfiguration;
	}

	/**
	 * @param ingresserFactoryConfiguration the ingresserFactoryConfiguration to set
	 */
	public void setIngresserFactoryConfiguration(String ingresserFactoryConfiguration) {
		this.ingresserFactoryConfiguration = ingresserFactoryConfiguration;
	}

	/**
	 * @return the ingresserParallelism
	 */
	public int getIngresserParallelism() {
		return ingresserParallelism;
	}

	/**
	 * @param ingresserParallelism the ingresserParallelism to set
	 */
	public void setIngresserParallelism(int ingresserParallelism) {
		this.ingresserParallelism = ingresserParallelism;
	}

	/**
	 * @return the ruleEngineConfiguration
	 */
	public String getRuleEngineConfiguration() {
		return ruleEngineConfiguration;
	}

	/**
	 * @param ruleEngineConfiguration the ruleEngineConfiguration to set
	 */
	public void setRuleEngineConfiguration(String ruleEngineConfiguration) {
		this.ruleEngineConfiguration = ruleEngineConfiguration;
	}

	/**
	 * @return the ruleEngineParallelism
	 */
	public int getRuleEngineParallelism() {
		return ruleEngineParallelism;
	}

	/**
	 * @param ruleEngineParallelism the ruleEngineParallelism to set
	 */
	public void setRuleEngineParallelism(int ruleEngineParallelism) {
		this.ruleEngineParallelism = ruleEngineParallelism;
	}

	/**
	 * @return the alertEngineParallelism
	 */
	public int getAlertEngineParallelism() {
		return alertEngineParallelism;
	}

	/**
	 * @param alertEngineParallelism the alertEngineParallelism to set
	 */
	public void setAlertEngineParallelism(int alertEngineParallelism) {
		this.alertEngineParallelism = alertEngineParallelism;
	}

	/**
	 * @return the integrationTest
	 */
	public boolean isIntegrationTest() {
		return integrationTest;
	}

	/**
	 * @param integrationTest the integrationTest to set
	 */
	public void setIntegrationTest(boolean integrationTest) {
		this.integrationTest = integrationTest;
	}

}
