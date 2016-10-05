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
package com.srotya.tau.wraith.conditions.relational;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.srotya.tau.wraith.Required;
import com.srotya.tau.wraith.conditions.AbstractSimpleCondition;

/**
 * An {@link AbstractSimpleCondition} that is true if the header value matches
 * the regular expression this condition has been configured for.
 * 
 * @author ambud_sharma
 */
public class JavaRegexCondition extends AbstractSimpleCondition {

	private static final long serialVersionUID = 1L;
	private transient Pattern pattern;
	@Required
	private String value;

	public JavaRegexCondition(String header, String regex) {
		super(header);
		this.value = regex;
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public boolean satisfiesCondition(Object value) {
		if (value instanceof String) {
			Matcher matcher = this.pattern.matcher(value.toString());
			return matcher.matches();
		}
		return false;
	}

	/**
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * @param pattern the pattern to set
	 */
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	/**
	 * @return the regex
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param regex the regex to set
	 */
	public void setValue(String regex) {
		this.value = regex;
		this.pattern = Pattern.compile(regex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getkey() + " matches " + value;
	}

}
