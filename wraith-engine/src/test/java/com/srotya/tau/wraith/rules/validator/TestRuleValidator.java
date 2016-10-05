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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.rules.RuleSerializer;
import com.srotya.tau.wraith.rules.SimpleRule;
import com.srotya.tau.wraith.rules.validator.RuleValidator;
import com.srotya.tau.wraith.rules.validator.ValidationException;

/**
 * Tests for rule validator 
 * 
 * @author ambud_sharma
 */
public class TestRuleValidator {

	private List<String> validRules = new ArrayList<>();
	private List<String> invalidRules = new ArrayList<>();

	@Before
	public void before() throws IOException {
		List<String> valid = Utils.readAllLinesFromStream(
				TestRuleValidator.class.getClassLoader().getResourceAsStream("testValidRules.json"));
		validRules.addAll(valid);

		List<String> invalid = Utils.readAllLinesFromStream(
				TestRuleValidator.class.getClassLoader().getResourceAsStream("testInvalidRules.json"));
		invalidRules.addAll(invalid);
	}

	@Test
	public void testValidRules() throws ValidationException {
		RuleValidator validator = RuleValidator.getInstance();
		for (String rule : validRules) {
			SimpleRule ruleObj = RuleSerializer.deserializeJSONStringToRule(rule);
			validator.validate(ruleObj);
		}
	}
	
	@Test
	public void testInvalidRules() {
		RuleValidator validator = RuleValidator.getInstance();
		for (String rule : invalidRules) {
			try {
				SimpleRule ruleObj = RuleSerializer.deserializeJSONStringToRule(rule);
				validator.validate(ruleObj);
				Assert.fail("Invalid rule shouldn't pass validation: "+rule);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

}