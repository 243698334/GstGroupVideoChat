package com.kevinychen.gstgroupvideochat.core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JOptionPane;

import org.json.JSONObject;

import com.kevinychen.gstgroupvideochat.ui.ResourceSettingsPanel;
import com.kevinychen.gstgroupvideochat.ui.SettingsFrame;
import com.kevinychen.gstgroupvideochat.ui.WaitForPeerFrame;
import com.kevinychen.gstgroupvideochat.util.InternalMessage;
import com.kevinychen.gstgroupvideochat.util.InternalMessage.MessageType;
import com.kevinychen.gstgroupvideochat.util.Peer;
import com.kevinychen.gstgroupvideochat.util.TCPConnection;
import com.kevinychen.gstgroupvideochat.util.TCPConnection.TCPConnectionStatus;
import com.kevinychen.gstgroupvideochat.util.TCPConnection.TCPConnectionType;

/**
 * This class is responsible for resource admission, TCP negotiation. It also
 * starts multiple threads for video chats from different peers. TODO: invoke
 * MediaStreamer class
 */
public class PeerManager {

	public static boolean resourceFileLoaded = false;
	public static boolean resourceAdmissionOk = false;

	private static int tcpListenPort;
	private static long totalBandwidth;
	private static long availableBandwidth;
	private static int peersConnected;

	private static Map<Integer, Peer> peersMap;

	private PeerManager() {
	}

	public static enum PeerVideoMode {
		ACTIVE, PASSIVE;
		public static String toString(PeerVideoMode mode) {
			if (mode == PeerVideoMode.ACTIVE) {
				return "active";
			} else {
				return "passive";
			}
		}

		public static PeerVideoMode fromString(String mode) {
			if (mode.equals("active")) {
				return PeerVideoMode.ACTIVE;
			} else if (mode.equals("passive")) {
				return PeerVideoMode.PASSIVE;
			} else {
				return null;
			}
		}
	}
	
	public static void onStartUp() {
		peersMap = new LinkedHashMap<Integer, Peer>();
	}

	public static Peer getPeer(int peerId) {
		return peersMap.get(peerId);
	}

	public static Map<Integer, String> getPeerNicknameList() {
		Map<Integer, String> peerNicknameList = new LinkedHashMap<Integer, String>();
		for (Map.Entry<Integer, Peer> currentPeer : peersMap.entrySet()) {
			peerNicknameList.put(currentPeer.getKey(), currentPeer.getValue().getNickname());
		}
		return peerNicknameList;
	}

	private static void listen() {
		System.out.println("PeerManager: Start listening TCP port: [" + tcpListenPort + "]");
		class TCPListener implements Runnable {
			ServerSocket mSocket;
			Socket peerSocket;

			@Override
			public void run() {
				try {
					mSocket = new ServerSocket(tcpListenPort);
				} catch (Exception ex) {
					System.out.println("PeerManager: ERROR: TCP failed on port: [" + tcpListenPort + "]");
				}
				while (true) {
					System.out.println("PeerManager: INFO: Listener thread starts.");
					try {
						peerSocket = mSocket.accept();
					} catch (IOException ex) {
						System.out.println("ConnectionManager: ERROR: Failed to accept a peer socket");
					}
					System.out.println("PeerManager: INFO: Incoming connection.");
					int peerId = Core.getNextPeerId();
					Peer newPeer = new Peer(peerSocket, peerId);
					peersMap.put(peerId, newPeer);
					newPeer.connect(TCPConnectionType.INBOUND);
				}
			}
		}
		new Thread(new TCPListener()).start();
	}

	/**
	 * Load the given resource file and set the resourceAdmissionOk public
	 * boolean.
	 * 
	 * @param resourceFile
	 * @return true if there is no error reading the file. false otherwise.
	 */
	@SuppressWarnings("resource")
	public static boolean loadResourceFile(File resourceFile) {
		try {
			String fileContent = new Scanner(resourceFile).useDelimiter("\\Z").next();
			JSONObject resourceJson = new JSONObject(fileContent);
			long newTotalBandwidth = resourceJson.getLong("bandwidth");
			PortManager.loadInitialPortLevel(resourceJson.getInt("udp_port_level"));
			if (resourceFileLoaded) {
				if (newTotalBandwidth < totalBandwidth - availableBandwidth) {
					resourceAdmissionOk = false;
					System.out.println("ConnectionManager: ERROR: Insufficient bandwidth.");
					return true;
				} else {
					availableBandwidth = newTotalBandwidth - (totalBandwidth - availableBandwidth);
					totalBandwidth = newTotalBandwidth;
					resourceAdmissionOk = true;
				}
			} else {
				availableBandwidth = totalBandwidth = newTotalBandwidth;
				peersConnected = 0;
				resourceAdmissionOk = true;
			}
			System.out.println("ConnectionManager: Resource file loaded.");
			printStatus();
			listen();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("ConnectionManager: ERROR: Failed to load the resource file.");
			return false;
		}
	}

