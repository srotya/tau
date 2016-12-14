package com.srotya.tau.linea.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.srotya.tau.wraith.TauEvent;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Decodes {@link Kryo} Serialized {@link TauEvent} objects. This Decoder
 * implementation has refactored utility methods that are used for both TCP and
 * UDP based transports.
 * 
 * @author ambud
 */
public class KryoObjectDecoder extends LengthFieldBasedFrameDecoder {

	public KryoObjectDecoder() {
		super(Short.MAX_VALUE, 0, 4, 0, 4);
	}

	/**
	 * Takes a Netty {@link ByteBuf} as input and returns a list of events
	 * deserialized from the buffer. <br>
	 * The buffer must be length prefixed to get the number of events in the
	 * buffer.
	 * 
	 * @param in
	 * @return list of tauEvents
	 */
	public static List<TauEvent> bytebufToEvents(ByteBuf in) {
		short count = in.readShort();
		ByteBufInputStream bis = new ByteBufInputStream(in);
		InputStream stream = bis;
		if (InternalTCPTransportServer.COMPRESS) {
			stream = new DeflaterInputStream(bis);
		}
		List<TauEvent> events = new ArrayList<>(count);
		Input input = new Input(stream);
		int i = 0;
		try {
			for (; i < count; i++) {
				TauEvent event = InternalTCPTransportServer.kryoThreadLocal.get().readObject(input, TauEvent.class);
				events.add(event);
			}
			return events;
		} catch (Exception e) {
			System.err.println("FAILURE to read count of events:" + count + "\tat i=" + i);
			e.printStackTrace();
			throw e;
		} finally {
			input.close();
		}
	}

	/**
	 * Deserializes a sinlge {@link TauEvent} from a Netty {@link ByteBuf}
	 * 
	 * @param in
	 * @return tauEvent
	 * @throws IOException
	 */
	public static TauEvent byteBufToEvent(ByteBuf in) throws IOException {
		ByteBufInputStream bis = new ByteBufInputStream(in);
		InputStream stream = bis;
		if (InternalTCPTransportServer.COMPRESS) {
			stream = new DeflaterInputStream(bis);
		}
		return streamToEvent(stream);
	}

	/**
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static TauEvent streamToEvent(InputStream stream) throws IOException {
		Input input = new Input(stream);
		try {
			TauEvent event = InternalTCPTransportServer.kryoThreadLocal.get().readObject(input, TauEvent.class);
			return event;
		} finally {
			input.close();
		}
	}

	/**
	 * Deserializes {@link List} of {@link TauEvent}s from a byte array
	 * 
	 * @param bytes
	 * @param skip
	 *            prefix bytes to skip
	 * @param count
	 *            of events to read
	 * @return list of tauEvents
	 * @throws IOException
	 */
	public static List<TauEvent> bytesToEvent(byte[] bytes, int skip, int count) throws IOException {
		List<TauEvent> events = new ArrayList<>();
		Input input = new Input(bytes);
		input.skip(skip);
		try {
			for (int i = 0; i < count; i++) {
				TauEvent event = InternalTCPTransportServer.kryoThreadLocal.get().readObject(input, TauEvent.class);
				events.add(event);
			}
			return events;
		} finally {
			input.close();
		}
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		ByteBuf frame = (ByteBuf) super.decode(ctx, in);
		if (frame == null) {
			return null;
		}
		return byteBufToEvent(in);
	}

}