/**
 * Copyright 2016 Symantec Corporation.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.symcpe.hendrix.interceptor.aws;

import java.io.Serializable;

public class VPCFlowLogRecord implements Serializable {

	private static final long serialVersionUID = 1L;
	private short version;
	private String accountId;
	private String interfaceId;
	private String srcAddr;
	private String dstAddr;
	private int srcPort;
	private int dstPort;
	private char protocol;
	private int packets;
	private int bytes;
	private int startTs;
	private int endTs;
	private boolean accepted;
	private byte logStatus;

	public VPCFlowLogRecord() {
	}
	
	public VPCFlowLogRecord(short version, String accountId, String interfaceId, String srcAddr, String dstAddr,
			int srcPort, int dstPort, char protocol, int packets, int bytes, int startTs, int endTs, boolean accepted,
			byte logStatus) {
		this.version = version;
		this.accountId = accountId;
		this.interfaceId = interfaceId;
		this.srcAddr = srcAddr;
		this.dstAddr = dstAddr;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.protocol = protocol;
		this.packets = packets;
		this.bytes = bytes;
		this.startTs = startTs;
		this.endTs = endTs;
		this.accepted = accepted;
		this.logStatus = logStatus;
	}

	/**
	 * @return the version
	 */
	public short getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(short version) {
		this.version = version;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the interfaceId
	 */
	public String getInterfaceId() {
		return interfaceId;
	}

	/**
	 * @param interfaceId the interfaceId to set
	 */
	public void setInterfaceId(String interfaceId) {
		this.interfaceId = interfaceId;
	}

	/**
	 * @return the srcAddr
	 */
	public String getSrcAddr() {
		return srcAddr;
	}

	/**
	 * @param srcAddr the srcAddr to set
	 */
	public void setSrcAddr(String srcAddr) {
		this.srcAddr = srcAddr;
	}

	/**
	 * @return the dstAddr
	 */
	public String getDstAddr() {
		return dstAddr;
	}

	/**
	 * @param dstAddr the dstAddr to set
	 */
	public void setDstAddr(String dstAddr) {
		this.dstAddr = dstAddr;
	}

	/**
	 * @return the srcPort
	 */
	public int getSrcPort() {
		return srcPort;
	}

	/**
	 * @param srcPort the srcPort to set
	 */
	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	/**
	 * @return the dstPort
	 */
	public int getDstPort() {
		return dstPort;
	}

	/**
	 * @param dstPort the dstPort to set
	 */
	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}

	/**
	 * @return the protocol
	 */
	public char getProtocol() {
		return protocol;
	}

	/**
	 * @param protocol the protocol to set
	 */
	public void setProtocol(char protocol) {
		this.protocol = protocol;
	}

	/**
	 * @return the packets
	 */
	public int getPackets() {
		return packets;
	}

	/**
	 * @param packets the packets to set
	 */
	public void setPackets(int packets) {
		this.packets = packets;
	}

	/**
	 * @return the bytes
	 */
	public int getBytes() {
		return bytes;
	}

	/**
	 * @param bytes the bytes to set
	 */
	public void setBytes(int bytes) {
		this.bytes = bytes;
	}

	/**
	 * @return the startTs
	 */
	public int getStartTs() {
		return startTs;
	}

	/**
	 * @param startTs the startTs to set
	 */
	public void setStartTs(int startTs) {
		this.startTs = startTs;
	}

	/**
	 * @return the endTs
	 */
	public int getEndTs() {
		return endTs;
	}

	/**
	 * @param endTs the endTs to set
	 */
	public void setEndTs(int endTs) {
		this.endTs = endTs;
	}

	/**
	 * @return the accepted
	 */
	public boolean isAccepted() {
		return accepted;
	}

	/**
	 * @param accepted the accepted to set
	 */
	public void setAccepted(boolean accepted) {
		this.accepted = accepted;
	}

	/**
	 * @return the logStatus
	 */
	public byte getLogStatus() {
		return logStatus;
	}

	/**
	 * @param logStatus the logStatus to set
	 */
	public void setLogStatus(byte logStatus) {
		this.logStatus = logStatus;
	}

}