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

import java.util.List;

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.omega.OmegaAction.LANGUAGE;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.srotya.tau.wraith.rules.validator.Validator;

/**
 * @author ambudsharma
 */
public class ScriptValidator implements Validator<Action> {

	@Override
	public void configure(List<Validator<?>> validators) {
	}

	@Override
	public void validate(Action value) throws ValidationException {
		if (value instanceof ScriptAction) {
			ScriptAction script = (ScriptAction) value;
			LANGUAGE language = script.getLanguage();
			if (language != LANGUAGE.Javascript && language != LANGUAGE.JRuby) {
				throw new ValidationException("Only Javascript and JRuby are currently supported");
			}
		}
	}

	@Override
	public Class<Action> getType() {
		return Action.class;
	}

}
