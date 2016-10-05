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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map.Entry;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

/**
 * Auth bean
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "lb")
@SessionScoped
public class LoginBean implements Serializable {

	private static final long serialVersionUID = 1L;
	@ManagedProperty(value = "#{am}")
	private ApplicationManager am;
	private boolean authenticated;
	private String pwd;
	private String msg;
	private String user;
	private String token;
	private String hmac;

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	// validate login
	public String validateUsernamePassword() {
		String response = "unauthenticated";
		boolean valid = false;
		if (!am.isEnableAuth()) {
			valid = LoginDAO.authenticate(user, pwd);
		} else {
			System.out.println("Using Bapi Login module");
			try {
				Entry<String, String> result = BapiLoginDAO.authenticate(am.getAuthUrl(), user, pwd);
				if (result != null) {
					token = result.getKey();
					hmac = result.getValue();
					valid = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_FATAL,
						"Not Authorized", "Unable to perform authentication against Bapi"));
			}
		}
		if (valid) {
			if (authorize(user)) {
				HttpSession session = SessionUtil.getSession();
				session.setAttribute("username", user);
				response = "authenticated";
				authenticated = true;
			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
						"Not Authorized", "This account is not authorized to access any tenant"));
			}
		} else {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
					"Incorrect Username and Passowrd", "Please enter correct username and Password"));
		}
		this.pwd = "";
		System.out.println(response);
		return response;
	}

	// logout event, invalidate session
	public void logout() throws IOException {
		HttpSession session = SessionUtil.getSession();
		session.invalidate();
		FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
	}

	public boolean authorize(String user) {
		// EntityManager em = am.getEM();
		long groupCounts = 1;// ((Number)em.createNamedQuery("Tenant.findCountByUser").setParameter("user",
								// user).getSingleResult()).longValue();
		if (groupCounts < 1) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @return the authenticated
	 */
	public boolean isAuthenticated() {
		return authenticated;
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
