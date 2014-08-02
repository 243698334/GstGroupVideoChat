package com.kevinychen.gstgroupvideochat.util;

import org.gstreamer.Gst;

public class GStreamerLoader {

	public static void loadLibrary() {
		if (com.sun.jna.Platform.isMac()) {
			final String jnaLibraryPath = System.getProperty("jna.library.path");
			final StringBuilder newJnaLibraryPath = new StringBuilder(
					jnaLibraryPath != null ? (jnaLibraryPath + ":") : "");			
			newJnaLibraryPath.append("/opt/local/lib:");
			System.setProperty("jna.library.path", newJnaLibraryPath.toString());
		}
		Gst.init();
	}
	
}