	/**
	 * Callback method for InboundTCPConnection after establishing the
	 * connection. Tasks include resource admission, update UI for user's
	 * approval and remove unnecessary instances of
	 * TCPConnections/BlockingQueues on failure.
	 * 
	 * @param peerId
	 * @return the status of resource admission and user's approval
	 */
	public static synchronized TCPConnectionStatus onInboundConnectionCreate(int peerId) {
		Peer currentPeer = peersMap.get(peerId);
		TCPConnection inboundTCPConnection = currentPeer.getInboundTCPConnection();
		String peerIpAddress = inboundTCPConnection.getPeerIpAddress();
		int peerPort = inboundTCPConnection.getPeerPort();
		String peerNickname = inboundTCPConnection.getPeerNickname();
		PeerVideoMode outboundVideoMode = inboundTCPConnection.getOutboundPeerVideoMode();
		long requiredBandwidth = resourceAdmission(PeerVideoMode.PASSIVE, outboundVideoMode, 10,
				currentPeer.getFrameRate(TCPConnectionType.INBOUND));
		if (requiredBandwidth != -1) {
			// update UI and get user's approval
			SettingsFrame.setSettingsWindowVisible(true);
			int userChoice = JOptionPane.showConfirmDialog(SettingsFrame.getInstance(), "Start video chat with "
					+ peerNickname + "?", "New Connection", JOptionPane.YES_NO_OPTION);
			if (userChoice == JOptionPane.YES_OPTION) {
				SettingsFrame.addPeerSettingsTab(peerId, peerIpAddress, peerPort, peerNickname);
				availableBandwidth -= requiredBandwidth;
				ResourceSettingsPanel.updateAvailableBandwidth(0 - requiredBandwidth);
				TCPConnection outboundTCPConnection = currentPeer.getOutboundTCPConnection();
				outboundTCPConnection.setNeedEstablishConnection(false);
				new Thread(outboundTCPConnection).start();
				return TCPConnectionStatus.OK;
			} else {
				SettingsFrame.setSettingsWindowVisible(false);
				// TODO clear instances
				return TCPConnectionStatus.USER_DENIED;
			}
		} else {
			SettingsFrame.setSettingsWindowVisible(false);
			// TODO clear instances
			return TCPConnectionStatus.INSUFFICIENT_BANDWIDTH;
		}
	}

	/**
	 * Used by PeerSettingsPanel to create an outboundTCPConnection.
	 * 
	 * @return resource admission status
	 */
	public static synchronized boolean onOutboundConnectionCreateCommand(int peerId, String peerIpAddress,
			int peerPort, PeerVideoMode peerVideoMode, int inboundFrameRate) {
		// On default, outboundVideoMode is passive
		if (resourceAdmission(peerVideoMode, PeerVideoMode.PASSIVE, inboundFrameRate, 10) != -1) {
			Socket peerSocket = null;
			try {
				peerSocket = new Socket(peerIpAddress, peerPort);
			} catch (UnknownHostException e) {
				WaitForPeerFrame.loadErrorInterface(true, "Unknown Host.");
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				WaitForPeerFrame.loadErrorInterface(true, "Failed to connect to peer.");
				e.printStackTrace();
				return false;
			}
			Peer newPeer = new Peer(peerSocket, peerId);
			peersMap.put(peerId, newPeer);
			newPeer.setFrameRate(TCPConnectionType.INBOUND, inboundFrameRate);
			newPeer.setPeerVideoMode(TCPConnectionType.INBOUND, peerVideoMode);
			newPeer.connect(TCPConnectionType.OUTBOUND);
			return true;
		} else {
			WaitForPeerFrame.loadErrorInterface(true, "Insufficient bandwidth.");
			return false;
		}
	}

