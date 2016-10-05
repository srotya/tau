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
package com.srotya.tau.api.security;

import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

/**
 * Custom authorization filter to integrate with internal gateway
 * 
 * @author ambud_sharma
 */
@Priority(1000)
public class BapiAuthorizationFilter implements ContainerRequestFilter {

	public static final String ROLE_RULE_GROUP_SEPARATOR = "_";
	public static final String ROLE_PREFIX = "Role";
	private static final String TENANTS = "tenants";
	private static final String BAPI = "bapi";
	public static final String USERNAME = "Username";
	public static final String USER_GROUP = "Group";
	public static final String USER_ROLE = ROLE_PREFIX;
	private static final Logger logger = Logger.getLogger(BapiAuthorizationFilter.class.getName());
	private static final String SUPERADMIN_GROUP = System.getenv().getOrDefault("SUPERADMIN_GROUP", "superadmin");

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String path = requestContext.getUriInfo().getPath();
		if(path.startsWith("swagger") || path.startsWith("perf")) {
			return;
		}
		MultivaluedMap<String, String> headers = requestContext.getHeaders();
		if (!headers.containsKey(USERNAME)) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
			logger.severe("Invalid request:" + headers);
			return;
		}
		String username = headers.getFirst(USERNAME);
		Set<String> roles = null;
		logger.info("Authenticated request for path:" + path + " from user:" + username + " method:"
				+ requestContext.getRequest().getMethod().toLowerCase());
		if (path.startsWith("perf")) {
			roles = new HashSet<>();
			roles.add(ACLConstants.READER_ROLE);
		} else if (path.startsWith(TENANTS) && requestContext.getRequest().getMethod().toLowerCase().equals("post")) {
			// do nothing
		} else if (path.equals(TENANTS) && requestContext.getRequest().getMethod().toLowerCase().equals("get")) {
			roles = new HashSet<>();
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				if (entry.getKey().startsWith(ROLE_PREFIX)) {
					roles.add(ACLConstants.READER_ROLE);
				}
			}
		} else {
			String tenantId = path.split("/")[1];
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				if (entry.getKey().startsWith(ROLE_PREFIX)) {
					String[] splits = entry.getKey().split(ROLE_RULE_GROUP_SEPARATOR);
					if (splits.length == 2 && tenantId.equalsIgnoreCase(splits[1])) {
						roles = new HashSet<>(entry.getValue());
					}
				}
			}
		}
		if (headers.containsKey(USER_GROUP)) {
			Set<String> groups = new HashSet<>(headers.get(USER_GROUP));
			if (groups.contains(SUPERADMIN_GROUP)) {
				if(roles==null) {
					roles = new HashSet<>();
				}
				roles.add(ACLConstants.SUPER_ADMIN_ROLE);
			}
		}
		if (roles == null || roles.isEmpty()) {
			requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());
			logger.severe("Request not authorized, roles are empty. Headers:" + headers);
			return;
		}
		requestContext.setSecurityContext(new BapiSecurityContext(username, roles));
	}

	public static boolean containsRole(MultivaluedMap<String, String> headers) {
		Set<String> keySet = headers.keySet();
		for (String key : keySet) {
			if (key.startsWith(USER_ROLE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Security context to bridge with dynamic RBAC enforcement by Jersey
	 * 
	 * @author ambud_sharma
	 */
	public static class BapiSecurityContext implements SecurityContext {
		private Set<String> roles;
		private String username;

		protected BapiSecurityContext(String username, final Set<String> roles) {
			this.username = username;
			this.roles = roles;
		}

		@Override
		public Principal getUserPrincipal() {
			return new UserPrincipal(username);
		}

		@Override
		public boolean isUserInRole(String role) {
			return roles.contains(role);
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public String getAuthenticationScheme() {
			return BAPI;
		}
	}

}