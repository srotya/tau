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
package com.srotya.tau.linea.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.util.zip.DeflaterOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.srotya.tau.wraith.Event;
import com.srotya.tau.wraith.TauEvent;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * 
 * 
 * @author ambud
 */
public class InternalTCPTransportServer {

	public static final boolean COMPRESS = Boolean
			.parseBoolean(System.getProperty("serialization.compression.enabled", "false"));

	static final ThreadLocal<Kryo> kryoThreadLocal = new ThreadLocal<Kryo>() {
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
						p.addLast(new IWCHandler(null)); //TODO add router
					}

				}).bind(Inet4Address.getByName("localhost"), 9999).sync().channel().closeFuture().await();

	}

	/**
	 * {@link Kryo} serializes {@link TauEvent} for Netty transmission
	 * 
	 * @author ambud
	 */
	public static class KryoObjectEncoder extends MessageToByteEncoder<Event> {

		public static byte[] eventToByteArray(Event event, boolean compress) throws IOException {
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
		protected void encode(ChannelHandlerContext ctx, Event event, ByteBuf out) throws Exception {
			byte[] byteArray = eventToByteArray(event, COMPRESS);
			out.writeInt(byteArray.length);
			out.writeBytes(byteArray);
		}

	}
}