	/**
	 * Callback method for OutboundTCPConnection after establishing the
	 * connection. Tasks include report peer's resource admission status, update
	 * UI for user's notice and remove unnecessary instances of
	 * TCPConnections/BlockingQueues on failure
	 * 
	 * @param peerId
	 * @return
	 */
	public static synchronized void onOutboundConnectionCreateResponse(int peerId, TCPConnectionStatus status) {
		System.out.println("PeerManager: [ID=" + peerId + "]: Outbound connection response received and parsed.");
		Peer currentPeer = peersMap.get(peerId);
		TCPConnection outboundTCPConnection = currentPeer.getOutboundTCPConnection();
		String peerNickname = outboundTCPConnection.getPeerNickname();
		switch (status) {
		case OK:
			WaitForPeerFrame.getInstance().setVisible(false);
			TCPConnection inboundTCPConnection = currentPeer.getInboundTCPConnection();
			inboundTCPConnection.setNeedEstablishConnection(false);
			new Thread(inboundTCPConnection).start();
			SettingsFrame.updateTabTitle(peerId, currentPeer.getNickname());
			return;
		case INSUFFICIENT_BANDWIDTH:
			WaitForPeerFrame.loadErrorInterface(false, peerNickname + " has insufficient bandwidth.");
			break;
		case USER_DENIED:
			WaitForPeerFrame.loadErrorInterface(false, peerNickname + " has denied your request.");
			break;
		}
		// clear instances
	}

	/**
	 * Used by InboundTCPConnection
	 */
	public static synchronized void onInboundConnectionUpdateCommand(int peerId, PeerVideoMode outboundVideoMode,
			int outboundFrameRate) {
		System.out.println("onInboundConnectionUpdateCommand() called");
		// check for no change
		boolean noChange = false;
		Peer currentPeer = peersMap.get(peerId);
		if (currentPeer.getVideoMode(TCPConnectionType.OUTBOUND) == outboundVideoMode
				&& currentPeer.getFrameRate(TCPConnectionType.OUTBOUND) == outboundFrameRate) {
			noChange = true;
		}
		// resource admission and update peer info
		if (!noChange) {
			long requiredBandwidth = resourceAdmission(currentPeer.getVideoMode(TCPConnectionType.INBOUND),
					outboundVideoMode, currentPeer.getFrameRate(TCPConnectionType.INBOUND), outboundFrameRate);
			if (requiredBandwidth != -1) {
				// resource admission success
				currentPeer.setPeerVideoMode(TCPConnectionType.OUTBOUND, outboundVideoMode);
				currentPeer.setFrameRate(TCPConnectionType.OUTBOUND, outboundFrameRate);
				onInboundConnectionUpdateResponse(peerId, TCPConnectionStatus.OK);
			} else {
				// resource admission failed
				onInboundConnectionUpdateResponse(peerId, TCPConnectionStatus.INSUFFICIENT_BANDWIDTH);
			}
		} else {
			onInboundConnectionUpdateResponse(peerId, TCPConnectionStatus.OK);
		}
	}

	/**
	 * Used by onInboundConnectionUpdateCommand()
	 * 
	 * @param peerId
	 * @param status
	 */
	public static synchronized void onInboundConnectionUpdateResponse(int peerId, TCPConnectionStatus status) {
		System.out.println("onInboundConnectionUpdateResponse() called");
		Peer currentPeer = peersMap.get(peerId);
		InternalMessage updateResponse = new InternalMessage(MessageType.RESPONSE, peerId);
		switch (status) {
		case OK:
			updateResponse.put("status", "true");
			break;
		default:
			updateResponse.put("status", "false");
			updateResponse.put("reason", TCPConnectionStatus.toString(status));
			break;
		}
		try {
			currentPeer.getOutboundBlockingQueue().put(updateResponse);
		} catch (InterruptedException e) {
			System.out.println("PeerManager: [ID=" + peerId + "]: ERROR: BlockingQueue failed.");
			e.printStackTrace();
		}
	}

