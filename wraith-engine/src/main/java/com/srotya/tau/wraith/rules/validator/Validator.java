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

import java.util.List;

/**
 * Interface for defining a validator
 * 
 * @author ambud_sharma
 *
 * @param <T>
 */
public interface Validator<T> {

	/**
	 * To attach additional {@link Validator} to be used for validation of type T
	 * @param validators
	 */
	public void configure(List<Validator<?>> validators);
	
	/**
	 * Perform validation of type T
	 * @param value
	 * @throws ValidationException
	 */
	public void validate(T value) throws ValidationException;

}
