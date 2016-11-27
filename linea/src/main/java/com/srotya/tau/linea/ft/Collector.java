package com.srotya.tau.linea.ft;

import com.srotya.tau.wraith.Event;

public class Collector {
	
	public void ack(Event event) {
		
	}

	public void emit(Event event, Event ...anchorEvents) {
		for(Event anchorEvent:anchorEvents) {
			event.getSourceIds().add(anchorEvent.getEventId());
		}
	}
		
}
