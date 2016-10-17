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
package com.srotya.tau.ui.validations;

import java.io.StringReader;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;

import com.srotya.tau.wraith.actions.Action;
import com.srotya.tau.wraith.actions.alerts.Alert;
import com.srotya.tau.wraith.actions.alerts.AlertAction;
import com.srotya.tau.wraith.rules.validator.ValidationException;
import com.srotya.tau.wraith.rules.validator.Validator;

/**
 * {@link Validator} for Apache Velocity {@link Alert} template body
 * 
 * @author ambud_sharma
 */
public class VelocityValidator implements Validator<Action> {

	@Override
	public void configure(List<Validator<?>> validators) {
	}

	@Override
	public void validate(Action value) throws ValidationException {
		if (value instanceof AlertAction) {
			String body = ((AlertAction) value).getBody();
			try {
				StringReader reader = new StringReader(body);
				SimpleNode node = RuntimeSingleton.getRuntimeServices().parse(reader, "testAction");
				Template template = new Template();
				template.setRuntimeServices(RuntimeSingleton.getRuntimeServices());
				template.setData(node);
				template.initDocument();
			} catch (ParseException e) {
				throw new ValidationException("Invalid velocity template:"+e.getMessage());
			}
		}
	}

	@Override
	public Class<Action> getType() {
		return Action.class;
	}

}