	/**
	 * Used by PeerSettingsFrame.
	 * 
	 * @param peerId
	 * @param frameRate
	 * @return
	 */
	public static void onOutboundConnectionUpdateCommand(int peerId, PeerVideoMode peerVideoMode, int frameRate) {
		Peer currentPeer = peersMap.get(peerId);
		boolean noChange = false;
		if (currentPeer.getVideoMode(TCPConnectionType.INBOUND) == peerVideoMode
				&& currentPeer.getFrameRate(TCPConnectionType.INBOUND) == frameRate) {
			noChange = true;
		}
		if (!noChange) {
			InternalMessage updateCommand = new InternalMessage(MessageType.VIDEO_MODE, peerId);
			updateCommand.put("video_mode", PeerVideoMode.toString(peerVideoMode));
			updateCommand.put("frame_rate", Integer.toString(frameRate));
			try {
				currentPeer.getOutboundBlockingQueue().put(updateCommand);
			} catch (InterruptedException e) {
				System.out.println("PeerManager: [ID=" + peerId + "]: ERROR: BlockingQueue failed.");
				e.printStackTrace();
			}
		} else {
			WaitForPeerFrame.getInstance().setVisible(false);
		}
	}

	/**
	 * Used by InboundTCPConnection
	 * 
	 * @param peerId
	 * @param status
	 * @param reason
	 */
	public static synchronized void onOutboundConnectionUpdateResponse(int peerId, boolean status,
			TCPConnectionStatus tcpStatus) {
		if (status == true) {
			WaitForPeerFrame.getInstance().setVisible(false);
		} else {
			if (tcpStatus == TCPConnectionStatus.INSUFFICIENT_BANDWIDTH) {
				WaitForPeerFrame.loadErrorInterface(false, getPeer(peerId).getNickname()
						+ " has insufficient bandwidth.");
			} else if (tcpStatus == TCPConnectionStatus.USER_DENIED) {
				WaitForPeerFrame.loadErrorInterface(false, getPeer(peerId).getNickname() + " has denied your request.");
			}
		}
	}

	/**
	 * Used by InboundTCPConnection
	 * 
	 * @param peerId
	 * @return boolean returned back to TCP thread for confirmation of cleanup
	 */
	public static synchronized boolean onInboundConnectionEndCommand(int peerId) {
		Peer currentPeer = peersMap.get(peerId);
		JOptionPane.showMessageDialog(SettingsFrame.getInstance(), "Your connection with " + currentPeer.getNickname()
				+ " is terminated on the other end.");
		SettingsFrame.removePeerSettingsTab(SettingsFrame.getTabIndexById(peerId));
		return true;
	}

	/**
	 * Used by PeerSettingsPanel
	 * 
	 * @param peerId
	 * @return
	 */
	public static synchronized boolean onOutboundConnectionEndCommand(int peerId) {
		InternalMessage disconnectCommand = new InternalMessage(MessageType.DISCONNECT, peerId);
		try {
			peersMap.get(peerId).getOutboundBlockingQueue().put(disconnectCommand);
			return true;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Resource Admission
	 * 
	 * @return the required bandwidth for a peer or -1 if insufficient
	 *         bandwidth.
	 */
	private static synchronized long resourceAdmission(PeerVideoMode inboundMode, PeerVideoMode outboundMode,
			int inboundFrameRate, int outboundFrameRate) {
		long audioRate = 8000 * 16;
		long inboundBandwidth = inboundMode == PeerVideoMode.ACTIVE ? 640 * 480 * inboundFrameRate + audioRate
				: 320 * 240 * 10;
		long outboudBandwidth = outboundMode == PeerVideoMode.ACTIVE ? 640 * 480 * outboundFrameRate + audioRate
				: 320 * 240 * 10;
		long requiredBandwidth = inboundBandwidth + outboudBandwidth;
		return requiredBandwidth <= availableBandwidth ? requiredBandwidth : -1;
	}

	private static void printStatus() {
		System.out.println("ConnectionManager: Total bandwidth: [" + Long.toString(totalBandwidth) + "]");
		System.out.println("ConnectionManager: Available bandwidth: [" + Long.toString(availableBandwidth) + "]");
		System.out.println("ConnectionManager: Peers connected: [" + peersConnected + "]");

	}

	public static void setTCPListenPort(int port) {
		tcpListenPort = port;
	}

	public static long getTotalBandwidth() {
		return totalBandwidth;
	}

	public static long getAvailableBandwidth() {
		return availableBandwidth;
	}

	public static int getPeersConnected() {
		return peersConnected;
	}
}
