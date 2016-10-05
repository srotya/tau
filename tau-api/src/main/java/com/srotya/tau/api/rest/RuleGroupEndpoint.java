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
package com.srotya.tau.api.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.security.ACLConstants;
import com.srotya.tau.api.security.BapiAuthorizationFilter;
import com.srotya.tau.api.storage.RuleGroup;
import com.wordnik.swagger.annotations.Api;

/**
 * REST endpoint for Rule Group operations
 * 
 * @author ambud_sharma
 */
@Path("/rulegroups")
@Api
public class RuleGroupEndpoint {

	public static final String RULE_GROUP_ID = "ruleGroupId";
	private static final Logger logger = Logger.getLogger(RuleGroupEndpoint.class.getName());
	private ApplicationManager am;

	public RuleGroupEndpoint(ApplicationManager am) {
		this.am = am;
	}

	/**
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.READER_ROLE, ACLConstants.OPERATOR_ROLE, ACLConstants.SUPER_ADMIN_ROLE,
			ACLConstants.SUPER_ADMIN_ROLE })
	public List<RuleGroup> listRuleGroups(@Context HttpHeaders headers) {
		if (am.getConfiguration().isEnableAuthorization()) {
			MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
			List<String> tList = new ArrayList<>();
			for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
				if (entry.getKey().startsWith(BapiAuthorizationFilter.ROLE_PREFIX)) {
					String[] split = entry.getKey().split(BapiAuthorizationFilter.ROLE_RULE_GROUP_SEPARATOR);
					if (split.length == 2) {
						tList.add(split[1]);
					}
				}
			}
			EntityManager em = am.getEM();
			try {
				return RuleGroupManager.getInstance().getRuleGroups(em, tList);
			} catch (Exception e) {
				throw new NotFoundException("No matching Rule Groups found");
			} finally {
				em.close();
			}
		} else {
			EntityManager em = am.getEM();
			try {
				return RuleGroupManager.getInstance().getRuleGroups(em);
			} catch (Exception e) {
				throw new NotFoundException("No Rule Groups found");
			} finally {
				em.close();
			}
		}
	}

	/**
	 * @param ruleGroupId
	 * @return
	 */
	@Path("/{" + RULE_GROUP_ID + "}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.READER_ROLE, ACLConstants.OPERATOR_ROLE, ACLConstants.SUPER_ADMIN_ROLE,
			ACLConstants.SUPER_ADMIN_ROLE })
	public RuleGroup getRuleGroup(
			@NotNull @PathParam(RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE, message = "Rule Group ID can't be empty") String ruleGroupId) {
		EntityManager em = am.getEM();
		try {
			return RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
		} catch (Exception e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("No Rule Group found").build());
		} finally {
			em.close();
		}
	}

	/**
	 * @param ruleGroup
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE })
	public void createRuleGroup(@NotNull(message = "Rule Group information can't be empty") RuleGroup ruleGroup) {
		if (!validateRuleGroup(ruleGroup)) {
			throw new BadRequestException("Rule Group info is invalid");
		}
		EntityManager em = am.getEM();
		try {
			if (RuleGroupManager.getInstance().getRuleGroup(em, ruleGroup.getRuleGroupId()) != null) {
				throw new BadRequestException(Response.status(400)
						.entity("Rule Group with Rule Group id:" + ruleGroup.getRuleGroupId() + " already exists").build());
			}
		} catch (NoResultException e) {
		} catch (Exception e) {
		}
		try {
			RuleGroupManager.getInstance().createRuleGroup(em, ruleGroup);
			logger.info("Created new Rule Group:" + ruleGroup);
		} catch (EntityExistsException e) {
			throw new BadRequestException(Response.status(400)
					.entity("Rule Group with Rule Group id:" + ruleGroup.getRuleGroupId() + " already exists").build());
		} catch (Exception e) {
			throw new BadRequestException(Response.status(400).entity(e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	/**
	 * @param ruleGroupId
	 */
	@Path("/{" + RULE_GROUP_ID + "}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE })
	public void deleteRuleGroup(
			@NotNull @PathParam(RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().deleteRuleGroup(em, ruleGroupId, am);
			logger.info("Deleted Rule Group:" + ruleGroup);
		} catch (Exception e) {
			throw new BadRequestException(Response.status(400).entity(e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	/**
	 * @param ruleGroupId
	 * @param ruleGroup
	 */
	@Path("/{" + RULE_GROUP_ID + "}")
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE })
	public void updateRuleGroup(
			@NotNull(message = "Rule Group ID can't be empty") @PathParam(RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull(message = "Rule Group information can't be empty") RuleGroup ruleGroup) {
		if (!validateRuleGroup(ruleGroup)) {
			throw new BadRequestException("Rule Group info is invalid");
		}
		EntityManager em = am.getEM();
		try {
			ruleGroup = RuleGroupManager.getInstance().updateRuleGroup(em, ruleGroupId, ruleGroup.getRuleGroupName());
			logger.info("Updated Rule Group:" + ruleGroup);
		} catch (Exception e) {
			if (e instanceof NoResultException) {
				throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("No Rule Groups found").build());
			} else {
				throw new BadRequestException(Response.status(400).entity(e.getMessage()).build());
			}
		} finally {
			em.close();
		}
	}

	public static boolean validateRuleGroup(RuleGroup ruleGroup) {
		if (ruleGroup == null || ruleGroup.getRuleGroupId() == null || ruleGroup.getRuleGroupName() == null
				|| ruleGroup.getRuleGroupId().isEmpty() || ruleGroup.getRuleGroupName().isEmpty()) {
			return false;
		}
		if (ruleGroup.getRuleGroupId().length() > RuleGroup.RULE_GROUP_ID_MAX_SIZE) {
			return false;
		}
		if (ruleGroup.getRuleGroupName().length() > RuleGroup.RULE_GROUP_NAME_MAX_SIZE) {
			return false;
		}
		return true;
	}

}
