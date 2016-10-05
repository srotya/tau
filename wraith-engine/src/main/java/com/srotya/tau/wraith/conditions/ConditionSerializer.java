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
package com.srotya.tau.wraith.conditions;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.srotya.tau.wraith.conditions.relational.JavaRegexCondition;

/**
 * Gson Type adapter for {@link Condition} to serialize and deserialize
 * conditions.
 * 
 * @author ambud_sharma
 */
public class ConditionSerializer implements JsonSerializer<Condition>, JsonDeserializer<Condition> {

	public static final String TYPE = "type";
	public static final String PROPS = "props";

	public Condition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
		if (jsonObject.entrySet().isEmpty()) {
			throw new JsonParseException("Empty conditions are not allowed");
		}
		String type = jsonObject.get(TYPE).getAsString();
		if (Utils.CLASSNAME_REVERSE_MAP.containsKey(type)) {
			type = Utils.CLASSNAME_REVERSE_MAP.get(type);
		}
		JsonElement element = jsonObject.get(PROPS);
		try {
			Condition pojo = context.deserialize(element, Class.forName(type));
			if (pojo instanceof JavaRegexCondition) {
				JavaRegexCondition regex = ((JavaRegexCondition) pojo);
				if (regex.getValue() == null) {
					throw new JsonParseException("Regex can't be empty");
				} else {
					try {
						regex.setValue(regex.getValue());
					} catch (PatternSyntaxException e) {
						throw new JsonParseException("Regex " + regex.getValue() + " is not a valid Java regex");
					}
				}
			}
			List<Field> fields = new ArrayList<>();
			Utils.addDeclaredAndInheritedFields(Class.forName(type), fields);
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
			throw new JsonParseException("Unknown condition type: " + type, cnfe);
		}
	}

	public JsonElement serialize(Condition src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		String type = src.getClass().getCanonicalName();
		if (Utils.CLASSNAME_FORWARD_MAP.containsKey(src.getClass().getCanonicalName())) {
			type = Utils.CLASSNAME_FORWARD_MAP.get(src.getClass().getCanonicalName());
		}
		result.add(TYPE, new JsonPrimitive(type));
		result.add(PROPS, context.serialize(src, src.getClass()));
		return result;
	}

	public static Condition deserialize(String condition) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.disableHtmlEscaping();
		Gson gson = gsonBuilder.create();
		return gson.fromJson(condition, Condition.class);
	}

	public static String serialize(Condition condition) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Condition.class, new ConditionSerializer());
		gsonBuilder.disableHtmlEscaping();
		Gson gson = gsonBuilder.create();
		return gson.toJson(condition, Condition.class);
	}

}