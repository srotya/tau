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
package io.symcpe.hendrix.nifi.lmm.interceptor;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the processor
 * 
 * @author ambud_sharma
 */
public class TestLMMInterceptor {
	
	private static final String TENANT_ID = "334234234321344563";
	private Map<String, String> attributes;
	
	@Before
	public void before() {
		attributes = new HashMap<>();
		attributes.put("syslog.timestamp", "2014-07-16T06:49:39.919Z");
	}

	@Test
	public void testCase1() throws IOException {
        InputStream content = new ByteArrayInputStream("this is a test message".getBytes());
        TestRunner runner = TestRunners.newTestRunner(new LMMInterceptor());
        
        runner.setProperty(LMMInterceptor.TENANT_ID, TENANT_ID);
        runner.setProperty(LMMInterceptor.API_KEY, TENANT_ID);
        runner.setProperty(LMMInterceptor.TIMESTAMP, "${'syslog.timestamp'}");
        
        runner.enqueue(content, attributes);
        runner.run(1);
        runner.assertQueueEmpty();
        
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(LMMInterceptor.SUCCESS);
        assertEquals(1, results.size());
        MockFlowFile result = results.get(0);
        result.assertAttributeEquals("tenant_id", TENANT_ID);
        result.assertAttributeEquals("@version", "1");
        result.assertAttributeEquals("@timestamp", attributes.get("syslog.timestamp"));
        result.assertAttributeEquals("message", "this is a test message");
	}
	
	@Test
	public void testCase2() throws IOException {
        InputStream content = new ByteArrayInputStream("this is a test message".getBytes());
        TestRunner runner = TestRunners.newTestRunner(new LMMInterceptor());
        
        runner.setProperty(LMMInterceptor.TENANT_ID, TENANT_ID);
        runner.setProperty(LMMInterceptor.API_KEY, TENANT_ID);
        runner.setProperty(LMMInterceptor.TIMESTAMP, "${'syslo.timestamp'}");
        
        runner.enqueue(content, attributes);
        runner.run(1);
        runner.assertQueueEmpty();
        
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(LMMInterceptor.FAILURE);
        assertEquals(1, results.size());
        MockFlowFile result = results.get(0);

        result.assertAttributeEquals("tenant_id", TENANT_ID);
        result.assertAttributeEquals("@version", "1");
        result.assertAttributeEquals("message", "this is a test message");
        // all field but the timestamp field should still be populated
	}
	
}
