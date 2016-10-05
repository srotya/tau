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
package com.srotya.tau.ui;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import com.srotya.tau.ui.rules.TenantManager;
import com.srotya.tau.ui.storage.Tenant;

/**
 * JSF User bean
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "ub")
@SessionScoped
public class UserBean implements Serializable {

	private static final long serialVersionUID = 1L;
	@ManagedProperty(value = "#{am}")
	private ApplicationManager am;
	@ManagedProperty(value = "#{lb}")
	private LoginBean lb;
	private String userId;
	private Tenant tenant;
	private List<Tenant> tenants;
	private String token;
	private String hmac;

	public UserBean() {
	}

	@PostConstruct
	public void init() {
		userId = lb.getUser();
		if (am.isEnableAuth()) {
			token = lb.getToken();
			hmac = lb.getHmac();
		}
		try {
			loadGroups();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void loadGroups() throws Exception {
		List<Tenant> results = TenantManager.getInstance().getTenants(this);
		if (results.size() > 0) {
			tenants = results;
			tenant = results.get(0);
			System.out.println("Loaded tenants:" + results + "\t" + tenant);
		} else {
			if (tenants != null) {
				tenants.clear();
			}
			System.out.println("No tenants found for this user");
		}
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId
	 *            the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the lb
	 */
	public LoginBean getLb() {
		return lb;
	}

	/**
	 * @param lb
	 *            the lb to set
	 */
	public void setLb(LoginBean lb) {
		this.lb = lb;
	}

	/**
	 * @return the am
	 */
	public ApplicationManager getAm() {
		return am;
	}

	/**
	 * @param am
	 *            the am to set
	 */
	public void setAm(ApplicationManager am) {
		this.am = am;
	}

	/**
	 * @return the ruleGroup
	 */
	public Tenant getTenant() {
		return tenant;
	}

	/**
	 * @param tenant
	 *            the ruleGroup to set
	 */
	public void setTenant(Tenant tenant) {
		this.tenant = tenant;
	}

	/**
	 * @return the ruleGroups
	 */
	public List<Tenant> getTenants() {
		return tenants;
	}

	/**
	 * @param tenants
	 *            the ruleGroups to set
	 */
	public void setTenants(List<Tenant> tenants) {
		this.tenants = tenants;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * @param token
	 *            the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * @return the hmac
	 */
	public String getHmac() {
		return hmac;
	}

	/**
	 * @param hmac
	 *            the hmac to set
	 */
	public void setHmac(String hmac) {
		this.hmac = hmac;
	}

}