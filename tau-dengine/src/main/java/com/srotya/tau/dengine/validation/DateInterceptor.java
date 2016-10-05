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
package com.srotya.tau.dengine.validation;

import static org.joda.time.format.ISODateTimeFormat.*;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

import com.google.gson.JsonObject;
import com.srotya.tau.interceptors.ValidationException;
import com.srotya.tau.interceptors.ValidationInterceptor;

/**
 * @author ambud_sharma
 */
public class DateInterceptor extends ValidationInterceptor {

	public static final String TIMESTAMP = "@timestamp";
	public static final String DATEFIELD = "dateinterceptor.datefield";
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
	private String dateField;

	public DateInterceptor() {
		formatter = new DateTimeFormatterBuilder().append(null, formats).toFormatter();
	}

	@Override
	public void configure(Map<String, String> config) {
		if (config.containsKey(DATEFIELD)) {
			dateField = config.get(DATEFIELD);
		} else {
			dateField = TIMESTAMP;
		}
	}

	@Override
	public void validate(JsonObject event) throws ValidationException {
		try {
			DateTime ts = formatter.parseDateTime(event.get(dateField).getAsString());
			event.remove(dateField);
			event.addProperty(dateField, ts.getMillis());
			if (next != null) {
				next.validate(event);
			}
		} catch (Exception e) {
			throw new ValidationException(e.getMessage());
		}
	}

	@Override
	public void validate(Map<String, Object> eventHeaders) throws ValidationException {
		// TODO Auto-generated method stub
		
	}

}
