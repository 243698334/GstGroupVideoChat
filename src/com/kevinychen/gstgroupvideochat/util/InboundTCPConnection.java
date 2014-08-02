package com.kevinychen.gstgroupvideochat.util;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import com.kevinychen.gstgroupvideochat.core.Core;
import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;
import com.kevinychen.gstgroupvideochat.core.PortManager;
import com.kevinychen.gstgroupvideochat.util.InternalMessage.MessageType;

public class InboundTCPConnection extends TCPConnection implements Runnable {

	public InboundTCPConnection(Socket socket, int peerId, BlockingQueue<InternalMessage> blockingQueue) {
		this.socket = socket;
		this.peerId = peerId;
		String fullIpAddress = socket.getRemoteSocketAddress().toString();
		this.peerIpAddress = new String(fullIpAddress.substring(1, fullIpAddress.indexOf(':')));
		this.peerManagerBlockingQueue = blockingQueue;
		try {
			initializeStreams();
		} catch (IOException e) {
			printLog("ERROR", "Failed to initialize input/output streams.");
		}
	}

	@Override
	public void run() {
		System.out.println("InboundTCPConnection: [ID=" + peerId + "]: Thread start.");
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
		// read connection request
		String inboundRequest = null;
		try {
			inboundRequest = inputStream.readLine();
		} catch (IOException e) {
			printLog("ERROR", "Failed to read connection request.");
		}
		if (inboundRequest == null) {
			printLog("ERROR", "Connection request was empty.");
			return false;
		}
		try {
			JSONObject inboundRequestJson = new JSONObject(inboundRequest);
			if (inboundRequestJson.getString("message_type").equals("connection_request")) {
				peerNickname = new String(inboundRequestJson.getString("nickname"));
				outboundPeerVideoMode = PeerVideoMode.fromString(inboundRequestJson.getString("video_mode"));
				outboundFrameRate = inboundRequestJson.getInt("frame_rate");
				Peer currentPeer = PeerManager.getPeer(peerId);
				currentPeer.setNickname(peerNickname);
				currentPeer.setPeerVideoMode(TCPConnectionType.OUTBOUND, outboundPeerVideoMode);
				currentPeer.setFrameRate(TCPConnectionType.OUTBOUND, outboundFrameRate);
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			printLog("ERROR", "Invalid request (Expected: connection_request)");
		}

		// notify PeerManager and do Resource Admission and send response
		boolean connectionEstablished;
		try {
			JSONObject outboundResponseJson = new JSONObject();
			outboundResponseJson.put("message_type", "connection_response");
			outboundResponseJson.put("nickname", Core.getMyNickname());
			tcpConnectionStatus = PeerManager.onInboundConnectionCreate(peerId);
			switch (tcpConnectionStatus) {
			case OK:
				outboundResponseJson.put("resource_admission", true);
				connectionEstablished = true;
				break;
			case INSUFFICIENT_BANDWIDTH:
				outboundResponseJson.put("resource_admission", false);
				outboundResponseJson.put("reason", "insufficient_bandwidth");
				connectionEstablished = false;
				break;
			case USER_DENIED:
				outboundResponseJson.put("resource_admission", false);
				outboundResponseJson.put("reason", "user_denied");
				connectionEstablished = false;
				break;
			default:
				connectionEstablished = false;
				break;
			}
			outputStream.println(outboundResponseJson.toString());
			
			// terminate thread if necessary
			if (!connectionEstablished) {
				printLog("INFO", "Terminating current thread.");
				Thread.currentThread().interrupt();
				return false;
			} else {
				// UDP port negotiation
				Peer currentPeer = PeerManager.getPeer(peerId);
				int basePortLevel = negotiateUdpPortLevel();
				currentPeer.setUdpPortLevel(TCPConnectionType.INBOUND, basePortLevel);
				currentPeer.setUdpPortLevel(TCPConnectionType.OUTBOUND, basePortLevel + PortManager.PORT_BLOCK_SIZE / 2);
				
				TCPConnection outboundTCPConnection = currentPeer.getOutboundTCPConnection();
				outboundTCPConnection.setNeedEstablishConnection(false);
				new Thread(outboundTCPConnection).start();
				
				// launch streamer
				VideoStreamer videoStreamer = new VideoStreamer(peerId);
				currentPeer.setVideoStreamer(videoStreamer);
				new Thread(videoStreamer).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
	@Override
	protected int negotiateUdpPortLevel() {
		while (true) {
			// read port level offer
			String portLevelOffer = null;
			try {
				portLevelOffer = inputStream.readLine();
			} catch (IOException e) {
				printLog("ERROR", "Failed to read connection request.");
			}
			int portLevel;
			try {
				JSONObject portLevelOfferJson = new JSONObject(portLevelOffer);
				if (!portLevelOfferJson.getString("message_type").equals("port_negotiation")) {
					throw new JSONException(portLevelOffer);
				}
				// perspective of me:
				portLevel = portLevelOfferJson.getInt("port_level");
			} catch (JSONException e) {
				e.printStackTrace();
				continue;
			}
			// validate offer
			if (PortManager.registerPortLevel(peerId, portLevel)) {
				// set port level and send response
				try {
					JSONObject portLevelOfferResponse = new JSONObject();
					portLevelOfferResponse.put("message_type", "port_response");
					portLevelOfferResponse.put("status", true);
					outputStream.println(portLevelOfferResponse.toString());
					printLog("INFO", "UDP Port level set at [" + portLevel + "]");
					return portLevel;
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				// response and read for another offer
				try {
					JSONObject portLevelOfferResponse = new JSONObject();
					portLevelOfferResponse.put("message_type", "port_response");
					portLevelOfferResponse.put("status", false);
					outputStream.println(portLevelOfferResponse.toString());
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected void listen() {
		while (true) {
			try {
				receiveControlMessage(inputStream.readLine());
			} catch (Exception e) {
				printLog("ERROR", "Failed to read from inputStream");
			}
		}
	}

	private void receiveControlMessage(String controlMessage) {
		System.out.println("Line reveived: " + controlMessage);
		if (controlMessage == null) {
			printLog("ERROR", "Null message received.");
		}

		try {
			JSONObject inboundMessage = new JSONObject(controlMessage);
			MessageType messageType = (MessageType.fromString(inboundMessage.getString("message_type")));
			if (messageType == null) {
				printLog("ERROR", "Invalid message type.");
			}
			switch (messageType) {
			case DISCONNECT:
				PeerManager.onInboundConnectionEndCommand(peerId);
				return;
			case RESPONSE:
				if (inboundMessage.getBoolean("status")) {
					PeerManager.onOutboundConnectionUpdateResponse(peerId, true, null);
				} else {
					PeerManager.onOutboundConnectionUpdateResponse(peerId, false, TCPConnectionStatus.fromString(inboundMessage.getString("reason")));
				}
				return;
			case VIDEO_MODE:
				System.out.println("Video mode change request parsed.");
				PeerManager.onInboundConnectionUpdateCommand(peerId,
						PeerVideoMode.fromString(inboundMessage.getString("video_mode")),
						inboundMessage.getInt("frame_rate"));
				return;
			default: // ignoring RESOURCE_ADMISSION
				return;
			}
		} catch (JSONException e) {
			printLog("ERROR", "Invalid request revieved.");
		}
	}

	@Override
	protected void printLog(String type, String message) {
		System.out.println("TCPConnection: Inbound [ID=" + peerId + "]: " + type + ": " + message);
	}

}
