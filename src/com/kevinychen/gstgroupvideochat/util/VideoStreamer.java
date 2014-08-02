package com.kevinychen.gstgroupvideochat.util;

import org.gstreamer.swing.VideoComponent;

import com.kevinychen.gstgroupvideochat.core.PeerManager;
import com.kevinychen.gstgroupvideochat.streamer.InboundPipeline;
import com.kevinychen.gstgroupvideochat.streamer.OutboundPipeline;
import com.kevinychen.gstgroupvideochat.ui.VideoChatFrame;
import com.kevinychen.gstgroupvideochat.util.TCPConnection.TCPConnectionType;

public class VideoStreamer implements Runnable {

	private int peerId;
	private int inboundPortLevel;
	private int outboundPortLevel;
	private VideoComponent videoComponent;
	private InboundPipeline inboundPipeline;
	private OutboundPipeline outboundPipeline;

	public VideoStreamer(int peerId) {
		this.peerId = peerId;
		this.videoComponent = new VideoComponent();
		this.inboundPortLevel = PeerManager.getPeer(peerId).getUdpPortLevel(TCPConnectionType.INBOUND);
		this.outboundPortLevel = PeerManager.getPeer(peerId).getUdpPortLevel(TCPConnectionType.OUTBOUND);
		System.out.println("VideoStreamer: [ID=" + peerId + "]: New streamer initialized with inbound port level = ["
				+ inboundPortLevel + "] and outbound port level = [" + outboundPortLevel + "]");
	}

	@Override
	public void run() {
		Peer currentPeer = PeerManager.getPeer(peerId);
		VideoChatFrame.onConnectionCreate(peerId, videoComponent);
		inboundPipeline = new InboundPipeline(videoComponent, currentPeer.getIpAddress(), inboundPortLevel,
				currentPeer.getVideoMode(TCPConnectionType.INBOUND));
		outboundPipeline = new OutboundPipeline(currentPeer.getIpAddress(), outboundPortLevel,
				currentPeer.getVideoMode(TCPConnectionType.OUTBOUND),
				currentPeer.getFrameRate(TCPConnectionType.OUTBOUND));
		new Thread(inboundPipeline).start();
		new Thread(outboundPipeline).start();
		System.out.println("VideoStreamer: [ID=" + peerId + "]: Streamer started");
	}

	public InboundPipeline getInboundPipeline() {
		return inboundPipeline;
	}

	public OutboundPipeline getOutboundPipeline() {
		return outboundPipeline;
	}

}
