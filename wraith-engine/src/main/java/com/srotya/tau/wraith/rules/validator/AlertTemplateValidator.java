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
package com.srotya.tau.wraith.rules.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;

public class AlertTemplateValidator implements Validator<AlertTemplate> {

	private static final int MAX_LENGTH_ALERT_DESTINATION = 200;
	private static final int MAX_LENGTH_ALERT_MEDIA = 50;
	private static final Pattern EMAIL_PATTERN = Pattern
			.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
	private static final Pattern HTTP_PATTERN = Pattern.compile("^https?\\://.*");
	private static final Pattern SLACK_PATTERN = Pattern.compile("[a-zA-Z0-9/]{10,}@[a-z0-9\\_\\-]{1,21}");
	private List<Validator<AlertTemplate>> alertTemplateValidator;

	public AlertTemplateValidator() {
		this.alertTemplateValidator = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void configure(List<Validator<?>> validators) {
		for (Validator<?> validator : validators) {
			try {
				alertTemplateValidator.add((Validator<AlertTemplate>) validator);
			} catch (Exception e) { // ignore incompatible validators
			}
		}
	}

	@Override
	public void validate(AlertTemplate template) throws ValidationException {
		if (template.getDestination() == null || template.getDestination().trim().isEmpty()) {
			throw new ValidationException("Alert target can't be empty");
		}
		if (template.getDestination().length() > MAX_LENGTH_ALERT_DESTINATION) {
			throw new ValidationException(
					"Alert target must be less than " + MAX_LENGTH_ALERT_DESTINATION + " characters");
		}
		if (template.getMedia() == null || template.getMedia().trim().isEmpty()) {
			throw new ValidationException("Alert media can't be empty");
		}
		if (template.getMedia().length() > MAX_LENGTH_ALERT_MEDIA) {
			throw new ValidationException("Alert media must be less than " + MAX_LENGTH_ALERT_MEDIA + " characters");
		}
		if (template.getBody() == null || template.getBody().trim().isEmpty()) {
			throw new ValidationException("Template body can't be empty");
		}
		if (template.getTemplateName() == null || template.getTemplateName().trim().isEmpty()) {
			throw new ValidationException("Template name can't be empty");
		}
		if (template.getThrottleDuration() < 1) {
			throw new ValidationException("Throttle duration can't be less than 1");
		}
		if (template.getThrottleLimit() < 1) {
			throw new ValidationException("Throttle limit can't be less than 1");
		}
		switch (template.getMedia()) {
		case "mail":
		case "http":
		case "slack":
			break;
		default:
			throw new ValidationException("Bad media type, only mail and http are allowed");
		}
		if (template.getMedia().contains("mail")) {
			String[] emails = template.getDestination().split("\\s{0,1},");
			for (String email : emails) {
				if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
					throw new ValidationException("Not a valid email address:" + email);
				}
			}
		} else if (template.getMedia().contains("http")) {
			if (!HTTP_PATTERN.matcher(template.getDestination()).matches()) {
				throw new ValidationException("Not a valid http address:" + template.getDestination());
			}
		} else if (template.getMedia().contains("slack")) {
			if (!SLACK_PATTERN.matcher(template.getDestination()).matches()) {
				throw new ValidationException("Not a valid slack address:" + template.getDestination());
			}
		}
		for (Validator<AlertTemplate> validator : alertTemplateValidator) {
			validator.validate(template);
		}
	}

	@Override
	public Class<AlertTemplate> getType() {
		return AlertTemplate.class;
	}

}
