/**
 * Copyright 2016 Ambud Sharma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.linea;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.srotya.tau.wraith.TauEvent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class InternalTCPTransportServer {

	public static final boolean COMPRESS = Boolean
			.parseBoolean(System.getProperty("serialization.compression.enabled", "false"));

	private static final ThreadLocal<Kryo> kryoThreadLocal = new ThreadLocal<Kryo>() {
		@Override
		protected Kryo initialValue() {
			Kryo kryo = new Kryo();
			return kryo;
		}
	};

	static final boolean SSL = System.getProperty("ssl") != null;

	public static void main(String[] args) throws Exception {
		new InternalTCPTransportServer().init();
	}

	public void init() throws Exception {
		final SslContext sslCtx;
		if (SSL) {
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
		} else {
			sslCtx = null;
		}

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup(1);

		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc()));
						}
						p.addLast(new LoggingHandler(LogLevel.INFO));
						p.addLast(new KryoObjectEncoder());
						p.addLast(new KryoObjectDecoder());
						p.addLast(new IWCHandler());
					}

				}).bind(Inet4Address.getByName("localhost"), 9999).sync().channel().closeFuture().await();

	}

	public static class IWCHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			if ((Integer) ((TauEvent) msg).getHeaders().get("sequence") % 1000000 == 0) {
				System.out.println(System.currentTimeMillis());
			}
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) {
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}

	}

	public static class KryoObjectEncoder extends MessageToByteEncoder<TauEvent> {

		public static byte[] eventToByteArray(TauEvent event, boolean compress) throws IOException {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
			OutputStream os = bos;
			if (compress) {
				os = new DeflaterOutputStream(bos);
			}
			Output output = new Output(os);
			kryoThreadLocal.get().writeObject(output, event);
			output.close();
			return bos.toByteArray();
		}

		@Override
		protected void encode(ChannelHandlerContext ctx, TauEvent event, ByteBuf out) throws Exception {
			byte[] byteArray = eventToByteArray(event, COMPRESS);
			out.writeInt(byteArray.length);
			out.writeBytes(byteArray);
		}

	}

	public static class KryoObjectDecoder extends LengthFieldBasedFrameDecoder {

		public KryoObjectDecoder() {
			super(Short.MAX_VALUE, 0, 4, 0, 4);
		}

		public static List<TauEvent> bytebufToEvents(ByteBuf in) {
			short count = in.readShort();
//			System.out.println("Reading " + count + " events");
			ByteBufInputStream bis = new ByteBufInputStream(in);
			InputStream stream = bis;
			if (COMPRESS) {
				stream = new DeflaterInputStream(bis);
			}
			List<TauEvent> events = new ArrayList<>(count);
			Input input = new Input(stream);
			try {
				for (int i = 0; i < count; i++) {
					TauEvent event = kryoThreadLocal.get().readObject(input, TauEvent.class);
//					System.out.println("Event:"+event);
					events.add(event);
				}
				return events;
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			} finally {
				input.close();
			}
		}

		public static TauEvent byteBufToEvent(ByteBuf in) throws IOException {
			ByteBufInputStream bis = new ByteBufInputStream(in);
			InputStream stream = bis;
			if (COMPRESS) {
				stream = new DeflaterInputStream(bis);
			}
			return streamToEvent(stream);
		}

		public static TauEvent streamToEvent(InputStream stream) throws IOException {
			Input input = new Input(stream);
			try {
				TauEvent event = kryoThreadLocal.get().readObject(input, TauEvent.class);
				return event;
			} finally {
				input.close();
			}
		}

		public static List<TauEvent> bytesToEvent(byte[] bytes, int skip, int count) throws IOException {
			List<TauEvent> events = new ArrayList<>();
			Input input = new Input(bytes);
			input.skip(skip);
			try {
				for (int i = 0; i < count; i++) {
					TauEvent event = kryoThreadLocal.get().readObject(input, TauEvent.class);
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
}
