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

import static org.joda.time.format.ISODateTimeFormat.basicDate;
import static org.joda.time.format.ISODateTimeFormat.basicDateTime;
import static org.joda.time.format.ISODateTimeFormat.basicDateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.basicOrdinalDate;
import static org.joda.time.format.ISODateTimeFormat.basicOrdinalDateTime;
import static org.joda.time.format.ISODateTimeFormat.basicOrdinalDateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.basicTTime;
import static org.joda.time.format.ISODateTimeFormat.basicTTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.basicTime;
import static org.joda.time.format.ISODateTimeFormat.basicTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.basicWeekDate;
import static org.joda.time.format.ISODateTimeFormat.basicWeekDateTime;
import static org.joda.time.format.ISODateTimeFormat.basicWeekDateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.date;
import static org.joda.time.format.ISODateTimeFormat.dateElementParser;
import static org.joda.time.format.ISODateTimeFormat.dateHour;
import static org.joda.time.format.ISODateTimeFormat.dateHourMinute;
import static org.joda.time.format.ISODateTimeFormat.dateHourMinuteSecond;
import static org.joda.time.format.ISODateTimeFormat.dateHourMinuteSecondFraction;
import static org.joda.time.format.ISODateTimeFormat.dateHourMinuteSecondMillis;
import static org.joda.time.format.ISODateTimeFormat.dateOptionalTimeParser;
import static org.joda.time.format.ISODateTimeFormat.dateParser;
import static org.joda.time.format.ISODateTimeFormat.dateTime;
import static org.joda.time.format.ISODateTimeFormat.dateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.dateTimeParser;
import static org.joda.time.format.ISODateTimeFormat.hour;
import static org.joda.time.format.ISODateTimeFormat.hourMinute;
import static org.joda.time.format.ISODateTimeFormat.hourMinuteSecond;
import static org.joda.time.format.ISODateTimeFormat.hourMinuteSecondFraction;
import static org.joda.time.format.ISODateTimeFormat.hourMinuteSecondMillis;
import static org.joda.time.format.ISODateTimeFormat.localDateOptionalTimeParser;
import static org.joda.time.format.ISODateTimeFormat.localDateParser;
import static org.joda.time.format.ISODateTimeFormat.localTimeParser;
import static org.joda.time.format.ISODateTimeFormat.ordinalDate;
import static org.joda.time.format.ISODateTimeFormat.ordinalDateTime;
import static org.joda.time.format.ISODateTimeFormat.ordinalDateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.tTime;
import static org.joda.time.format.ISODateTimeFormat.tTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.time;
import static org.joda.time.format.ISODateTimeFormat.timeElementParser;
import static org.joda.time.format.ISODateTimeFormat.timeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.timeParser;
import static org.joda.time.format.ISODateTimeFormat.weekDate;
import static org.joda.time.format.ISODateTimeFormat.weekDateTime;
import static org.joda.time.format.ISODateTimeFormat.weekDateTimeNoMillis;
import static org.joda.time.format.ISODateTimeFormat.weekyear;
import static org.joda.time.format.ISODateTimeFormat.weekyearWeek;
import static org.joda.time.format.ISODateTimeFormat.weekyearWeekDay;
import static org.joda.time.format.ISODateTimeFormat.year;
import static org.joda.time.format.ISODateTimeFormat.yearMonth;
import static org.joda.time.format.ISODateTimeFormat.yearMonthDay;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

/**
 * @author ambud_sharma
 */
@CapabilityDescription("Convert inbound FlowFiles to LMM/Logstash format events. Must be used with AttributeToJSON processor")
public class LMMInterceptor extends AbstractProcessor {

	private static final String TARGET_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private static final String _1 = "1";
	private static final String ATTR_TIMESTAMP = "@timestamp";
	private static final String ATTR_VERSION = "@version";
	private static final String ATTR_TENANT_ID = "tenant_id";
	private static final String ATTR_API_KEY = "apikey";

	public static final PropertyDescriptor TENANT_ID = new PropertyDescriptor.Builder().name("Tenant Id").required(true).expressionLanguageSupported(false)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();
	
	public static final PropertyDescriptor API_KEY = new PropertyDescriptor.Builder().name("Api Key").required(true).expressionLanguageSupported(false)
			.addValidator(StandardValidators.NON_EMPTY_VALIDATOR).build();

	public static final PropertyDescriptor TIMESTAMP = new PropertyDescriptor.Builder().name("Timestamp").required(true).expressionLanguageSupported(true)
			.addValidator(StandardValidators.ATTRIBUTE_EXPRESSION_LANGUAGE_VALIDATOR).build();

