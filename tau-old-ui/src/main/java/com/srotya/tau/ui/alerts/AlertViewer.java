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
package com.srotya.tau.ui.alerts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.srotya.tau.ui.rules.RulesBean;

/**
 * Bean for alert views
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "av")
@SessionScoped
public class AlertViewer implements Serializable {

	private static final long serialVersionUID = 1L;
	private int updateFrequency = 2;
	private AlertReceiver receiver;
	@ManagedProperty(value="#{rb}")
	private RulesBean rules;
	private short ruleId = -1;
	private List<String> columns;
	private String columnName;

	public AlertViewer() {
	}

	@PostConstruct
	public void init() {
		this.receiver = AlertReceiver.getInstance();
		this.columns = new ArrayList<>();
	}

	public void addColumn() {
		columns.add(columnName);
	}

	public void removeColumn(int index) {
		columns.remove(index);
	}

	public void openChannel() {
		if (ruleId != -1) {
			try {
				receiver.addChannel(ruleId);
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage("Channel fail to open for ruleid:"+ruleId));
			}
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage("Channel opened for ruleid:"+ruleId));
		} else {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage("Must select a rule to open a channel"));
		}
	}

	public void closeChannel() {
		if (ruleId != -1) {
			try {
				receiver.closeChannel(ruleId);
			} catch (Exception e) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage("Channel fail to close for ruleid:"+ruleId));
			}
		}
	}

	/**
	 * @return the receiver
	 */
	public AlertReceiver getReceiver() {
		return receiver;
	}

	/**
	 * @param receiver
	 *            the receiver to set
	 */
	public void setReceiver(AlertReceiver receiver) {
		this.receiver = receiver;
	}

	/**
	 * @return
	 */
	public Queue<Map<String, Object>> getChannel() {
		if (ruleId<1) {
			return new LinkedList<>();
		} else {
			Queue<Map<String, Object>> channel = null;
			try {
				channel = receiver.getChannel(ruleId);
			} catch (Exception e) {
			}
			if(channel==null) {
				return new LinkedList<>();
			}else {
				return channel;
			}
		}
	}

	/**
	 * @return the ruleId
	 */
	public short getRuleId() {
		return ruleId;
	}

	/**
	 * @param ruleId the ruleId to set
	 */
	public void setRuleId(short ruleId) {
		this.ruleId = ruleId;
	}

	/**
	 * @return the updateFrequency
	 */
	public int getUpdateFrequency() {
		return updateFrequency;
	}

	/**
	 * @param updateFrequency
	 *            the updateFrequency to set
	 */
	public void setUpdateFrequency(int updateFrequency) {
		this.updateFrequency = updateFrequency;
	}

	/**
	 * @return the columns
	 */
	public List<String> getColumns() {
		return columns;
	}

	/**
	 * @return the columnName
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * @param columnName
	 *            the columnName to set
	 */
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	/**
	 * @return the rules
	 */
	public RulesBean getRules() {
		return rules;
	}

	/**
	 * @param rules the rules to set
	 */
	public void setRules(RulesBean rules) {
		this.rules = rules;
	}

}
