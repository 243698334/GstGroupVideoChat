package com.kevinychen.gstgroupvideochat.streamer;

import org.gstreamer.swing.VideoComponent;

import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;
import com.kevinychen.gstgroupvideochat.ui.VideoChatFrame;

public class Streamer implements Runnable {

	private VideoComponent videoComponent;
	private int peerId;
	private String peerIpAddress;
	private int peerPort;
	private PeerVideoMode peerVideoMode;
	private int frameRate;
	
	public Streamer(int peerId, String peerIpAddress, int peerPort, PeerVideoMode peerVideoMode, int frameRate) {
		this.peerId = peerId;
		this.peerIpAddress = peerIpAddress;
		this.peerPort = peerPort;
		this.peerVideoMode = peerVideoMode;
		this.frameRate = frameRate;
	}
	
	@Override
	public void run() {
		videoComponent = new VideoComponent();
		VideoChatFrame.onConnectionCreate(peerId, videoComponent);
		
	}
	
	public void setPeerVideoMode(PeerVideoMode peerVideoMode) {
		this.peerVideoMode = peerVideoMode;
	}
	
	public void setFrameRate(int frameRate) {
		this.frameRate = frameRate;
	}
	
	public void printStreamerInfo() {
		System.out.print("Streamer for peer [" + peerId + "]: ");
		System.out.print("IP Address: " + peerIpAddress + ", ");
		System.out.print("Port: " + peerPort + ", ");
		System.out.print("Mode: " + peerVideoMode + ", ");
		System.out.println("Frame Rate: " + frameRate);
	}
	
	

}