	public static final Relationship SUCCESS = new Relationship.Builder().name("SUCCESS")
			.description("Successful interception").build();
	public static final Relationship FAILURE = new Relationship.Builder().name("FAILURE")
			.description("Unsuccessful interception (exception)").build();

	private List<PropertyDescriptor> properties;
	private Set<Relationship> relationships;

	private final DateTimeParser[] formats = { basicDate().getParser(), // yyyyMMdd
			basicDateTime().getParser(), // yyyyMMdd'T'HHmmss.SSSZ
			basicDateTimeNoMillis().getParser(),
			// yyyyMMdd'T'HHmmssZ
			basicOrdinalDate().getParser(), // yyyyDDD
			basicOrdinalDateTime().getParser(),
			// yyyyDDD'T'HHmmss.SSSZ
			basicOrdinalDateTimeNoMillis().getParser(), basicTime().getParser(), basicTimeNoMillis().getParser(),
			basicTTime().getParser(), basicTTimeNoMillis().getParser(), basicWeekDate().getParser(),
			basicWeekDateTime().getParser(), basicWeekDateTimeNoMillis().getParser(), date().getParser(),
			dateElementParser().getParser(), dateHour().getParser(), dateHourMinute().getParser(),
			dateHourMinuteSecond().getParser(), dateHourMinuteSecondFraction().getParser(),
			dateHourMinuteSecondMillis().getParser(), dateOptionalTimeParser().getParser(), dateParser().getParser(),
			dateTime().getParser(), dateTimeNoMillis().getParser(), dateTimeParser().getParser(), hour().getParser(),
			hourMinute().getParser(), hourMinuteSecond().getParser(), hourMinuteSecondFraction().getParser(),
			hourMinuteSecondMillis().getParser(), localDateOptionalTimeParser().getParser(),
			localDateParser().getParser(), localTimeParser().getParser(), ordinalDate().getParser(),
			ordinalDateTime().getParser(), ordinalDateTimeNoMillis().getParser(), time().getParser(),
			timeElementParser().getParser(), timeNoMillis().getParser(), timeParser().getParser(), tTime().getParser(),
			tTimeNoMillis().getParser(), weekDate().getParser(), weekDateTime().getParser(),
			weekDateTimeNoMillis().getParser(), weekyear().getParser(), weekyearWeek().getParser(),
			weekyearWeekDay().getParser(), year().getParser(), yearMonth().getParser(), yearMonthDay().getParser(),
			DateTimeFormat.forPattern("yyyy.MM.dd").getParser() };
	private DateTimeFormatter formatter;

	@Override
	protected void init(ProcessorInitializationContext context) {
		List<PropertyDescriptor> properties = new ArrayList<>();
		properties.add(TENANT_ID);
		properties.add(TIMESTAMP);
		properties.add(API_KEY);
		this.properties = Collections.unmodifiableList(properties);

		Set<Relationship> relationships = new HashSet<>();
		relationships.add(SUCCESS);
		relationships.add(FAILURE);
		this.relationships = Collections.unmodifiableSet(relationships);

		formatter = new DateTimeFormatterBuilder().append(null, formats).toFormatter();
	}

	@Override
	public void onTrigger(ProcessContext ctx, ProcessSession session) throws ProcessException {
		FlowFile flowFile = session.get();
		if (flowFile == null) {
			return;
		}

		try {
			AtomicReference<String> message = new AtomicReference<String>();
			session.read(flowFile, true, new InputStreamCallback() {
				
				@Override
				public void process(InputStream in) throws IOException {
					message.set(IOUtils.toString(in));
				}
			});
			flowFile = session.putAttribute(flowFile, "message", message.get());
			flowFile = session.putAttribute(flowFile, ATTR_TENANT_ID, ctx.getProperty(TENANT_ID).getValue());
			flowFile = session.putAttribute(flowFile, ATTR_API_KEY, ctx.getProperty(API_KEY).getValue());
			flowFile = session.putAttribute(flowFile, ATTR_VERSION, _1);
			String timestamp = ctx.getProperty(TIMESTAMP).evaluateAttributeExpressions(flowFile).getValue();
			DateTime ts = formatter.withZoneUTC().parseDateTime(timestamp);
			flowFile = session.putAttribute(flowFile, ATTR_TIMESTAMP, ts.toString(TARGET_TIMESTAMP_PATTERN, Locale.ENGLISH));
			session.transfer(flowFile, SUCCESS);
		} catch (Exception e) {
			flowFile = session.putAttribute(flowFile, "Exception", e.getMessage());
			session.transfer(flowFile, FAILURE);
		}
	}

	@Override
	public Set<Relationship> getRelationships() {
		return relationships;
	}

	@Override
	protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
		return properties;
	}
}
