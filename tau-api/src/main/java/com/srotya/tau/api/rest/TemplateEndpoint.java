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
import com.srotya.tau.api.dao.TemplateManager;
import com.srotya.tau.api.dao.RuleGroupManager;
import com.srotya.tau.api.security.ACLConstants;
import com.srotya.tau.api.storage.AlertTemplates;
import com.srotya.tau.api.storage.RuleGroup;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * @author ambud_sharma
 */
@Path("/rulegroups/{"+RuleGroupEndpoint.RULE_GROUP_ID+"}/templates")
@Api
public class TemplateEndpoint {

	private static final String TEMPLATE_ID = "test";
	private ApplicationManager am;

	public TemplateEndpoint(ApplicationManager am) {
		this.am = am;
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE,
			ACLConstants.READER_ROLE })
	@ApiOperation(value = "List templates", notes = "List templates for the supplied Rule Group ID", response = AlertTemplate.class, responseContainer="List")
	public String listTemplates(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@DefaultValue("false") @QueryParam("pretty") boolean pretty) {
		EntityManager em = am.getEM();
		try {
			return TemplateManager.getInstance().getTemplateContents(em, ruleGroupId, pretty);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error fetching templates:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Create template", notes = "Create an empty for the supplied Rule Group ID", response = Short.class)
	public short createTemplate(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		TemplateManager mgr = TemplateManager.getInstance();
		RuleGroup ruleGroup;
		EntityManager em = am.getEM();
		try {
			ruleGroup = mgr.getRuleGroup(em, ruleGroupId);
		} catch (Exception e) {
			em.close();
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Rule Group not found").build());
		}
		try {
			return mgr.createNewTemplate(em, new AlertTemplates(), ruleGroup);
		} catch (Exception e) {
			throw new InternalServerErrorException();
		} finally {
			em.close();
		}
	}

	@Path("/{" + TEMPLATE_ID + "}")
	@PUT
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Update template", notes = "Update template for the supplied Rule Group ID and Template ID", response = Short.class)
	public short putTemplate(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE, message = "Rule Group ID must be under 100 characters") String ruleGroupId,
			@NotNull(message = "Template ID can't be empty") @PathParam(TEMPLATE_ID) short templateId,
			@HeaderParam("Accept-Charset") @DefaultValue("utf-8") String encoding,
			@NotNull(message = "Template JSON can't be empty") @Encoded String templateJson) {
		EntityManager em = am.getEM();
		if (templateJson != null && templateJson.length() > AlertTemplates.MAX_TEMPLATE_LENGTH) {
			throw new BadRequestException(Response.status(Status.BAD_REQUEST).entity("Template is too big").build());
		}

		if (!Utils.isCharsetMisInterpreted(templateJson, encoding)) {
			throw new BadRequestException(
					Response.status(Status.BAD_REQUEST).entity("Template JSON must be UTF-8 compliant").build());
		}

		TemplateManager mgr = TemplateManager.getInstance();
		RuleGroup ruleGroup;
		try {
			ruleGroup = mgr.getRuleGroup(em, ruleGroupId);
		} catch (Exception e) {
			em.close();
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Rule Group not found").build());
		}
		AlertTemplate template = null;
		try {
			template = AlertTemplateSerializer.deserialize(templateJson);
			if (template == null) {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST).entity("Unable to parse template").build());
			}
			if (template.getTemplateId() != templateId) {
				throw new BadRequestException(Response.status(Status.BAD_REQUEST)
						.entity("Template id in path doesn't match the template").build());
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
						.entity("Expecting a singel template object not an array").build());
			} else {
				throw new BadRequestException(
						Response.status(Status.BAD_REQUEST).entity(e.getLocalizedMessage()).build());
			}
		}
		try {
			AlertTemplates templateContainer = new AlertTemplates();
			if (template.getTemplateId() > 0) {
				try {
					AlertTemplates temp = mgr.getTemplate(em, ruleGroup.getRuleGroupId(), template.getTemplateId());
					if (temp != null) {
						templateContainer = temp;
					}
				} catch (NoResultException e) {
					// template doesn't exit, will save it as a new template
				}
			}
			return mgr.saveTemplate(em, templateContainer, ruleGroup, template, am);
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

	@Path("/{" + TEMPLATE_ID + "}")
	@GET
	@Produces({ MediaType.TEXT_HTML })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE,
			ACLConstants.READER_ROLE })
	@ApiOperation(value = "Get template", notes = "Get template for the supplied Rule Group ID and Template ID", response = AlertTemplate.class)
	public String getTemplate(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull @PathParam(TEMPLATE_ID) short templateId,
			@DefaultValue("false") @QueryParam("pretty") boolean pretty) {
		AlertTemplate template = null;
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			template = TemplateManager.getInstance().getTemplateObj(em, ruleGroup.getRuleGroupId(), templateId);
		} catch (Exception e) {
			if (e instanceof NoResultException) {
				throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
			} else {
				throw new BadRequestException();
			}
		} finally {
			em.close();
		}
		return AlertTemplateSerializer.serialize(template, pretty);
	}

	@Path("/{" + TEMPLATE_ID + "}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE, ACLConstants.OPERATOR_ROLE })
	@ApiOperation(value = "Delete template", notes = "Delete template for the supplied Rule Group ID and Template ID, template can only be deleted if there are no rules associated with them")
	public void deleteTemplate(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId,
			@NotNull @PathParam(TEMPLATE_ID) short templateId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			TemplateManager.getInstance().deleteTemplate(em, ruleGroup.getRuleGroupId(), templateId, am);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error deleting template:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}

	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@RolesAllowed({ ACLConstants.SUPER_ADMIN_ROLE, ACLConstants.ADMIN_ROLE })
	@ApiOperation(value = "Delete all templates", notes = "Delete all templates for the supplied Rule Group ID, templates can only be deleted if there are no rules associated with them")
	public void deleteAllTemplates(
			@NotNull @PathParam(RuleGroupEndpoint.RULE_GROUP_ID) @Size(min = 1, max = RuleGroup.RULE_GROUP_ID_MAX_SIZE) String ruleGroupId) {
		EntityManager em = am.getEM();
		try {
			RuleGroup ruleGroup = RuleGroupManager.getInstance().getRuleGroup(em, ruleGroupId);
			TemplateManager.getInstance().deleteTemplates(em, ruleGroup, am);
		} catch (NoResultException e) {
			throw new NotFoundException(Response.status(Status.NOT_FOUND).entity("Entity not found").build());
		} catch (Exception e) {
			throw new InternalServerErrorException(
					Response.serverError().entity("Error deleting all templates for rule group:" + e.getMessage()).build());
		} finally {
			em.close();
		}
	}
}