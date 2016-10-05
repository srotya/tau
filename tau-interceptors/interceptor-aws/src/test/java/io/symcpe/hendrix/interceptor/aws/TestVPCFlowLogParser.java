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
package io.symcpe.hendrix.interceptor.aws;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.srotya.tau.interceptors.InterceptException;

/**
 * Unit tests for VPC flow log parsing
 * 
 * @author ambud_sharma
 */
public class TestVPCFlowLogParser {

	@Test
	public void testRecordParsing() throws InterceptException {
		String val = "2 123456789101 eni-g123abcd 100.100.10.2 50.90.30.21 43895 443 6 13 1836 1469126675 1469126733 ACCEPT OK";
		VPCFlowLogRecord record = VPCFlowLogParser.parseToRecord(val);
		assertNotNull(record);
		assertEquals((short) 2, record.getVersion());
		assertEquals("123456789101", record.getAccountId());
		assertEquals("eni-g123abcd", record.getInterfaceId());
		assertEquals("100.100.10.2", record.getSrcAddr());
		assertEquals("50.90.30.21", record.getDstAddr());
		assertEquals(43895, record.getSrcPort());
		assertEquals(443, record.getDstPort());
		assertEquals("6".charAt(0), record.getProtocol());
		assertEquals(13, record.getPackets());
		assertEquals(1836, record.getBytes());
		assertEquals(1469126675, record.getStartTs());
		assertEquals(1469126733, record.getEndTs());
		assertEquals((byte) "O".charAt(0), record.getLogStatus());
		System.out.println(new Gson().toJson(record));
	}

	@Test
	public void testRecordParsingInvalid() {
		String val = "2 123456789101 eni-g123abcd 100.100.10.2 50.90.30.21 43895 443 6 13 1836 1469126675 1469126733 RE OK";
		try {
			VPCFlowLogParser.parseToRecord(val);
			fail("Should throw an exception and not pass");
		} catch (InterceptException e) {
		}
	}

	@Test
	public void testRecordParsingSkipData() throws InterceptException {
		String val = "2 123456789101 eni-g123abcd - - - - - - - 1431280876 1431280934 - SKIPDATA";
		VPCFlowLogRecord record = VPCFlowLogParser.parseToRecord(val);
		assertNotNull(record);
		assertEquals((short) 2, record.getVersion());
		assertEquals("123456789101", record.getAccountId());
		assertEquals("eni-g123abcd", record.getInterfaceId());
		assertEquals(1431280876, record.getStartTs());
		assertEquals(1431280934, record.getEndTs());
		assertEquals((byte) "S".charAt(0), record.getLogStatus());
	}

	@Test
	public void testRecordParsingNoData() throws InterceptException {
		String val = "2 123456789010 eni-1a2b3c4d - - - - - - - 1431280876 1431280934 - NODATA";
		VPCFlowLogRecord record = VPCFlowLogParser.parseToRecord(val);
		assertNotNull(record);
		assertEquals((short) 2, record.getVersion());
		assertEquals("123456789010", record.getAccountId());
		assertEquals("eni-1a2b3c4d", record.getInterfaceId());
		assertEquals(1431280876, record.getStartTs());
		assertEquals(1431280934, record.getEndTs());
		assertEquals((byte) "N".charAt(0), record.getLogStatus());
	}

	@Test
	public void testFlowLogParse() throws InterceptException {
		VPCFlowLogParser parser = new VPCFlowLogParser();
		String event = "{\"messageType\":\"DATA_MESSAGE\",\"owner\":\"123456789115\",\"logGroup\":\"vpc-flow-log-group\",\"logStream\""
				+ ":\"eni-00fv0000-all\",\"subscriptionFilters\":[\"cloudwatch_flowlog_lambda_subscription\"],\"logEvents\":[{\"id\":\"327880164000000002313720044516500000000\",\"timestamp\":1470265507000,\"message\":\"2 123456789115 eni-0625b517 100.100.10.100 100.220.1.1 49162 5938 6 33 1848 1470265507 1470266096 ACCEPT OK\"},{\"id\":\"327880164000000002313720044516500000000\",\"timestamp\":123456789115,\"message\":\"2 686559647175 eni-0000b000 120.220.2.1 102.123.34.145 5938 49162 6 22 1144 1470265507 1470266096 ACCEPT OK\"}]}";
		List<Map<String, Object>> result = parser.parseFlowLogMap(event);
		assertEquals(2, result.size());
	}

}
