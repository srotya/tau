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
package com.srotya.tau.linea.clustering;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.srotya.tau.nucleus.utils.NetUtils;

/**
 * Worker disovery service of Linea. Copied from Srotya-Gossip project.
 * 
 * @author ambud
 */
public class Columbus implements Runnable {

	private static final Logger logger = Logger.getLogger(Columbus.class.getName());
	private static final int PACKET_PAYLOAD_SIZE = 4; // 8 bytes for 1 long
														// string
	private AtomicBoolean loopControl = new AtomicBoolean(true);
	private AtomicInteger timer = new AtomicInteger(0);
	private SortedMap<Integer, WorkerEntry> workerMap;
	private ExecutorService esReceiver = Executors.newSingleThreadExecutor();
	private InetAddress address;
	private int port;
	private Random rand = new Random();
	private int evictionTimeThreshold;

	public Columbus(String bindAddress, int port, int initialTimer, int evictionTimeThreshold)
			throws UnknownHostException {
		this.evictionTimeThreshold = evictionTimeThreshold;
		this.address = InetAddress.getByName(bindAddress);
		this.port = port;
		this.timer.set(initialTimer);
		workerMap = new ConcurrentSkipListMap<>();
	}

	protected void startReceptionServer(final DatagramSocket dgSocket) {
		esReceiver.submit(new Runnable() {

			@Override
			public void run() {
				receptionLoop(dgSocket);
			}
		});
	}

	protected void startTransmissionServer(final DatagramSocket dgSocket, int destPort)
			throws SocketException, InvalidStateException, InterruptedException {
		try {
			logger.info("Starting Gossip transmission server");
			byte[] buffer = new byte[PACKET_PAYLOAD_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, PACKET_PAYLOAD_SIZE);
			while (loopControl.get()) {
				// send gossip
				List<Integer> pruneList = new ArrayList<>();
				packet.setPort(destPort);
				for (Entry<Integer, WorkerEntry> peer : workerMap.entrySet()) {
					if ((System.currentTimeMillis()
							- peer.getValue().getLastContactTimestamp()) > evictionTimeThreshold) {
						pruneList.add(peer.getKey());
					} else {
						for (Entry<Integer, WorkerEntry> unicast : workerMap.entrySet()) {
							try {
								packet.setAddress(peer.getValue().getWorkerAddress());
								packet.setData(unicast.getValue().getWorkerAddress().getHostAddress().getBytes());
								dgSocket.send(packet);
							} catch (IOException e) {
								logger.log(Level.SEVERE, "Failed to send gossip packet", e);
							}
						}
					}
				}
				for (Integer pruneItem : pruneList) {
					workerMap.remove(pruneItem);
				}
				Thread.sleep(timer.get() + rand.nextInt(100));
			}
		} finally {
			dgSocket.close();
		}
	}

	protected void receptionLoop(final DatagramSocket dgSocket) {
		byte[] buffer = new byte[PACKET_PAYLOAD_SIZE];
		DatagramPacket packet = new DatagramPacket(buffer, PACKET_PAYLOAD_SIZE);
		while (loopControl.get()) {
			try {
				dgSocket.receive(packet);
				byte[] data = packet.getData();
				if (!workerMap.containsKey(packet.getAddress())) {
					logger.info("Added direct peer:" + packet.getAddress().getHostAddress());
				}
				workerMap.put(NetUtils.stringIPtoInt(packet.getAddress().getHostAddress()),
						new WorkerEntry(packet.getAddress(), System.currentTimeMillis()));
				InetAddress payloadAddress = InetAddress.getByAddress(data);
				if (!workerMap.containsKey(payloadAddress)) {
					logger.info("Discovered new peer:" + payloadAddress.getHostAddress());
					workerMap.put(NetUtils.stringIPtoInt(payloadAddress.getHostAddress()),
							new WorkerEntry(payloadAddress, -1L));
				}
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error receiving gossip packet", e);
			}
		}
	}

	@Override
	public void run() {
		try {
			if (workerMap.size() == 0) {
				throw new InvalidStateException(
						"Either multicast needs to be turned on or a seed of unicast must be provided");
			}
			final DatagramSocket dgSocket = new DatagramSocket(port, address);
			dgSocket.setTrafficClass(0x04);
			startTransmissionServer(dgSocket, port);
		} catch (SocketException | InvalidStateException e) {
			logger.log(Level.SEVERE, "Exception starting server", e);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Broadcast loop interrupted", e);
		}
		port = -1;
	}

	public void stop(boolean wait) throws InterruptedException {
		loopControl.set(false);
		esReceiver.shutdown();
		while (wait) {
			if (port == -1) {
				return;
			} else {
				Thread.sleep(100);
			}
		}
		esReceiver.shutdownNow();
	}

	public void addKnownPeer(String peer) throws UnknownHostException {
		InetAddress peerAddress = InetAddress.getByName(peer);
		workerMap.put(NetUtils.stringIPtoInt(InetAddress.getByName(peer).getHostAddress()),
				new WorkerEntry(peerAddress, -1L));
	}

	public Set<Integer> getPeers() {
		return workerMap.keySet();
	}

	/**
	 * @return
	 */
	public SortedMap<Integer, WorkerEntry> getWorkerMap() {
		return workerMap;
	}
}
