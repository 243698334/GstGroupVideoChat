package com.kevinychen.gstgroupvideochat.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;

public abstract class TCPConnection implements Runnable {

	protected int peerId;
	protected BlockingQueue<InternalMessage> peerManagerBlockingQueue;
	protected Socket socket;
	protected BufferedReader inputStream;
	protected PrintStream outputStream;

	protected String peerNickname;
	protected String peerIpAddress;
	protected int peerPort;
	protected boolean peerResourceAdmissionStatus;
	protected PeerVideoMode outboundPeerVideoMode = null;
	protected PeerVideoMode inboundPeerVideoMode = null;
	protected int outboundFrameRate;
	protected int inboundFrameRate;

	protected TCPConnectionStatus tcpConnectionStatus;
	
	protected boolean needEstablishConnection = true;
	
	public static enum TCPConnectionType {
		INBOUND, OUTBOUND;
		public static String toString(TCPConnectionType type) {
			switch (type) {
			case INBOUND:
				return "inbound";
			case OUTBOUND:
				return "outbound";
			default:
				return null;
			}
		}
	}
	
	public static enum TCPConnectionStatus {
		OK, INSUFFICIENT_BANDWIDTH, USER_DENIED;
		public static String toString(TCPConnectionStatus status) {
			switch (status) {
			case OK:
				return "ok";
			case INSUFFICIENT_BANDWIDTH:
				return "insufficient_bandwidth";
			case USER_DENIED:
				return "user_denied";
			default:
				return null;	
			}
		}
		public static TCPConnectionStatus fromString(String status) {
			if (status.equals("ok")) {
				return OK;
			} else if (status.equals("insufficient_bandwidth")) {
				return INSUFFICIENT_BANDWIDTH;
			} else if (status.equals("user_denied")) {
				return USER_DENIED;
			} else {
				return null;
			}
		}
	}
	
	public void setPeerVideoMode(TCPConnectionType type, PeerVideoMode mode) {
		if (type == TCPConnectionType.INBOUND) {
			inboundPeerVideoMode = mode;
		} else if (type == TCPConnectionType.OUTBOUND) {
			outboundPeerVideoMode = mode;
		} else {
			System.out.println("Invalid TCP type");
		}
	}
	
	protected void initializeStreams() throws IOException {
		this.peerIpAddress = this.socket.getRemoteSocketAddress().toString();
		this.peerPort = this.socket.getPort();
		this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		this.outputStream = new PrintStream(socket.getOutputStream());
	}
	
	protected void cleanUpStreams() {
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	abstract protected boolean establishConnection();
	
	/**
	 * For peers outbound-connected, outboundUdpPortLevel < inboundUdpPortLevel.
	 * For peers inbound-connected, inboundUdpPortLevel < outboundPortLevel.
	 * @return the base port level
	 */
	abstract protected int negotiateUdpPortLevel();
	
	abstract protected void listen();

	abstract protected void printLog(String type, String message);

	public String getPeerNickname() {
		return peerNickname;
	}
	
	public String getPeerIpAddress() {
		return peerIpAddress;
	}
	
	public int getPeerPort() {
		return peerPort;
	}
	
	public PeerVideoMode getOutboundPeerVideoMode() {
		return outboundPeerVideoMode;
	}
	
	public PeerVideoMode getInboundPeerVideoMode() {
		return inboundPeerVideoMode;
	}
	
	public void setNeedEstablishConnection(boolean b) {
		needEstablishConnection = b;
	}
	
	public void setPreferredPeerVideoMode(PeerVideoMode preferredMode) {
		inboundPeerVideoMode = preferredMode;
	}

}
