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
package com.srotya.tau.wraith;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.srotya.tau.wraith.conditions.ConditionSerializer;

/**
 * Utils class for wraith-crux
 * 
 * @author ambud_sharma
 */
public class Utils {

	public static final Charset UTF8 = Charset.forName("utf-8");
	public static final String PROP_NAMING_MAP = "naming.map";
	public static final Map<String, String> CLASSNAME_FORWARD_MAP = new HashMap<>();
	public static final Map<String, String> CLASSNAME_REVERSE_MAP = new HashMap<>();

	static {
		String map = System.getProperty(PROP_NAMING_MAP);
		InputStream stream;
		try {
			if (map == null) {
				stream = ConditionSerializer.class.getClassLoader().getResourceAsStream("naming.default");
				System.out.println("Loading default naming convention");
			} else {
				stream = new FileInputStream(new File(map));
				System.out.println("Found naming map configuration:" + map);
			}
			if (stream != null) {
				List<String> lines = Utils.readAllLinesFromStream(stream);
				for (String line : lines) {
					String[] entry = line.split("=");
					CLASSNAME_FORWARD_MAP.put(entry[0], entry[1]);
					CLASSNAME_REVERSE_MAP.put(entry[1], entry[0]);
				}
			}else {
				System.out.println("Couldn't load the default naming resource");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Utils() {
	}

	public static byte[] eventToBytes(Event event) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ObjectOutputStream stream = new ObjectOutputStream(bytes);
			stream.writeObject(event);
			return bytes.toByteArray();
		} catch (IOException e) {
			// should be an unreachable state
			return null;
		}
	}

	public static List<String> readAllLinesFromStream(InputStream stream) throws IOException {
		List<String> lines = new ArrayList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF8));
		try {
			String temp = null;
			while ((temp = reader.readLine()) != null) {
				if(temp.trim().isEmpty()) {
					continue;
				}
				lines.add(temp);
			}
		} finally {
			reader.close();
		}
		return lines;
	}

	public static void addDeclaredAndInheritedFields(Class<?> c, Collection<Field> fields) {
	    fields.addAll(Arrays.asList(c.getDeclaredFields())); 
	    Class<?> superClass = c.getSuperclass(); 
	    if (superClass != null) { 
	        addDeclaredAndInheritedFields(superClass, fields); 
	    }       
	}
	
	/**
	 * @param timestamp
	 * @param aggregationWindow
	 * @param ruleActionId
	 * @param aggregationKey
	 * @return
	 */
	public static String createMapKey(long timestamp, int aggregationWindow, String ruleActionId,
			String aggregationKey) {
		String ts = intToString(floorTs(timestamp, aggregationWindow));
		return new StringBuilder(ruleActionId.length() + 1 + ts.length() + 1 + aggregationKey.length())
				.append(ruleActionId).append(Constants.KEY_SEPARATOR).append(ts).append(Constants.KEY_SEPARATOR)
				.append(aggregationKey).toString();
	}
	
	/**
	 * @param timestamp
	 * @param aggregationWindow
	 * @return
	 */
	public static int floorTs(long timestamp, int aggregationWindow) {
		return (int) (timestamp / (1000 * aggregationWindow)) * aggregationWindow;
	}

	public static short bytesToShort(byte[] data) {
		return (short) (((data[0] << 8)) | ((data[1] & 0xFF)));
	}

	public static byte[] shortToBytes(short s) {
		return new byte[] { (byte) ((s & 0xFF00) >> 8), (byte) (s & 0x00FF) };
	}

	public static byte[] combineRuleAction(short ruleId, short actionId) {
		return ByteBuffer.allocate(4).putShort(ruleId).putShort(actionId).array();
	}

	public static Entry<Short, Short> separateRuleAction(byte[] ruleAction) {
		ByteBuffer wrap = ByteBuffer.wrap(ruleAction);
		return new AbstractMap.SimpleEntry<Short, Short>(wrap.getShort(), wrap.getShort());
	}

	public static byte[] combineRuleActionTs(short ruleId, short actionId, int ts) {
		return ByteBuffer.allocate(8).putShort(ruleId).putShort(actionId).putInt(ts).array();
	}

	public static Entry<short[], Integer> separateRuleActionTs(byte[] data) {
		ByteBuffer wrap = ByteBuffer.wrap(data);
		return new AbstractMap.SimpleEntry<short[], Integer>(new short[] { wrap.getShort(), wrap.getShort() },
				wrap.getInt());
	}

	public static String combineRuleActionIdTs(short ruleId, short actionId, int ts) {
		return Base64.getEncoder().encodeToString(combineRuleActionTs(ruleId, actionId, ts));
	}

	public static Entry<short[], Integer> separateRuleActionIdTs(String data) {
		return separateRuleActionTs(Base64.getDecoder().decode(data));
	}

	public static byte[] intToBytes(int val) {
		return ByteBuffer.allocate(4).putInt(val).array();
	}

	public static int byteToInt(byte[] val) {
		return ByteBuffer.wrap(val).getInt();
	}

	public static String intToString(int val) {
		return Integer.toHexString(val);
	}

	public static int stringToInt(String val) {
		return Integer.parseInt(val, 16);
	}
	
	public static String longToString(long val) {
		return Long.toHexString(val);
	}

	public static long stringToLong(String val) {
		return Long.parseLong(val, 16);
	}

	public static String combineRuleActionId(short ruleId, short actionId) {
		String str = Base64.getEncoder().encodeToString(combineRuleAction(ruleId, actionId));
		return str;
	}

	public static Entry<Short, Short> separateRuleActionId(String ruleActionId) {
		return separateRuleAction(Base64.getDecoder().decode(ruleActionId));
	}

	/**
	 * @param key
	 * @return
	 */
	public static String[] splitMapKey(String key) {
		return key.split("\\" + Constants.KEY_SEPARATOR);
	}

	/**
	 * @param v
	 * @return
	 */
	public static String concat(String... v) {
		int size = 0;
		for (String vx : v) {
			size += vx.length();
		}
		StringBuilder builder = new StringBuilder(size);
		for (String vx : v) {
			builder.append(vx);
		}
		return builder.toString();
	}

}