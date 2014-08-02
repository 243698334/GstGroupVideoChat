package com.kevinychen.gstgroupvideochat.util;

import java.util.HashMap;

public class InternalMessage extends HashMap<String, String> {

	private static final long serialVersionUID = 1L;
	private MessageType messageType;
	private int peerId;
	
	public InternalMessage(MessageType type, int peerId) {
		this.messageType = type;
		this.peerId = peerId;
	}
	
	public enum MessageType {
		RESOURCE_ADMISSION, VIDEO_MODE, DISCONNECT, RESPONSE;
		public static String toString(MessageType type) {
			switch (type) {
			case RESOURCE_ADMISSION:
				return "resource_admission";
			case VIDEO_MODE:
				return "video_mode";
			case DISCONNECT:
				return "disconnect";
			case RESPONSE:
				return "response";
			default:
				return null;
			}
		}
		public static MessageType fromString(String type) {
			if (type.equals("resource_admission")) {
				return RESOURCE_ADMISSION;
			} else if (type.equals("video_mode")) {
				return VIDEO_MODE;
			} else if (type.equals("disconnect")) {
				return DISCONNECT;
			} else if (type.equals("response")) {
				return RESPONSE;
			} else {
				return null;
			}
		}
	}
	
	public MessageType getMessageType() {
		return messageType;
	}
	
	public int getPeerId() {
		return peerId;
	}
}
