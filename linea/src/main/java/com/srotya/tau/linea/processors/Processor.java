package com.srotya.tau.linea.processors;

import java.util.Map;

import com.srotya.tau.wraith.Event;

public interface Processor {
	
	public void configure(Map<String, String> conf);

	public void onEvent(Event event);
	
}
