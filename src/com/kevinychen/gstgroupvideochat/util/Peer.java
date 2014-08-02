package com.kevinychen.gstgroupvideochat.util;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;
import com.kevinychen.gstgroupvideochat.util.TCPConnection.TCPConnectionType;

public class Peer {
	private int id;
	private String ipAddress;
	private String nickname = null;
	private int inboundFrameRate;
	private int outboundFrameRate;
	private PeerVideoMode inboundVideoMode = null;
	private PeerVideoMode outboundVideoMode = null;
	private BlockingQueue<InternalMessage> inboundBlockingQueue;
	private BlockingQueue<InternalMessage> outboundBlockingQueue;
	private TCPConnection inboundTCPConnection;
	private TCPConnection outboundTCPConnection;
	private int inboundUdpPortLevel;
	private int outboundUdpPortLevel;
	private VideoStreamer videoStreamer;
	
	public Peer(Socket peerSocket, int peerId) {
		this.id = peerId;
		String fullIpAddress = peerSocket.getRemoteSocketAddress().toString();
		this.ipAddress = new String(fullIpAddress.substring(1, fullIpAddress.indexOf(':')));
		System.out.println("PeerManager: [ID=" + peerId + "]: New connection setup.");
		this.inboundBlockingQueue = new LinkedBlockingQueue<InternalMessage>();
		this.outboundBlockingQueue = new LinkedBlockingQueue<InternalMessage>();
		this.inboundTCPConnection = new InboundTCPConnection(peerSocket, peerId, inboundBlockingQueue);
		this.outboundTCPConnection = new OutboundTCPConnection(peerSocket, peerId, outboundBlockingQueue);
	}
	
	public void connect(TCPConnectionType type) {
		if (type == TCPConnectionType.INBOUND) {
			System.out.println("PeerManager: [ID=" + id + "]: Starting InboundTCPConnection thread...");
			new Thread(inboundTCPConnection).start();
		} else {
			System.out.println("PeerManager: [ID=" + id + "]: Starting OutboundTCPConnection thread...");
			new Thread(outboundTCPConnection).start();
		}
	}
	
	public void setNickname(String peerNickname) {
		this.nickname = new String(peerNickname);
	}
	
	public void setVideoStreamer(VideoStreamer videoStreamer) {
		this.videoStreamer = videoStreamer;
	}
	
	public VideoStreamer getVideoStreamer() {
		return this.videoStreamer;
	}
	
	public void setFrameRate(TCPConnectionType type, int frameRate) {
		if (type == TCPConnectionType.INBOUND) {
			this.inboundFrameRate = frameRate;
		} else if (type == TCPConnectionType.OUTBOUND) {
			this.outboundFrameRate = frameRate;
		}
	}
	
	public void setPeerVideoMode(TCPConnectionType type, PeerVideoMode mode) {
		if (type == TCPConnectionType.INBOUND) {
			inboundVideoMode = mode;
		} else if (type == TCPConnectionType.OUTBOUND) {
			outboundVideoMode = mode;
		}
	}
	
	public void setUdpPortLevel(TCPConnectionType type, int portLevel) {
		if (type == TCPConnectionType.INBOUND) {
			inboundUdpPortLevel = portLevel;
		} else if (type == TCPConnectionType.OUTBOUND) {
			outboundUdpPortLevel = portLevel;
		}
	}
	
	public int getUdpPortLevel(TCPConnectionType type) {
		if (type == TCPConnectionType.INBOUND) {
			return inboundUdpPortLevel;
		} else if (type == TCPConnectionType.OUTBOUND) {
			return outboundUdpPortLevel;
		} else return 0;
	}
	
	public int getFrameRate(TCPConnectionType type) {
		if (type == TCPConnectionType.INBOUND) {
			return inboundFrameRate;
		} else if (type == TCPConnectionType.OUTBOUND) {
			return outboundFrameRate;
		} else return 0;
	}
	
	public PeerVideoMode getVideoMode(TCPConnectionType type) {
		if (type == TCPConnectionType.INBOUND) {
			return inboundVideoMode;
		} else if (type == TCPConnectionType.OUTBOUND) {
			return outboundVideoMode;
		} else return null;
	}
	
	public String getIpAddress() {
		return ipAddress;
	}
	
	public String getNickname() {
		return nickname;
	}
	
	public BlockingQueue<InternalMessage> getInboundBlockingQueue() {
		return this.inboundBlockingQueue;
	}
	
	public BlockingQueue<InternalMessage> getOutboundBlockingQueue() {
		return this.outboundBlockingQueue;
	}
	
	public TCPConnection getInboundTCPConnection() {
		return this.inboundTCPConnection;
	}
	
	public TCPConnection getOutboundTCPConnection() {
		return this.outboundTCPConnection;
	}
}
