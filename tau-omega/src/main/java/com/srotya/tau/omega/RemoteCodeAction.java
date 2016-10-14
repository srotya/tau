/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.omega;

import com.srotya.tau.wraith.actions.omega.OmegaAction;

/**
 * @author ambudsharma
 */
public class RemoteCodeAction extends OmegaAction {

	private static final long serialVersionUID = 1L;
	private Code code;

	public RemoteCodeAction(short actionId, LANGUAGE language, Code code) {
		super(actionId, language);
		this.code = code;
	}

	/**
	 * @return the code
	 */
	public Code getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(Code code) {
		this.code = code;
	}

}
