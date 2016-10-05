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
package com.srotya.tau.dengine;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.stubbing.Answer;

import com.srotya.tau.dengine.Constants;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.actions.alerts.templated.TemplateCommand;
import com.srotya.tau.wraith.rules.RuleCommand;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Tuple;

/**
 * Mockito helpers to create different mockable components
 * 
 * @author ambud_sharma
 */
public class MockTupleHelpers {

	private MockTupleHelpers() {
	}

	public static Tuple mockRuleTuple(boolean deleteRule, String ruleGroup, String rule) {
		Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceComponent()).thenReturn(Constants.RULE_SYNC_COMPONENT);
		when(tuple.getSourceStreamId()).thenReturn(Constants.SYNC_STREAM_ID);
		RuleCommand command = new RuleCommand();
		command.setDelete(deleteRule);
		command.setRuleContent(rule);
		command.setRuleGroup(ruleGroup);
		when(tuple.getValue(0)).thenReturn(command);
		when(tuple.getValueByField(Constants.FIELD_RULE_CONTENT)).thenReturn(command);
		System.err.println("MockRule:"+command.toString());
		return tuple;
	}
	
	public static Tuple mockTemplateTuple(boolean deleteRule, String tenantId, String template) {
		Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceComponent()).thenReturn(Constants.TEMPLATE_SYNC_COMPONENT);
		when(tuple.getSourceStreamId()).thenReturn(Constants.SYNC_STREAM_ID);
		TemplateCommand command = new TemplateCommand();
		command.setDelete(deleteRule);
		command.setTemplateContent(template);
		command.setRuleGroup(tenantId);
		when(tuple.getValue(0)).thenReturn(command);
		when(tuple.getValueByField(Constants.FIELD_TEMPLATE_CONTENT)).thenReturn(command);
		return tuple;
	}

	public static Tuple mockTuple(String componentId, String streamId, String rule) {
		Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceComponent()).thenReturn(componentId);
		when(tuple.getSourceStreamId()).thenReturn(streamId);
		when(tuple.getString(0)).thenReturn(rule);
		return tuple;
	}
	
	public static Tuple mockEventTuple(Event event) {
		Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceComponent()).thenReturn(Constants.RULE_SYNC_COMPONENT);
		when(tuple.getSourceStreamId()).thenReturn(Constants.STORE_STREAM_ID);
		when(tuple.getValueByField(com.srotya.tau.wraith.Constants.FIELD_EVENT)).thenReturn(event);
		return tuple;
	}

	public static OutputCollector mockBasicCollector() {
		OutputCollector collector = mock(OutputCollector.class);
		return collector;
	}

	public static OutputCollector mockCollector(Answer<Object> callBack) {
		OutputCollector collector = mock(OutputCollector.class);
		when(collector.emit(any(Tuple.class), anyObject())).thenAnswer(callBack);
		when(collector.emit(anyString(), any(Tuple.class), anyObject())).thenAnswer(callBack);
		return collector;
	}

}
