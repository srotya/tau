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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.srotya.tau.interceptors.InterceptException;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.EventFactory;

/**
 * Parser for VPC Flow Logs streamed to Lambda from CloudWatch via a
 * subscription filter
 * 
 * @author ambud_sharma
 */
public class VPCFlowLogParser {

	private static final char STATUS_CODE_ZERO = 'O';
	private static final String MESSAGE = "message";
	private static final String ID = "id";
	private static final String TIMESTAMP2 = "timestamp";
	private static final String TIMESTAMP = "@timestamp";
	private static final String LOG_EVENTS = "logEvents";
	private static final String LOG_STREAM = "logStream";
	private static final String LOG_GROUP = "logGroup";
	private static final String ACCEPT = "ACCEPT";
	private static final String LOG_STATUS = "log-status";
	private static final String ACCEPTED = "accepted";
	private static final String END = "end";
	private static final String START = "start";
	private static final String BYTES = "bytes";
	private static final String PACKETS = "packets";
	private static final String PROTOCOL = "protocol";
	private static final String DSTPORT = "dstport";
	private static final String SRCPORT = "srcport";
	private static final String DSTADDR = "dstaddr";
	private static final String SRCADDR = "srcaddr";
	private static final String INTERFACE_ID = "interface-id";
	private static final String ACCOUNT_ID = "account-id";
	private static final String VERSION = "version";
	// http://docs.aws.amazon.com/AmazonVPC/latest/UserGuide/flow-logs.html#flow-log-records
	private static final String FLOW_RECORD_REGEX = "(\\d)" // version
			+ "\\s(.*)" // account-id
			+ "\\s(.*-.*)" // interface-id
			+ "\\s(\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}|\\-)" // srcaddr
			+ "\\s(\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}|\\-)" // dstaddr
			+ "\\s(\\d{1,5}|\\-)" // srcport
			+ "\\s(\\d{1,5}|\\-)" // dstport
			+ "\\s(\\d{1,3}|\\-)" // protocol
			+ "\\s(\\d+|\\-)" // packets
			+ "\\s(\\d+|\\-)" // bytes
			+ "\\s(\\d+)" // start
			+ "\\s(\\d+)" // end
			+ "\\s(ACCEPT|REJECT|\\-)" // action
			+ "\\s(OK|NODATA|SKIPDATA)"; // log-status
	private static final Pattern FLOW_RECORD_PATTERN = Pattern.compile(FLOW_RECORD_REGEX);
	private Gson gson;

	public VPCFlowLogParser() {
		gson = new Gson();
	}
	
	public List<Map<String, Object>> parseFlowLogMap(String json) throws InterceptException {
		try {
			List<Map<String, Object>> events = new ArrayList<>();
			JsonObject flowLogs = gson.fromJson(json, JsonObject.class);
			String logGroup = flowLogs.get(LOG_GROUP).getAsString();
			String logStream = flowLogs.get(LOG_STREAM).getAsString();
			for (JsonElement element : flowLogs.get(LOG_EVENTS).getAsJsonArray()) {
				Map<String, Object> map = new LinkedHashMap<>();
				JsonObject obj = element.getAsJsonObject();
				map.put(LOG_GROUP, logGroup);
				map.put(LOG_STREAM, logStream);
				map.put(TIMESTAMP, obj.get(TIMESTAMP2).getAsLong());
				map.put(ID, obj.get(ID).getAsString());
				parseToRecord(map, obj.get(MESSAGE).getAsString());
				events.add(map);
			}
			return events;
		} catch (Exception e) {
			throw new InterceptException(e);
		}
	}

	public static void parseToRecord(Map<String, Object> map, String line) throws InterceptException {
		Matcher matcher = FLOW_RECORD_PATTERN.matcher(line);
		if (matcher.matches()) {
			try {
				map.put(VERSION, Short.parseShort(matcher.group(1)));
				map.put(ACCOUNT_ID, matcher.group(2));
				map.put(INTERFACE_ID, matcher.group(3));
				if (matcher.group(14).charAt(0) == STATUS_CODE_ZERO) {
					map.put(SRCADDR, matcher.group(4));
					map.put(DSTADDR, matcher.group(5));
					map.put(SRCPORT, Integer.parseInt(matcher.group(6)));
					map.put(DSTPORT, Integer.parseInt(matcher.group(7)));
					map.put(PROTOCOL, matcher.group(8).charAt(0));
					map.put(PACKETS, Integer.parseInt(matcher.group(9)));
					map.put(BYTES, Integer.parseInt(matcher.group(10)));
				}
				map.put(START, Integer.parseInt(matcher.group(11)));
				map.put(END, Integer.parseInt(matcher.group(12)));
				map.put(ACCEPTED, matcher.group(13).equals(ACCEPT));
				map.put(LOG_STATUS, (byte) matcher.group(14).charAt(0));
			} catch (Exception e) {
				throw new InterceptException(e);
			}
		} else {
			throw new InterceptException("Line doesn't match flow record:" + line);
		}
	}

	public List<Event> parseFlowLogJson(EventFactory factory, String json) throws InterceptException {
		try {
			List<Event> events = new ArrayList<>();
			JsonObject flowLogs = gson.fromJson(json, JsonObject.class);
			String logGroup = flowLogs.get(LOG_GROUP).getAsString();
			String logStream = flowLogs.get(LOG_STREAM).getAsString();
			for (JsonElement element : flowLogs.get(LOG_EVENTS).getAsJsonArray()) {
				Event event = factory.buildEvent();
				JsonObject obj = element.getAsJsonObject();
				event.getHeaders().put(LOG_GROUP, logGroup);
				event.getHeaders().put(LOG_STREAM, logStream);
				event.getHeaders().put(TIMESTAMP, obj.get(TIMESTAMP2).getAsLong());
				event.getHeaders().put(ID, obj.get(ID).getAsString());
				parseToRecord(event, obj.get(MESSAGE).getAsString());
				events.add(event);
			}
			return events;
		} catch (Exception e) {
			throw new InterceptException(e);
		}
	}

	public static void parseToRecord(Event event, String line) throws InterceptException {
		parseToRecord(event.getHeaders(), line);
	}

	public static VPCFlowLogRecord parseToRecord(String line) throws InterceptException {
		Matcher matcher = FLOW_RECORD_PATTERN.matcher(line);
		if (matcher.matches()) {
			try {
				VPCFlowLogRecord record = new VPCFlowLogRecord();
				record.setVersion(Short.parseShort(matcher.group(1)));
				record.setAccountId(matcher.group(2));
				record.setInterfaceId(matcher.group(3));
				if (matcher.group(14).charAt(0) == STATUS_CODE_ZERO) {
					record.setSrcAddr(matcher.group(4));
					record.setDstAddr(matcher.group(5));
					record.setSrcPort(Integer.parseInt(matcher.group(6)));
					record.setDstPort(Integer.parseInt(matcher.group(7)));
					record.setProtocol(matcher.group(8).charAt(0));
					record.setPackets(Integer.parseInt(matcher.group(9)));
					record.setBytes(Integer.parseInt(matcher.group(10)));
				}
				record.setStartTs(Integer.parseInt(matcher.group(11)));
				record.setEndTs(Integer.parseInt(matcher.group(12)));
				record.setAccepted(matcher.group(13).equals(ACCEPT));
				record.setLogStatus((byte) matcher.group(14).charAt(0));
				return record;
			} catch (Exception e) {
				throw new InterceptException(e);
			}
		} else {
			throw new InterceptException("Line doesn't match flow record:" + line);
		}
	}

}
