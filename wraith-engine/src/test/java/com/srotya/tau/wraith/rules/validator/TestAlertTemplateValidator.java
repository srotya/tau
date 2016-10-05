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

import static org.junit.Assert.*;

import org.junit.Test;

import com.srotya.tau.wraith.actions.alerts.templated.AlertTemplate;
import com.srotya.tau.wraith.rules.validator.AlertTemplateValidator;
import com.srotya.tau.wraith.rules.validator.ValidationException;

/**
 * Tests for validation of alert templates
 * 
 * @author ambud_sharma
 */
public class TestAlertTemplateValidator {
	
	@Test
	public void testMailAlertTemplatePositive() throws ValidationException {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		template.setMedia("mail");
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("abc@xyz.com, efg@xyz.com");
		validator.validate(template);
	}

	@Test
	public void testMailAlertTemplateNegative() {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		try{
			validator.validate(template);
			fail("Invalid template, can't pass test");
		}catch(ValidationException e) {
		}
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("abc@xyz.com, efg@xyz.com");
		template.setMedia("test");
		try{
			validator.validate(template);
			fail("Invalid template, can't pass test");
		}catch(ValidationException e) {
		}
		template.setDestination("abc@xyz.com, efg@xy.c");
		try{
			validator.validate(template);
			fail("Invalid template, can't pass test");
		}catch(ValidationException e) {
		}
		template.setDestination("abc@xyz.com,efg@xy.c");
		try{
			validator.validate(template);
			fail("Invalid template, can't pass test");
		}catch(ValidationException e) {
		}
		template.setDestination("hello924jwjaksfsduufasidjfhkasdjfadsf9asfijklsdfoasdljfo9sdfpiojkasdfoasjdifaisdpfjasdl;fiaosdf9posdjklfasdfhaosdif9paidsujklfasdfa09wrjiwqjrwfjoifjlas@xyz.com");
		try{
			validator.validate(template);
			fail("Invalid template, can't pass test");
		}catch(ValidationException e) {
		}
	}
	
	@Test
	public void testHTTPAlertTemplatePostive() throws ValidationException {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		template.setMedia("http");
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("https://google.com");
		validator.validate(template);
		template.setDestination("http://google.com");
		validator.validate(template);
	}
	
	@Test
	public void testHTTPAlertTemplateNegative() {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		template.setMedia("http");
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("htt://google.com");
		try {
			validator.validate(template);
			fail("Invalid template, can't pass test");
		} catch (ValidationException e) {
		}
		template.setDestination("smtp://google.com");
		try {
			validator.validate(template);
			fail("Invalid template, can't pass test");
		} catch (ValidationException e) {
		}
	}
	
	@Test
	public void testSlackAlertTemplatePostive() throws ValidationException {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		template.setMedia("slack");
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX@slack-channel");
		validator.validate(template);
		template.setDestination("T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX@slack_channel");
		validator.validate(template);
	}
	
	@Test
	public void testSlackAlertTemplateNegative() {
		AlertTemplateValidator validator = new AlertTemplateValidator();
		AlertTemplate template = new AlertTemplate((short)2);
		template.setMedia("slack");
		template.setSubject("hello");
		template.setBody("hello $x");
		template.setTemplateName("Template");
		template.setDestination("ABC@slack-channel");
		try {
			validator.validate(template);
			fail("Invalid template, can't pass test");
		} catch (ValidationException e) {
		}
		template.setDestination("ANV@slack#channel");
		try {
			validator.validate(template);
			fail("Invalid template, can't pass test");
		} catch (ValidationException e) {
		}
	}
}
