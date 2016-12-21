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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.srotya.tau.nucleus.utils.NetUtils;
import com.srotya.tau.nucleus.utils.NetworkUtils;

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
	private AtomicInteger workerCount = new AtomicInteger(0);
	private Map<Integer, WorkerEntry> workerMap;
	private ExecutorService esReceiver = Executors.newSingleThreadExecutor();
	private InetAddress address;
	private int discoveryPort;
	private Random rand = new Random();
	private int evictionTimeThreshold;
	private int selfWorkerId;

	public Columbus(int discoveryPort, int dataPort, int initialTimer, int evictionTimeThreshold, int selfWorkerId)
			throws UnknownHostException, SocketException {
		this.evictionTimeThreshold = evictionTimeThreshold;
		this.selfWorkerId = selfWorkerId;
		NetworkInterface iface = NetworkUtils.selectDefaultIPAddress(false);
		logger.info("Auto-selected network interface:" + iface);
		this.address = NetworkUtils.getIPv4Address(iface);
//		logger.info("Auto-selected IP Address:" + address.getHostAddress());
		this.discoveryPort = discoveryPort;
		this.timer.set(initialTimer);
		workerMap = new ConcurrentHashMap<>();
		addKnownPeer(selfWorkerId, address, discoveryPort, dataPort);
	}

	protected void startReceptionServer(final DatagramSocket dgSocket) {
		esReceiver.submit(new Runnable() {

			@Override
			public void run() {
				receptionLoop(dgSocket);
			}
		});
	}

	protected void startTransmissionServer(int destPort)
			throws SocketException, InvalidStateException, InterruptedException {
		DatagramSocket dgSocket = new DatagramSocket(discoveryPort + new Random().nextInt(), address);
		try {
			dgSocket.setTrafficClass(0x04);
			logger.info("Starting Gossip transmission server");
			while (loopControl.get()) {
				// send gossip
				System.out.println("Sending pings:" + workerMap);
				List<Entry<Integer, WorkerEntry>> pruneList = new ArrayList<>();
				for (Entry<Integer, WorkerEntry> peer : workerMap.entrySet()) {
					if ((System.currentTimeMillis()
							- peer.getValue().getLastContactTimestamp()) > evictionTimeThreshold) {
						if (peer.getValue().getWorkerAddress() != address) {
							pruneList.add(peer);
						}
					} else {
						for (Entry<Integer, WorkerEntry> unicast : workerMap.entrySet()) {
							try {
								ByteBuffer buf = ByteBuffer.allocate(8);
								buf.putInt(unicast.getKey());
								buf.putInt(
										NetUtils.stringIPtoInt(unicast.getValue().getWorkerAddress().getHostAddress()));
								buf.putInt(unicast.getValue().getDiscoveryPort());
								buf.putInt(unicast.getValue().getDataPort());
								DatagramPacket packet = new DatagramPacket(buf.array(), PACKET_PAYLOAD_SIZE);
								packet.setAddress(peer.getValue().getWorkerAddress());
								packet.setPort(peer.getValue().getDiscoveryPort());
								dgSocket.send(packet);
								logger.info("Sending packets to:" + unicast.getValue().getWorkerAddress());
							} catch (IOException e) {
								logger.log(Level.SEVERE, "Failed to send gossip packet", e);
							}
						}
					}
				}
				for (Entry<Integer, WorkerEntry> pruneItem : pruneList) {
					boolean remove = workerMap.remove(pruneItem.getKey(), pruneItem.getValue());
					if (remove) {
						workerCount.decrementAndGet();
						logger.log(Level.INFO, "Lost worker:" + pruneItem);
					}
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
				ByteBuffer data = ByteBuffer.wrap(packet.getData());
				int workerId = data.getInt();
				InetAddress payloadAddress = InetAddress.getByName(NetUtils.toStringIP(data.getInt()));
				int discoveryPort = data.getInt();
				int dataPort = data.getInt();
				addKnownPeer(workerId, payloadAddress, discoveryPort, dataPort);
				Thread.sleep(1000);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Error receiving gossip packet", e);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
			final DatagramSocket dgSocket = new DatagramSocket(discoveryPort, address);
			if (dgSocket.isClosed()) {
				System.exit(-1);
			}
			startReceptionServer(dgSocket);
			dgSocket.setTrafficClass(0x04);
			startTransmissionServer(discoveryPort);
		} catch (SocketException | InvalidStateException e) {
			logger.log(Level.SEVERE, "Exception starting server", e);
		} catch (InterruptedException e) {
			logger.log(Level.SEVERE, "Broadcast loop interrupted", e);
		}
		discoveryPort = -1;
	}

	public void stop(boolean wait) throws InterruptedException {
		loopControl.set(false);
		esReceiver.shutdown();
		while (wait) {
			if (discoveryPort == -1) {
				return;
			} else {
				Thread.sleep(100);
			}
		}
		esReceiver.shutdownNow();
	}

	public void addKnownPeer(int workerId, InetAddress peer, int discoveryPort, int dataPort) {
		WorkerEntry entry = new WorkerEntry(peer, discoveryPort, dataPort, System.currentTimeMillis());
		workerMap.put(workerId, entry);
		workerCount.incrementAndGet();
	}

	public Integer getSelfWorkerId() {
		return selfWorkerId;
	}

	public Map<Integer, WorkerEntry> getWorkerMap() {
		return workerMap;
	}

	public int getWorkerCount() {
		return workerCount.get();
	}

}