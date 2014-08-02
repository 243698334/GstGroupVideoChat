package com.kevinychen.gstgroupvideochat.util;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import com.kevinychen.gstgroupvideochat.core.Core;
import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.kevinychen.gstgroupvideochat.core.PortManager;
import com.kevinychen.gstgroupvideochat.util.InternalMessage.MessageType;

public class OutboundTCPConnection extends TCPConnection implements Runnable {

	public OutboundTCPConnection(Socket socket, int peerId, BlockingQueue<InternalMessage> blockingQueue) {
		this.socket = socket;
		this.peerId = peerId;
		this.peerManagerBlockingQueue = blockingQueue;
		try {
			initializeStreams();
		} catch (IOException e) {
			printLog("ERROR", "Failed to initialize input/output streams.");
		}
	}

	@Override
	public void run() {
		System.out.println("OutboundTCPConnection: [ID=" + peerId + "]: Thread start.");
		if (needEstablishConnection) {
			if (establishConnection()) {
				listen();
			}
		} else {
			listen();
		}
	}

	@Override
	protected boolean establishConnection() {
		this.inboundFrameRate = PeerManager.getPeer(peerId).getFrameRate(TCPConnectionType.INBOUND);
		this.inboundPeerVideoMode = PeerManager.getPeer(peerId).getVideoMode(TCPConnectionType.INBOUND);

		try{
		// send connection request
		JSONObject outboundRequestJson = new JSONObject();
		outboundRequestJson.put("message_type", "connection_request");
		outboundRequestJson.put("nickname", Core.getMyNickname());
		outboundRequestJson.put("video_mode", inboundPeerVideoMode == null ? "passive" : inboundPeerVideoMode
				.toString().toLowerCase());
		outboundRequestJson.put("frame_rate", inboundFrameRate);
		outputStream.println(outboundRequestJson.toString());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		// read connection response
		boolean connectionEstablished = false;
		String inboundResponse = null;
		try {
			inboundResponse = inputStream.readLine();
		} catch (IOException e) {
			connectionEstablished = false;
			printLog("ERROR", "Failed to read connection response from Peer");
		}
		if (inboundResponse == null) {
			connectionEstablished = false;
			printLog("ERROR", "Peer is not sending anything as connection response");
		}
		try {
			JSONObject inboundResponseJson = new JSONObject(inboundResponse);
			if (inboundResponseJson.getString("message_type").equals("connection_response")) {
				peerNickname = inboundResponseJson.getString("nickname");
				System.out.println(peerNickname);
				PeerManager.getPeer(peerId).setNickname(peerNickname);
				System.out.println(PeerManager.getPeer(peerId).getNickname());

				if (inboundResponseJson.getBoolean("resource_admission")) {
					tcpConnectionStatus = TCPConnectionStatus.OK;
					connectionEstablished = true;
				} else {
					tcpConnectionStatus = TCPConnectionStatus.fromString(inboundResponseJson.getString("reason"));
					connectionEstablished = false;
				}
				
			} else {
				connectionEstablished = false;
				throw new Exception();
			}
		} catch (Exception e) {
			connectionEstablished = false;
			printLog("ERROR", "Invalid request from peer (Expected: connection_response)");
			e.printStackTrace();
		}

		

		if (connectionEstablished) {
			// UDP port negotiation
			Peer currentPeer = PeerManager.getPeer(peerId);
			int basePortLevel = negotiateUdpPortLevel();
			//int basePortLevel = 56000;
			currentPeer.setUdpPortLevel(TCPConnectionType.INBOUND, basePortLevel);
			currentPeer.setUdpPortLevel(TCPConnectionType.OUTBOUND, basePortLevel + PortManager.PORT_BLOCK_SIZE / 2);

			PeerManager.onOutboundConnectionCreateResponse(peerId, tcpConnectionStatus);
			
			// launch streamer
			VideoStreamer videoStreamer = new VideoStreamer(peerId);
			currentPeer.setVideoStreamer(videoStreamer);
			new Thread(videoStreamer).start();
			System.out.println("OutboundTCPConnection: [ID=" + peerId + "]: Connection established.");
		}

		return true;
	}

	@Override
	protected int negotiateUdpPortLevel() {
		while (true) {
			// send port level offer
			int portLevel = PortManager.getNextPortLevel();
			try {
				JSONObject portLevelOffer = new JSONObject();
				portLevelOffer.put("message_type", "port_negotiation");
				portLevelOffer.put("port_level", portLevel);
				outputStream.println(portLevelOffer.toString());
				printLog("INFO", "Port level offer sent on [" + portLevel + "]");
			} catch (JSONException e) {

			}

			// read response
			String portLevelResponse = null;
			System.out.println("before read: " + portLevelResponse);
			try {
				portLevelResponse = inputStream.readLine();
				System.out.println("Line Received: " + portLevelResponse);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				JSONObject portLevelResponseJson = new JSONObject(portLevelResponse);
				if (portLevelResponseJson.getBoolean("status")) {
					if (PortManager.registerPortLevel(peerId, portLevel)) {
						printLog("INFO", "UDP Port level set at [" + portLevel + "]");
						return portLevel;
					}
				} else {
					continue;
				}
			} catch (JSONException e) {
				System.out.println(portLevelResponse);
				e.printStackTrace();
				continue;
			}
		}
	}

	@Override
	protected void listen() {
		System.out.println("OutboundTCPConnection: [ID=" + peerId + "]: Listening...");
		while (true) {
			try {
				sendControlMessage(peerManagerBlockingQueue.take());
				System.out.println("take from bq");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendControlMessage(InternalMessage internalMessage) {
		if (internalMessage.getMessageType() == MessageType.VIDEO_MODE) {
			try {
				JSONObject outboundRequest = new JSONObject();
				outboundRequest.put("message_type", "video_mode");
				outboundRequest.put("video_mode", internalMessage.get("video_mode"));
				outboundRequest.put("frame_rate", new Integer(internalMessage.get("frame_rate")));
				outputStream.println(outboundRequest.toString());
				printLog("INFO", "Request for changing video_mode sent.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (internalMessage.getMessageType() == MessageType.DISCONNECT) {
			try {
				JSONObject outboundRequest = new JSONObject();
				outboundRequest.put("message_type", "disconnect");
				outputStream.println(outboundRequest.toString());
				printLog("INFO", "Request for disconnect sent.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (internalMessage.getMessageType() == MessageType.RESPONSE) {
			try {
				JSONObject outboundRequest = new JSONObject();
				outboundRequest.put("message_type", "response");
				if (Boolean.parseBoolean(internalMessage.get("status"))) {
					outboundRequest.put("status", true);
				} else {
					outboundRequest.put("status", false);
					outboundRequest.put("reason", internalMessage.get("reason"));
				}
				outputStream.println(outboundRequest.toString());
				printLog("INFO", "Response sent.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			printLog("ERROR", "Unknown internal message type");
		}
	}

	@Override
	protected void printLog(String type, String message) {
		System.out.println("TCPConnection: Outbound [ID=" + peerId + "]: " + type + ": " + message);
	}

}
