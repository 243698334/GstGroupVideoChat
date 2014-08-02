package com.kevinychen.gstgroupvideochat.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PortManager {

	// reserve more ports for future pipeline connections
	public static final int PORT_BLOCK_SIZE = 20;

	private static int currentPortLevel;
	
	private static Map<Integer, Integer> portLevelsMap;
	
	private PortManager() {
	}

	public static void loadInitialPortLevel(int portLevel) {
		currentPortLevel = portLevel;
		portLevelsMap = new LinkedHashMap<Integer, Integer>();
	}
	
	public static boolean registerPortLevel(int peerId, int portLevel) {
		if (validatePortLevel(portLevel)) {
			portLevelsMap.put(peerId, portLevel);
			currentPortLevel = portLevel + PORT_BLOCK_SIZE;
			return true;
		} else {
			return false;
		}
	}
	
	public static int getNextPortLevel() {
		return currentPortLevel += PORT_BLOCK_SIZE;
	}
	
	private static boolean validatePortLevel(int portLevel) {
		for (Entry<Integer, Integer> peerIdPortLevelPair : portLevelsMap.entrySet()) {
			if (Math.abs(portLevel - peerIdPortLevelPair.getValue()) < PORT_BLOCK_SIZE) {
				return false;
			}
		}
		return true;
	}
	
}