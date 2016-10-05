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
package com.srotya.tau.dengine.metrics;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.syslog.Facility;
import com.cloudbees.syslog.MessageFormat;
import com.cloudbees.syslog.Severity;
import com.cloudbees.syslog.sender.AbstractSyslogMessageSender;
import com.cloudbees.syslog.sender.TcpSyslogMessageSender;
import com.cloudbees.syslog.sender.UdpSyslogMessageSender;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.srotya.tau.dengine.bolts.RulesEngineBolt;

import backtype.storm.metric.api.IMetricsConsumer.DataPoint;

/**
 * To receive metrics over syslog
 * 
 * @author ambud_sharma
 */
public class JsonMetricProcessor implements IMetricsProcessor {

	private static final String METRICS_SYSLOG_PORT = "metrics.syslog.port";
	private static final String METRICS_SYSLOG_DESTINATION = "metrics.syslog.destination";
	private static final String METRICS_SYSLOG_UDP = "metrics.syslog.udp";
	private boolean syslogUdp = true;
	private String syslogDestination = "localhost";
	private short syslogPort = 514;
	private Gson gson;
	private AbstractSyslogMessageSender messageSender;
	private Logger logger;

	@SuppressWarnings("rawtypes")
	public JsonMetricProcessor(Map stormConf) {
		logger = Logger.getLogger(JsonMetricProcessor.class.getName());
		if (stormConf.containsKey(METRICS_SYSLOG_UDP)) {
			syslogUdp = Boolean.parseBoolean(stormConf.get(METRICS_SYSLOG_UDP).toString());
		}
		if (stormConf.containsKey(METRICS_SYSLOG_DESTINATION)) {
			syslogDestination = stormConf.get(METRICS_SYSLOG_DESTINATION).toString();
		}
		if (stormConf.containsKey(METRICS_SYSLOG_PORT)) {
			syslogPort = Short.parseShort(stormConf.get(METRICS_SYSLOG_PORT).toString());
		}
		gson = new GsonBuilder().create();
		if (syslogUdp) {
			UdpSyslogMessageSender udpSyslogMessageSender = new UdpSyslogMessageSender();
			udpSyslogMessageSender.setSyslogServerHostname(syslogDestination);
			udpSyslogMessageSender.setSyslogServerPort(syslogPort);
			messageSender = udpSyslogMessageSender;
		} else {
			TcpSyslogMessageSender tcpSyslogMessageSender = new TcpSyslogMessageSender();
			tcpSyslogMessageSender.setSyslogServerHostname(syslogDestination);
			tcpSyslogMessageSender.setSsl(false);
			tcpSyslogMessageSender.setSyslogServerPort(syslogPort);
			messageSender = tcpSyslogMessageSender;
		}
		messageSender.setDefaultFacility(Facility.SYSLOG);
		messageSender.setDefaultSeverity(Severity.INFORMATIONAL);
		messageSender.setMessageFormat(MessageFormat.RFC_3164);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void processDataPoints(Collection<DataPoint> dataPoints) {
		for (DataPoint point : dataPoints) {
			JsonObject obj = new JsonObject();
			if (point.name.startsWith("mcm")) {
				for (Map.Entry<String, Long> entry : ((Map<String, Long>) point.value).entrySet()) {
					try {
						obj.addProperty("seriesName", point.name);
						obj.addProperty("name", point.name + "." + entry.getKey());
						if (point.name.contains("rule")) {
							String[] split = entry.getKey().split(RulesEngineBolt.TENANTID_SEPARATOR);
							obj.addProperty("ruleId", split[1]);
							obj.addProperty("tenantId", split[0]);
						}
						obj.addProperty("value", (Number) entry.getValue());
						messageSender.sendMessage(gson.toJson(obj));
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Exception parsin/sending instrumentation metrics", e);
					}
				}
			} else {
				if (point.value instanceof Number) {
					obj.addProperty("seriesName", point.name);
					obj.addProperty("value", (Number) point.value);
					try {
						messageSender.sendMessage(gson.toJson(obj));
					} catch (IOException e) {
						logger.log(Level.SEVERE, "Exception sending instrumentation metrics", e);
					}
				}
			}
		}
	}

}
