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
package com.srotya.tau.ui.rules;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.NotFoundException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.srotya.tau.ui.ApplicationManager;
import com.srotya.tau.ui.BapiLoginDAO;
import com.srotya.tau.ui.UserBean;
import com.srotya.tau.ui.Utils;
import com.srotya.tau.ui.storage.Tenant;

/**
 * Persistence manager for {@link Tenant}s
 * 
 * @author ambud_sharma
 */
public class TenantManager {

	private static final Logger logger = Logger.getLogger(TenantManager.class.getCanonicalName());
	private static final TenantManager TENANT_MANAGER = new TenantManager();
	private static final String TENANT_URL = "/tenants";
	private ApplicationManager am;

	private TenantManager() {
	}

	public static TenantManager getInstance() {
		return TENANT_MANAGER;
	}

	public void init(ApplicationManager am) {
		this.am = am;
	}

	public void createTenant(UserBean ub, Tenant tenant) throws Exception {
		if (tenant == null) {
			throw new NullPointerException("Tenant can't be empty");
		}
		try {
			Gson gson = new Gson();
			CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(),
					am.getRequestTimeout());
			StringEntity entity = new StringEntity(gson.toJson(tenant), ContentType.APPLICATION_JSON);
			HttpPost post = new HttpPost(am.getBaseUrl() + TENANT_URL);
			post.setEntity(entity);
			if(am.isEnableAuth()) {
				post.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
				post.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
			}
			CloseableHttpResponse resp = client.execute(post);
			if (!Utils.validateStatus(resp)) {
				throw new Exception("status code:"+resp.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to create tenant:" + tenant + "\t" + e.getMessage());
			throw e;
		}
	}

	public Tenant deleteTenant(UserBean ub, String tenantId) throws Exception {
		Tenant tenant = getTenant(ub, tenantId);
		if (tenant != null) {
			try {
				CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
				HttpDelete delete = new HttpDelete(am.getBaseUrl() + TENANT_URL + "/" + tenantId);
				if(am.isEnableAuth()) {
					delete.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
					delete.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
				}
				CloseableHttpResponse resp = client.execute(delete);
				if (!Utils.validateStatus(resp)) {
					throw new Exception("status code:"+resp.getStatusLine().getStatusCode());
				}
				return tenant;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to delete tenant:" + tenant, e);
				throw e;
			}
		} else {
			throw new NotFoundException("Tenant not found");
		}
	}

	public Tenant updateTenant(UserBean ub, String tenantId, String tenantName) throws Exception {
		Tenant tenant = getTenant(ub, tenantId);
		if (tenant != null) {
			try {
				Gson gson = new Gson();
				tenant.setTenantName(tenantName);
				CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
				HttpPut put = new HttpPut(am.getBaseUrl() + TENANT_URL + "/" + tenantId);
				if(am.isEnableAuth()) {
					put.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
					put.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
				}
				StringEntity entity = new StringEntity(gson.toJson(tenant), ContentType.APPLICATION_JSON);
				put.setEntity(entity);
				CloseableHttpResponse resp = client.execute(put);
				if (!Utils.validateStatus(resp)) {
					throw new Exception("status code:"+resp.getStatusLine().getStatusCode());
				}
				return tenant;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to update tenant:" + tenant, e);
				throw e;
			}
		} else {
			throw new NotFoundException("Tenant not found");
		}
	}

	public Tenant getTenant(UserBean ub, String tenantId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpGet get = new HttpGet(am.getBaseUrl() + TENANT_URL + "/" + tenantId);
		if(am.isEnableAuth()) {
			get.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
			get.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
		}
		CloseableHttpResponse resp = client.execute(get);
		if(Utils.validateStatus(resp)) {
			String result = EntityUtils.toString(resp.getEntity());
			Gson gson = new Gson();
			return gson.fromJson(result, Tenant.class);
		}else {
			throw new Exception("Tenant not found");
		}
	}

	public List<Tenant> getTenants(UserBean ub) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpGet get = new HttpGet(am.getBaseUrl() + TENANT_URL);
		if(am.isEnableAuth()) {
			get.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
			get.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
		}
		CloseableHttpResponse resp = client.execute(get);
		if(Utils.validateStatus(resp)) {
			String result = EntityUtils.toString(resp.getEntity());
			Gson gson = new Gson();
			return Arrays.asList(gson.fromJson(result, Tenant[].class));
		}else {
			throw new Exception("Tenant not found:"+resp.toString()+"\t"+am.isEnableAuth()+"\thmac:"+ub.getHmac()+"\ttoken:"+ub.getToken());
		}
	}

}