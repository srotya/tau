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
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;

/**
 * Persistence manager for {@link Tenant}s
 * 
 * @author ambud_sharma
 */
public class TemplateManager {

	private static final Logger logger = Logger.getLogger(TemplateManager.class.getCanonicalName());
	private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();
	private static final String TEMPLATE_URL = "/templates";
	private ApplicationManager am;

	private TemplateManager() {
	}

	public static TemplateManager getInstance() {
		return TEMPLATE_MANAGER;
	}

	public void init(ApplicationManager am) {
		this.am = am;
	}

	public short createTemplate(UserBean ub, Tenant tenant) throws Exception {
		if (tenant == null) {
			throw new NullPointerException("Template can't be empty");
		}
		try {
			CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(),
					am.getRequestTimeout());
			HttpPost post = new HttpPost(am.getBaseUrl() + TEMPLATE_URL + "/" + tenant.getTenantId());
			if(am.isEnableAuth()) {
				post.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
				post.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
			}
			CloseableHttpResponse resp = client.execute(post);
			if (!Utils.validateStatus(resp)) {
				throw new Exception("status code:" + resp.getStatusLine().getStatusCode());
			}
			return Short.parseShort(EntityUtils.toString(resp.getEntity()));
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to create template:" + tenant + "\t" + e.getMessage());
			throw e;
		}
	}

	public AlertTemplate deleteTemplate(UserBean ub, String tenantId, short templateId) throws Exception {
		AlertTemplate template = getTemplate(ub, tenantId, templateId);
		if (template != null) {
			try {
				CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(),
						am.getRequestTimeout());
				HttpDelete delete = new HttpDelete(am.getBaseUrl() + TEMPLATE_URL + "/" + tenantId + "/" + templateId);
				if(am.isEnableAuth()) {
					delete.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
					delete.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
				}
				CloseableHttpResponse resp = client.execute(delete);
				if (!Utils.validateStatus(resp)) {
					throw new Exception("status code:" + resp.getStatusLine().getStatusCode());
				}
				return template;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to delete tenant:" + template, e);
				throw e;
			}
		} else {
			throw new NotFoundException("Tenant not found");
		}
	}

	public AlertTemplate updateTemplate(UserBean ub, String tenantId, AlertTemplate template) throws Exception {
		if (template != null) {
			try {
				CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(),
						am.getRequestTimeout());
				HttpPut put = new HttpPut(
						am.getBaseUrl() + TEMPLATE_URL + "/" + tenantId + "/" + template.getTemplateId());
				if(am.isEnableAuth()) {
					put.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
					put.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
				}
				StringEntity entity = new StringEntity(AlertTemplateSerializer.serialize(template, false),
						ContentType.APPLICATION_JSON);
				put.setEntity(entity);
				CloseableHttpResponse resp = client.execute(put);
				if (!Utils.validateStatus(resp)) {
					throw new Exception("status code:" + resp.getStatusLine().getStatusCode());
				}
				template.setTemplateId(Short.parseShort(EntityUtils.toString(resp.getEntity())));
				return template;
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to update tenant:" + template, e);
				throw e;
			}
		} else {
			throw new NotFoundException("Tenant not found");
		}
	}

	public AlertTemplate getTemplate(UserBean ub, String tenantId, short templateId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpGet get = new HttpGet(am.getBaseUrl() + TEMPLATE_URL + "/" + tenantId + "/" + templateId);
		if(am.isEnableAuth()) {
			get.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
			get.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
		}
		CloseableHttpResponse resp = client.execute(get);
		if (Utils.validateStatus(resp)) {
			String result = EntityUtils.toString(resp.getEntity());
			Gson gson = new Gson();
			return gson.fromJson(result, AlertTemplate.class);
		} else {
			throw new Exception("Template not found");
		}
	}

	public List<AlertTemplate> getTemplates(UserBean ub, String tenantId) throws Exception {
		CloseableHttpClient client = Utils.buildClient(am.getBaseUrl(), am.getConnectTimeout(), am.getRequestTimeout());
		HttpGet get = new HttpGet(am.getBaseUrl() + TEMPLATE_URL + "/" + tenantId);
		if(am.isEnableAuth()) {
			get.addHeader(BapiLoginDAO.X_SUBJECT_TOKEN, ub.getToken());
			get.addHeader(BapiLoginDAO.HMAC, ub.getHmac());
		}
		CloseableHttpResponse resp = client.execute(get);
		if (Utils.validateStatus(resp)) {
			String result = EntityUtils.toString(resp.getEntity());
			return Arrays.asList(AlertTemplateSerializer.deserializeArray(result));
		} else {
			throw new Exception("Templates not found");
		}
	}

}