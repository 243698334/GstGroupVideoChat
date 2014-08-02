package com.kevinychen.gstgroupvideochat.core;

import com.kevinychen.gstgroupvideochat.ui.SettingsFrame;
import com.kevinychen.gstgroupvideochat.ui.VideoChatFrame;
import com.kevinychen.gstgroupvideochat.util.GStreamerLoader;

public class Core {

	private static Core instance = null;

	private static String myNickname;
	private static int peerIdCounter;
	public final static String copyrightText = "Presented by Yufei Chen, Chi Zhou, Bing-Jui Ho, Zhongyuan Xie";

	private Core() {
	}

	public static Core getInstance() {
		return instance;
	}

	public static void start() {
		instance = new Core();
		peerIdCounter = -1;
		GStreamerLoader.loadLibrary();
		SettingsFrame.loadInterface(true);
		VideoChatFrame.loadInterface(false);
		PeerManager.onStartUp();
	}

	public static String getMyNickname() {
		return myNickname;
	}
	
	public static void setMyNickname(String nickname) {
		myNickname = new String(nickname);
	}
	
	public static synchronized int getNextPeerId() {
		return ++peerIdCounter;
	}
	
	public static synchronized int getCurrentPeerId() {
		return peerIdCounter;
	}
	
	public static void onClientExit() {
		// TODO Auto-generated method stub
		// clean up
	}

}
