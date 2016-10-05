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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.aggregations.StateAggregationAction;
import com.srotya.tau.wraith.actions.alerts.templated.TemplatedAlertAction;

/**
 * JSF Action Bean
 * 
 * @author ambud_sharma
 */
@ManagedBean(name = "ab")
@SessionScoped
public class ActionBean implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String AGGREGATE = "aggregate";
	public static final String ALERT = "alert";
	public static final String STATE = "state";
	public static final String TAG = "tag";
	public static final String BUILD = "build";
	public static final List<String> ACTION_TYPES = Arrays.asList(AGGREGATE, ALERT, TAG, BUILD);
	private List<Action> actions;
	private Action currAction;

	@ManagedProperty(value = "#{rb}")
	private RulesBean rb;

	public ActionBean() {
	}

	@PostConstruct
	public void init() {
		actions = new ArrayList<>();
	}

	public void addAction(String actionType) {
		if (actionType != null) {
			switch (actionType) {
			case ALERT:
				actions.add(new TemplatedAlertAction((short) actions.size(), (short)-1));
				break;
			case STATE:
				actions.add(new StateAggregationAction((short) actions.size(), "", 30, null));
			default:
				break;
			}
		}
	}

	public void loadActions() {
		if (rb.getCurrRule() != null) {
			actions = rb.getCurrRule().getActions();
		}
	}

	public void alignActionIds() {
		for (int i = 0; i < actions.size(); i++) {
			actions.get(i).setActionId((short) i);
		}
	}

	public void moveup(int i) {
		if (i > 0 && actions.size() > i) {
			Action act = actions.remove(i);
			actions.add(--i, act);
			alignActionIds();
		} else {
			System.err.println("Invalid action sequence:" + i);
		}
	}

	public void movedown(int i) {
		if ((i + 1) < actions.size()) {
			Action act = actions.remove(i);
			actions.add(++i, act);
			alignActionIds();
		} else {
			System.err.println("Invalid action sequence:" + i);
		}
	}

	public String getCommonName(Class<? extends Action> actionClass) {
		if (actionClass != null) {
			return Utils.CLASSNAME_FORWARD_MAP.get(actionClass.getName());
		} else {
			return "";
		}
	}

	public boolean alertAction(Class<? extends Action> actionClass) {
		return actionClass == TemplatedAlertAction.class;
	}
	
	public boolean stateAction(Class<? extends Action> actionClass) {
		return actionClass == StateAggregationAction.class;
	}

	public void selectAction(int actionId) {
		if (actionId < actions.size()) {
			currAction = actions.get(actionId);
		}
	}

	/**
	 * @return the rb
	 */
	public RulesBean getRb() {
		return rb;
	}

	/**
	 * @param rb
	 *            the rb to set
	 */
	public void setRb(RulesBean rb) {
		this.rb = rb;
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		return actions;
	}

	public List<String> getActionTypes() {
		return ACTION_TYPES;
	}

	/**
	 * @return the currAction
	 */
	public Action getCurrAction() {
		return currAction;
	}

	/**
	 * @param currAction
	 *            the currAction to set
	 */
	public void setCurrAction(Action currAction) {
		this.currAction = currAction;
	}

}
