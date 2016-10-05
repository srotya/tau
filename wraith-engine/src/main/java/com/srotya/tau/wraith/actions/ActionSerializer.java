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
package com.srotya.tau.wraith.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srotya.tau.wraith.Required;
import com.srotya.tau.wraith.Utils;
import com.srotya.tau.wraith.conditions.ConditionSerializer;

/**
 * JSON serializer/deserializer for the Actions, naming convention can be
 * supplied via configurable conventions file.
 * 
 * @author ambud_sharma
 */
public class ActionSerializer implements JsonSerializer<Action>, JsonDeserializer<Action> {

	public static final String TYPE = "type";
	public static final String PROPS = "props";

	public static final String PROP_NAMING_MAP = "naming.map";
	private static final Map<String, String> CLASSNAME_FORWARD_MAP = new HashMap<>();
	private static final Map<String, String> CLASSNAME_REVERSE_MAP = new HashMap<>();

	static {
		String map = System.getProperty(PROP_NAMING_MAP);
		InputStream stream;
		try {
			if (map == null) {
				stream = ConditionSerializer.class.getClassLoader().getResourceAsStream("naming.default");
				System.out.println("Loading default naming convection:" + stream);
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
			} else {
				System.out.println("Couldn't load the default naming resource");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		if(jsonObject.entrySet().isEmpty()) {
			throw new JsonParseException("Empty action are not allowed");
		}
		String type = jsonObject.get(TYPE).getAsString();
		if (Utils.CLASSNAME_REVERSE_MAP.containsKey(type)) {
			type = Utils.CLASSNAME_REVERSE_MAP.get(type);
		}
		JsonElement element = jsonObject.get(PROPS);
		try {
			Action pojo = context.deserialize(element, Class.forName(type));
			Field[] fields = pojo.getClass().getDeclaredFields();
			for (Field f : fields) {
				if (f.getAnnotation(Required.class) != null) {
					try {
						f.setAccessible(true);
						if (f.get(pojo) == null) {
							throw new JsonParseException("Missing required field in condition: " + f.getName());
						}
					} catch (IllegalArgumentException | IllegalAccessException ex) {
					}
				}
			}
			return pojo;
		} catch (ClassNotFoundException cnfe) {
			throw new JsonParseException("Unknown action type: " + type, cnfe);
		} catch (NumberFormatException e) {
			throw new JsonParseException("Type must be a number:"+e.getLocalizedMessage());
		}
	}

	public JsonElement serialize(Action src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		String type = src.getClass().getCanonicalName();
		if (Utils.CLASSNAME_FORWARD_MAP.containsKey(src.getClass().getCanonicalName())) {
			type = Utils.CLASSNAME_FORWARD_MAP.get(src.getClass().getCanonicalName());
		}
		result.add(TYPE, new JsonPrimitive(type));
		result.add(PROPS, context.serialize(src, src.getClass()));
		return result;
	}

}