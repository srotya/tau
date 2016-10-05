package com.srotya.tau.ui.rules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;

import com.srotya.tau.ui.ApplicationManager;
import com.srotya.tau.ui.UserBean;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplateSerializer;

@ManagedBean(name="atb")
@SessionScoped
public class AlertTemplateBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private boolean enableEdit;
	@ManagedProperty(value = "#{am}")
	private ApplicationManager am;
	@ManagedProperty(value = "#{ub}")
	private UserBean ub;
	private AlertTemplate template;

	public AlertTemplateBean() {
	}

	@PostConstruct
	public void init() {
		template = new AlertTemplate();
	}

	public void onClose() {
		enableEdit = false;
	}

	public void save() {
		try {
			try {
				if (TemplateManager.getInstance().getTemplate(ub, ub.getTenant().getTenantId(),
						template.getTemplateId()) != null) {
					updateTemplate();
				}
			} catch (Exception e) {
				newTemplate();
			}
			enableEdit = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void newTemplate() {
		try {
			template = new AlertTemplate(TemplateManager.getInstance().createTemplate(ub, ub.getTenant()));
			enableEdit = true;
			RequestContext.getCurrentInstance().execute("location.reload();");
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to create new template", e.getMessage()));
		}
	}

	public void updateTemplate() {
		try {
			TemplateManager.getInstance().updateTemplate(ub, ub.getTenant().getTenantId(), template);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to update template", e.getMessage()));
		}
	}

	public void deleteTemplate(short templateId) {
		try {
			TemplateManager.getInstance().deleteTemplate(ub, ub.getTenant().getTenantId(), templateId);
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to delete template", e.getMessage()));
		}
	}

	public void changeCurrentTemplate(short templateId) {
		try {
			template = TemplateManager.getInstance().getTemplate(ub, ub.getTenant().getTenantId(), templateId);
			enableEdit = true;
			RequestContext.getCurrentInstance().execute("location.reload();");
		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unable to find template", e.getMessage()));
		}
	}
	
	public String templateToJson(AlertTemplate template) {
		return AlertTemplateSerializer.serialize(template, true).replace("\n", "<br/>").replaceAll("\\s", "&nbsp;");
	}
	
	public List<AlertTemplate> getTemplates() {
		try {
			return TemplateManager.getInstance().getTemplates(ub, ub.getTenant().getTenantId());
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * @return the enableEdit
	 */
	public boolean isEnableEdit() {
		return enableEdit;
	}

	/**
	 * @param enableEdit the enableEdit to set
	 */
	public void setEnableEdit(boolean enableEdit) {
		this.enableEdit = enableEdit;
	}

	/**
	 * @return the am
	 */
	public ApplicationManager getAm() {
		return am;
	}

	/**
	 * @param am the am to set
	 */
	public void setAm(ApplicationManager am) {
		this.am = am;
	}

	/**
	 * @return the ub
	 */
	public UserBean getUb() {
		return ub;
	}

	/**
	 * @param ub the ub to set
	 */
	public void setUb(UserBean ub) {
		this.ub = ub;
	}

	/**
	 * @return the template
	 */
	public AlertTemplate getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(AlertTemplate template) {
		this.template = template;
	}
	
}
