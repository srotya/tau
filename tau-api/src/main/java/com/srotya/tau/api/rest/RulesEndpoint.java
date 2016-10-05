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

import javax.annotation.security.RolesAllowed;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonParseException;
import com.srotya.tau.api.ApplicationManager;
import com.srotya.tau.api.Utils;
import com.srotya.tau.api.dao.RulesManager;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.security.ACLConstants;
import com.srotya.tau.api.storage.Rules;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.rules.Rule;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * REST endpoint for {@link Rule} CRUD operations
 * 
 * @author ambud_sharma
 */
@Path("/rulegroups/{"+RuleGroupEndpoint.RULE_GROUP_ID+"}/rules")
@Api
public class RulesEndpoint {

	private static final String RULE_ID = "ruleId";
	private ApplicationManager am;

	public RulesEndpoint(ApplicationManager am) {
		this.am = am;
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE,
			ACLConstants.READER_ROLE })
	@ApiOperation(value = "List rules", notes = "Will return list of rules for a given RuleGroup ID", response = Rule.class, responseContainer = "List")
	public String listRules(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@DefaultValue("false") @QueryParam("pretty") boolean pretty,
			@DefaultValue("0") @QueryParam("filter") int filter) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			return RulesManager.getInstance().getRuleContents(em, ruleGroup.getRuleGroupId(), pretty, filter);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error fetching rules:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Create rule", notes = "Will create an empty rule for a given Rule Group ID", response = Short.class)
	public String createRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		RulesManager mgr = RulesManager.getInstance();
		RuleGroup ruleGroup;
		EntityManager em = am.getEM();
		try {
			ruleGroup = mgr.getRuleGroup(em, ruleGroupId);
		} catch (Exception e) {
			em.close();
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Rule Group not found").build());
		}
		try {
			return RuleSerializer.serializeRuleToJSONString(mgr.createNewRule(em, new Rules(), ruleGroup), false);
		} catch (Exception e) {
			throw new InternalServerErrorException();
		} finally {
			em.close();
		}
	}

	@Path("/{" + RULE_ID + "}")
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Update rule", notes = "Will update rule logic for a given Rule Group ID and Rule ID", response = Short.class)
	public String putRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE, message = "Rule Group ID must be under 100 characters") String ruleGroupId,
			@NotNull(message = "Rule ID can't be empty") @PathParam(RULE_ID) short ruleId,
			@HeaderParam("Accept-Charset") @DefaultValue("utf-8") String encoding,
			@NotNull(message = "Rule JSON can't be empty") @Encoded String ruleJson) {
		EntityManager em = am.getEM();
		if (ruleJson != null && ruleJson.length() > Rules.MAX_RULE_LENGTH) {
			throw new BadRequestException(Response.status(Status.BAD_REQUEST).entity("Rule is too big").build());
		}
		if (!Utils.isCharsetMisInterpreted(ruleJson, encoding)) {
			throw new BadRequestException(
					Response.status(Status.BAD_REQUEST).entity("Rule JSON must be UTF-8 compliant").build());
		}

		RulesManager mgr = RulesManager.getInstance();
		RuleGroup ruleGroup;
		try {
			ruleGroup = mgr.getRuleGroup(em, ruleGroupId);
		} catch (Exception e) {
			em.close();
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Rule Group not found").build());
		}
		SimpleRule rule = null;
		try {
			rule = RuleSerializer.deserializeJSONStringToRule(ruleJson);
			if (rule == null) {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST).entity("Unable to parse rule").build());
			}
			if (rule.getRuleId() != ruleId) {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST).entity("Rule id in path doesn't match the rule").build());
			}
		} catch (BadRequestException e) {
			throw e;
		} catch (JsonParseException | IllegalStateException | NumberFormatException e) {
			em.close();
			if (e.getMessage().contains("NumberFormat") || (e instanceof NumberFormatException)) {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST)
								.entity("Invalid number "
										+ e.getLocalizedMessage().replace("java.lang.NumberFormatException", ""))
								.build());
			} else if (e.getMessage().contains("Malformed")) {
				throw new BadRequestException(Response.status(Status.BAD_REQUEST).entity("Invalid JSON").build());
			} else if (e.getMessage().contains("IllegalStateException")) {
				throw new BadRequestException(Response.status(Status.BAD_REQUEST)
						.entity("Expecting a singel rule object not an array").build());
			} else {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST).entity(e.getLocalizedMessage()).build());
			}
		}
		try {
			Rules ruleContainer = new Rules();
			if (rule.getRuleId() > 0) {
				try {
					Rules temp = mgr.getRule(em, ruleGroup.getRuleGroupId(), rule.getRuleId());
					if (temp != null) {
						ruleContainer = temp;
					}
				} catch (NoResultException e) {
					// rule doesn't exit, will save it as a new rule
				}
			}
			return RuleSerializer.serializeRuleToJSONString(mgr.saveRule(em, ruleContainer, ruleGroup, rule, am), false);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (ValidationException e) {
			throw new BadRequestException(Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
		} catch (Exception e) {
			throw new InternalServerErrorException(Response.serverError().entity(e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@Path("/{" + RULE_ID + "}/enable")
	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Enable rule", notes = "Enable rule for the supplied Rule Group ID and Rule ID", response = Rule.class)
	public String enableRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull @PathParam(RULE_ID) short ruleId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			return RulesManager.getInstance().enableDisableRule(em, true, ruleGroup.getRuleGroupId(), ruleId, am).getRuleContent();
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error enabling rule:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@Path("/{" + RULE_ID + "}/disable")
	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Disable rule", notes = "Disable rule for the supplied Rule Group ID and Rule ID", response = Rule.class)
	public String disableRule(@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) String ruleGroupId,
			@NotNull @PathParam(RULE_ID) short ruleId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			return RulesManager.getInstance().enableDisableRule(em, false, ruleGroup.getRuleGroupId(), ruleId, am).getRuleContent();
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error disabling rule:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@Path("/{" + RULE_ID + "}")
	@GET
	@Produces({ MediaType.TEXT_HTML })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE,
			ACLConstants.READER_ROLE })
	@ApiOperation(value = "Get rule", notes = "Get rule for the supplied Rule Group ID and Rule ID", response = Rule.class)
	public String getRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull @PathParam(RULE_ID) short ruleId, @DefaultValue("false") @QueryParam("pretty") boolean pretty) {
		Rules rule = null;
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			rule = RulesManager.getInstance().getRule(em, ruleGroup.getRuleGroupId(), ruleId);
		} catch (Exception e) {
			if (e instanceof NoResultException) {
				throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
			} else {
				throw new BadRequestException();
			}
		} finally {
			em.close();
		}
		if (rule.getRuleContent() != null) {
			if (pretty) {
				return Utils.getPrettyRuleJson(rule.getRuleContent());
			} else {
				return rule.getRuleContent();
			}
		} else {
			return RuleSerializer.serializeRuleToJSONString(
					new SimpleRule(rule.getRuleId(), "", false, null, new Action[] {}), pretty);
		}
	}

	@Path("/{" + RULE_ID + "}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Delete rule", notes = "Delete rule for the supplied Rule Group ID and Rule ID")
	public void deleteRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull @PathParam(RULE_ID) short ruleId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			RulesManager.getInstance().deleteRule(em, ruleGroup.getRuleGroupId(), ruleId, am);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error deleting rule:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE })
	@ApiOperation(value = "Delete all rules", notes = "Delete all rules for the supplied Rule Group ID")
	public void deleteAllRules(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			RulesManager.getInstance().deleteRules(em, ruleGroup, am);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error deleting all rules for rule group:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@Path("/disable")
	@PUT
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE })
	@ApiOperation(value = "Disable all rules", notes = "Disable all rules for the supplied Rule Group ID")
	public void disableAllRule(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			RulesManager.getInstance().disableAllRules(em, ruleGroup.getRuleGroupId(), am);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error disabling all rules for rule group:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

}