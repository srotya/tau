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
package com.srotya.tau.ui.storage;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * The persistent class for the tenant database table.
 * 
 * @author ambud_sharma
 */
public class Tenant implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotNull(message="Tenant ID can't be empty")
	@Size(min = 1, max = 100, message="Tenant ID must be under 100 characters")
	private String tenant_id;

	@NotNull(message="Tenant name can't be empty")
	@Size(min = 1, max = 100, message="Tenant name must be under 100 characters")
	private String tenant_name;

	public Tenant() {
	}

	public String getTenantId() {
		return this.tenant_id;
	}

	public void setTenantId(String tenantId) {
		this.tenant_id = tenantId;
	}

	public String getTenantName() {
		return this.tenant_name;
	}

	public void setTenantName(String tenantName) {
		this.tenant_name = tenantName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Tenant [tenantId=" + tenant_id + ", tenantName=" + tenant_name + ", ";
	}

}