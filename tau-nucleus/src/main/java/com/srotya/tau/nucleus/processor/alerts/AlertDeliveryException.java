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
package com.srotya.tau.nucleus.processor.alerts;

import com.srotya.tau.wraith.PerformantException;

/**
 * {@link PerformantException} to notify failure of alert deliveries 
 * 
 * @author ambud_sharma
 */
public class AlertDeliveryException extends PerformantException {

	private static final long serialVersionUID = 1L;

	public AlertDeliveryException() {
		super();
	}

	public AlertDeliveryException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AlertDeliveryException(String message, Throwable cause) {
		super(message, cause);
	}

	public AlertDeliveryException(String message) {
		super(message);
	}

	public AlertDeliveryException(Throwable cause) {
		super(cause);
	}

}
