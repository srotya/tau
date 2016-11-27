package com.srotya.tau.linea;

import org.junit.Test;

import com.srotya.tau.linea.network.InternalTCPTransportServer.KryoObjectEncoder;
import com.srotya.tau.wraith.TauEvent;

public class TestKryoSerialization {
	
	@Test
	public void testSerializationSize() throws Exception {
		TauEvent event = new TauEvent();
		event.getHeaders().put("host", "xyz.srotya.com");
		event.getHeaders().put("message", "ix-dc9-19.ix.netcom.com - - [04/Sep/1995:00:00:28 -0400] \"GET /html/cgi.html HTTP/1.0\" 200 2217\r\n");
		event.getHeaders().put("value", 10);
		event.setEventId(13123134234L);
		byte[] ary = KryoObjectEncoder.eventToByteArray(event, false);
		System.out.println("Without Compression Array Length:"+ary.length);
		ary = KryoObjectEncoder.eventToByteArray(event, true);
		System.out.println("With Compression Array Length:"+ary.length);
	}

}
