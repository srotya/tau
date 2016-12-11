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

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.List;
import java.util.logging.Logger;

import com.srotya.tau.linea.network.InternalTCPTransportServer.KryoObjectDecoder;
import com.srotya.tau.linea.network.InternalTCPTransportServer.KryoObjectEncoder;
import com.srotya.tau.nucleus.utils.NetworkUtils;
import com.srotya.tau.wraith.TauEvent;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;

/**
 * @author ambud
 */
public class InternalUDPTransportServer {

	private static final Logger logger = Logger.getLogger(InternalUDPTransportServer.class.getName());
	public static final boolean SSL = System.getProperty("ssl") != null;

	private Channel channel;
	private Router router;
	private int port;
	
	public InternalUDPTransportServer(Router router, int port) {
		this.router = router;
		this.port = port;
	}

	public void start() throws Exception {
		NetworkInterface iface = NetworkUtils.selectDefaultIPAddress(true);
		Inet4Address address = NetworkUtils.getIPv4Address(iface);
		logger.info("Selected default interface:" + iface.getName() + "\twith address:" + address.getHostAddress());

		EventLoopGroup workerGroup = new NioEventLoopGroup(2);

		Bootstrap b = new Bootstrap();
		channel = b.group(workerGroup).channel(NioDatagramChannel.class).handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline().addLast(new KryoDatagramDecoderWrapper()).addLast(new IWCHandler(router));
			}
		}).bind(address, port).sync().channel();
	}
	
	public void stop() throws InterruptedException {
		channel.close().await();
	}

	public static class KryoDatagramDecoderWrapper extends MessageToMessageDecoder<DatagramPacket> {

		@Override
		protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
			ByteBuf buf = msg.content();
			out.addAll(KryoObjectDecoder.bytebufToEvents(buf));
		}

	}

	public static class KryoDatagramEncoderWrapper extends MessageToMessageEncoder<TauEvent> {

		@Override
		protected void encode(ChannelHandlerContext ctx, TauEvent msg, List<Object> out) throws Exception {
			out.add(KryoObjectEncoder.eventToByteArray(msg, InternalTCPTransportServer.COMPRESS));
		}

	}

}
