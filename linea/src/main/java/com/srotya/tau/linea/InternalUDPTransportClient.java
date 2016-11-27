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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.wraith.TauEvent;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class InternalUDPTransportClient implements EventHandler<TauEvent> {

	private ByteBuffer buf = ByteBuffer.allocate(1024);
	private Channel channel;

	public void init() throws Exception {
		EventLoopGroup workerGroup = new NioEventLoopGroup(2);

		Bootstrap b = new Bootstrap();
		channel = b.group(workerGroup).channel(NioDatagramChannel.class).handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline();
			}
		}).bind(0).sync().channel();
		
		channel.closeFuture().await(3000);
		workerGroup.shutdownGracefully().sync();
	}

	public static void main(String[] args) throws Exception {
		// TauEvent event = new TauEvent();
		// event.getHeaders().put("host", "xyz.srotya.com");
		// event.getHeaders().put("message",
		// "ix-dc9-19.ix.netcom.com - - [04/Sep/1995:00:00:28 -0400] \"GET
		// /html/cgi.html HTTP/1.0\" 200 2217\r\n");
		// event.getHeaders().put("value", 10);
		// event.setEventId("13123134234");
		// DatagramSocket soc = new DatagramSocket();
		// for (int i = 0; i < 100; i++) {
		// byte[] bytes =
		// InternalTCPTransportServer.KryoObjectEncoder.eventToByteArray(event,
		// InternalTCPTransportServer.COMPRESS);
		// System.out.println("Writing event:"+i);
		// soc.send(new DatagramPacket(bytes, 0, bytes.length,
		// Inet4Address.getByName("localhost"), 9999));
		// }
		// soc.close();

		InternalUDPTransportClient client = new InternalUDPTransportClient();
		client.init();
	}

	@Override
	public void onEvent(TauEvent event, long sequence, boolean endOfBatch) throws Exception {
		short j = 0;
		buf.putShort(j);
		byte[] bytes = InternalTCPTransportServer.KryoObjectEncoder.eventToByteArray(event,
				InternalTCPTransportServer.COMPRESS);
		if (bytes.length > 1024) {
			// discard
			System.err.println("Discarded event");
		}
		if (buf.remaining() - bytes.length >= 0) {
			buf.put(bytes);
			j++;
		} else {
			buf.putShort(0, j);
			buf.rewind();
			channel.writeAndFlush(
					new DatagramPacket(Unpooled.copiedBuffer(buf), new InetSocketAddress("localhost", 9999)));
			j = 0;
			buf.rewind();
			buf.putShort(j);
			buf.put(bytes);
		}
	}

}
