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
package com.srotya.tau.nucleus.processor.alerts;

import com.srotya.tau.wraith.actions.alerts.Alert;

/**
 * @author ambudsharma
 */
public class SlackService extends HttpService {

	@Override
	public void sendHttpCallback(Alert alert) throws AlertDeliveryException {
		String target = alert.getTarget().split("@")[0];
		String channel = alert.getTarget().split("@")[1];
		String bodyContent = "{ \"channel\":\"" + channel + "\",\"username\":\"webhookbot\", \"text\":\"" + alert.getBody()
				+ "\" }";
		super.sendHttpCallback(target, bodyContent);
	}

}
