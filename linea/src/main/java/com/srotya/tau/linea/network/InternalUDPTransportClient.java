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
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.lmax.disruptor.EventHandler;
import com.srotya.tau.linea.clustering.Columbus;
import com.srotya.tau.nucleus.utils.NetworkUtils;
import com.srotya.tau.wraith.Constants;
import com.srotya.tau.wraith.Event;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * @author ambud
 *
 */
public class InternalUDPTransportClient implements EventHandler<Event> {

	private static final Logger logger = Logger.getLogger(InternalUDPTransportClient.class.getName());
	private Channel channel;
	private short bufferEventCount;
	private Map<Integer, ByteBuffer> bufferMap;
	private Columbus columbus;
	private int port;

	public InternalUDPTransportClient(Columbus columbus, int port) {
		this.columbus = columbus;
		this.port = port;
		int selfId = columbus.getSelfWorkerId();
		bufferMap = new HashMap<>();
		for (int i = 0; i < columbus.getWorkerCount(); i++) {
			if (i != selfId) {
				ByteBuffer buf = ByteBuffer.allocate(1024);
				buf.putShort(bufferEventCount);
				System.err.println("Buffer map:" + i);
				bufferMap.put(i, buf);
			}
		}
	}

	public void init() throws Exception {
		NetworkInterface iface = NetworkUtils.selectDefaultIPAddress(true);
		Inet4Address address = NetworkUtils.getIPv4Address(iface);
		logger.info("Selected default interface:" + iface.getName() + "\twith address:" + address.getHostAddress());

		EventLoopGroup workerGroup = new NioEventLoopGroup(2);
		Bootstrap b = new Bootstrap();
		channel = b.group(workerGroup).channel(NioDatagramChannel.class).handler(new ChannelInitializer<Channel>() {

			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline();
			}
		}).bind(address, port).sync().channel();

//		workerGroup.shutdownGracefully().sync();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lmax.disruptor.EventHandler#onEvent(java.lang.Object, long,
	 * boolean)
	 */
	@Override
	public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
		Integer workerId = (Integer) event.getHeaders().get(Constants.FIELD_DESTINATION_WORKER_ID);
		try {
			ByteBuffer buf = bufferMap.get(workerId);
			byte[] bytes = InternalTCPTransportServer.KryoObjectEncoder.eventToByteArray(event,
					InternalTCPTransportServer.COMPRESS);
			if (bytes.length > 1024) {
				// discard
				System.err.println("Discarded event");
			}
			if (buf.remaining() - bytes.length >= 0) {
				buf.put(bytes);
				bufferEventCount++;
			} else {
				buf.putShort(0, bufferEventCount);
				buf.rewind();
//				System.out
//						.println("Writing messages:" + bufferEventCount + "\t" + columbus.getWorkerMap().get(workerId));
				channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(buf),
						new InetSocketAddress(columbus.getWorkerMap().get(workerId).getWorkerAddress(),
								columbus.getWorkerMap().get(workerId).getDataPort())));
				bufferEventCount = 0;
				buf.rewind();
				buf.putShort(bufferEventCount);
				buf.put(bytes);
			}
		} catch (Exception e) {
			System.out.println("Exception:"+event+"\tSelf worker ID:"+columbus.getSelfWorkerId()+"\t"+workerId+"\t"+columbus.getWorkerMap());
			throw e;
		}
	}

}
