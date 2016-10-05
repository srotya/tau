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
package com.srotya.tau.ui;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import com.srotya.tau.wraith.conditions.Condition;
import com.srotya.tau.wraith.conditions.ConditionSerializer;

/**
 * JSF converter for tenant drop down
 * 
 * @author ambud_sharma
 */
@FacesConverter("conditionConverter")
public class ConditionConverter implements Converter {

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		return ConditionSerializer.deserialize(value); 
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) return null;
	    if (value instanceof String) return (String) value;
	    if (value instanceof Condition) return ConditionSerializer.serialize((Condition)value);
		return null;
	}

}
