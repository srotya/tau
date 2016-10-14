package com.srotya.tau.omega;

import com.srotya.tau.wraith.actions.omega.OmegaAction;

public class ScriptAction extends OmegaAction {

	private static final long serialVersionUID = 1L;
	private String script;

	public ScriptAction(short actionId, LANGUAGE language, String script) {
		super(actionId, language);
		this.script = script;
	}
	
	public String getScript() {
		return script;
	}

}
