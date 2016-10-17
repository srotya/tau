package com.srotya.tau.wraith.rules.validator;

import java.util.List;

import com.srotya.tau.wraith.actions.alerts.Alert;

public class AlertValidator implements Validator<Alert> {
	
	private static ValidationException EXCEPTION = new ValidationException("Bad alert");

	@Override
	public void configure(List<Validator<?>> validators) {
	}

	@Override
	public void validate(Alert value) throws ValidationException {
		if(value.getBody()==null || value.getBody().trim().isEmpty()) {
			throw EXCEPTION;
		}
		if(value.getMedia()==null || value.getMedia().trim().isEmpty()) {
			throw EXCEPTION;
		}
		if(value.getSubject()==null || value.getSubject().trim().isEmpty()) {
			throw EXCEPTION;
		}
		if(value.getTarget()==null || value.getTarget().trim().isEmpty()) {
			throw EXCEPTION;
		}
		if(value.getTimestamp()<1) {
			throw EXCEPTION;
		}
	}

	@Override
	public Class<Alert> getType() {
		return Alert.class;
	}

}