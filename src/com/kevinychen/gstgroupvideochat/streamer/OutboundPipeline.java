package com.kevinychen.gstgroupvideochat.streamer;

import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;

import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;
import com.sun.jna.Platform;

public class OutboundPipeline implements Runnable {

	private String name;
	private String host;
	private int port;

	private int height;
	private int width;
	private int fps;
	private int hertz;

	private boolean active;

	private Pipeline pipe;

	private Element videoSrc;
	private Element videoFilter;
	private Element videoEnc;
	private Element videoRTPPay;
	private Element videoSink;

	private Element audioSrc;
	private Element audioFilter;
	private Element audioEnc;
	private Element audioRTPPay;
	private Element audioSink;

	private Bin rtpBin;

	private Element videoRTCPsink;
	private Element videoRTCPsrc;

	private Element audioRTCPsink;
	private Element audioRTCPsrc;

	public OutboundPipeline(String host, int port, PeerVideoMode mode, int frameRate) {
		this.name = "defaultName";
		this.host = host;
		this.port = port;
		
		System.out.println("Outbound port = " + port);
		
		this.active=true;
		this.height = mode == PeerVideoMode.ACTIVE ? 480 : 240;
		this.width = mode == PeerVideoMode.ACTIVE ? 640 : 320;
		this.fps = mode == PeerVideoMode.ACTIVE ? frameRate : 10;
		this.hertz = 8000;
	}

	public void startActivePipe() {

		setupPipe();

		setupVideoSource();
		setupVideoFilter();
		setupVideoEncoder();
		setupVideoRTPPay();
		addAndLinkVideoElementsToPipe();

		setupAudioSource();
		setupAudioFilter();
		setupAudioEncoder();
		setupAudioRTPPay();
		addAndLinkAudioElementsToPipe();

		setupRTPbin();
		addRTPbinToPipe();

		setupRTPvideoElements();
		addRTPvideoElementsToPipe();
		linkVideoToRTPbin();

		setupRTPaudioElements();
		addRTPaudioElementsToPipe();
		linkAudioToRTPbin();
	}

	public void startPassivePipe() {

		setupPipe();

		setupVideoSource();
		setupVideoFilter();
		setupVideoEncoder();
		setupVideoRTPPay();
		addAndLinkVideoElementsToPipe();

		setupRTPbin();
		addRTPbinToPipe();

		setupRTPvideoElements();
		addRTPvideoElementsToPipe();
		linkVideoToRTPbin();
	}

	private void setupPipe() {
		pipe = new Pipeline(name);
	}

	private void setupVideoSource() {
		if (Platform.isWindows())
			videoSrc = ElementFactory.make("ksvideosrc", "VideoSrc");
		else if (Platform.isLinux()) 
		{
			videoSrc = ElementFactory.make("v4l2src", "VideoSrc");
			System.out.println("Linux Webcam Created");
		}
		else if (Platform.isMac()) {
			videoSrc = ElementFactory.make("osxvideosrc", "VideoSrc");
			//videoSrc = ElementFactory.make("v4l2src", "VideoSrc");
		}
			
	}

	private void setupVideoFilter() {
		String capsStr = "video/x-raw-yuv," + "width=" + Integer.toString(width) + "," + "height="
				+ Integer.toString(height) + "," + "framerate=" + Integer.toString(fps) + "/1";

		videoFilter = ElementFactory.make("capsfilter", "VideoFilter");
		videoFilter.setCaps(Caps.fromString(capsStr));
	}

	private void setupVideoEncoder() {
		videoEnc = ElementFactory.make("jpegenc", "VideoEncoder");
	}

	private void setupVideoRTPPay() {
		videoRTPPay = ElementFactory.make("rtpjpegpay", "VideoPay");
	}

	private void addAndLinkVideoElementsToPipe() {
		pipe.addMany(videoSrc, videoFilter, videoEnc, videoRTPPay);
		Element.linkMany(videoSrc, videoFilter, videoEnc, videoRTPPay);
	}

	private void setupAudioSource() {
		if (Platform.isWindows())
			audioSrc = ElementFactory.make("dshowaudiosrc", "AudioSrc");
		else if (Platform.isLinux())
			audioSrc = ElementFactory.make("alsasrc", "AudioSrc");
	}

	private void setupAudioFilter() {
		String capsStr = "audio/x-raw-int," + "rate=" + Integer.toString(hertz) + "," + "channels=1";

		audioFilter = ElementFactory.make("capsfilter", "AudioFilter");
		audioFilter.setCaps(Caps.fromString(capsStr));
	}

	private void setupAudioEncoder() {
		audioEnc = ElementFactory.make("alawenc", "AudioEncoder");
	}

	private void setupAudioRTPPay() {
		audioRTPPay = ElementFactory.make("rtppcmapay", "AudioPay");
	}

	private void addAndLinkAudioElementsToPipe() {
		pipe.addMany(audioSrc, audioFilter, audioEnc, audioRTPPay);
		Element.linkMany(audioSrc, audioFilter, audioEnc, audioRTPPay);
	}

	private void setupRTPbin() {
		rtpBin = (Bin) ElementFactory.make("gstrtpbin", "RTPbin");
	}

	private void addRTPbinToPipe() {
		pipe.add(rtpBin);
	}

	private void setupRTPvideoElements() {
		videoSink = ElementFactory.make("udpsink", "VideoSink");
		videoSink.set("host", host);
		videoSink.set("port", port);
		//videoSink.set("clients", HOST + ":" + PORT);

		videoRTCPsink = ElementFactory.make("udpsink", "VideoRTCPsink");
		videoRTCPsink.set("host", host);
		videoRTCPsink.set("port", port + 1);
		//videoRTCPsink.set("clients", HOST + ":" + (PORT + 1));

		videoRTCPsrc = ElementFactory.make("udpsrc", "VideoRTCPsrc");
		videoRTCPsrc.set("port", (port + 2));
	}

	private void addRTPvideoElementsToPipe() {
		pipe.addMany(videoSink, videoRTCPsink, videoRTCPsrc);
	}

	private void linkVideoToRTPbin() {
		Pad sink = rtpBin.getRequestPad("send_rtp_sink_0");
		Pad src = videoRTPPay.getStaticPad("src");
		src.link(sink);

		src = rtpBin.getStaticPad("send_rtp_src_0");
		sink = videoSink.getStaticPad("sink");
		src.link(sink);

		src = rtpBin.getRequestPad("send_rtcp_src_0");
		sink = videoRTCPsink.getStaticPad("sink");
		src.link(sink);

		src = videoRTCPsrc.getStaticPad("src");
		sink = rtpBin.getRequestPad("recv_rtcp_sink_0");
		src.link(sink);
	}

	private void setupRTPaudioElements() {
		audioSink = ElementFactory.make("udpsink", "AudioSink");
		audioSink.set("host", host);
		audioSink.set("port", port + 3);
		//audioSink.set("clients", HOST + ":" + (PORT + 3));

		audioRTCPsink = ElementFactory.make("udpsink", "AudioRTCPsink");
		audioRTCPsink.set("host", host);
		audioRTCPsink.set("port", port + 4);
		//audioRTCPsink.set("clients", HOST + ":" + (PORT + 4));

		audioRTCPsrc = ElementFactory.make("udpsrc", "AudioRTCPsrc");
		audioRTCPsrc.set("port", (port + 5));
	}

	private void addRTPaudioElementsToPipe() {
		pipe.addMany(audioSink, audioRTCPsink, audioRTCPsrc);
	}

	private void linkAudioToRTPbin() {
		Pad sink = rtpBin.getRequestPad("send_rtp_sink_1");
		Pad src = audioRTPPay.getStaticPad("src");
		src.link(sink);

		src = rtpBin.getStaticPad("send_rtp_src_1");
		sink = audioSink.getStaticPad("sink");
		src.link(sink);

		src = rtpBin.getRequestPad("send_rtcp_src_1");
		sink = audioRTCPsink.getStaticPad("sink");
		src.link(sink);

		src = audioRTCPsrc.getStaticPad("src");
		sink = rtpBin.getRequestPad("recv_rtcp_sink_1");
		src.link(sink);
	}

	public void startPipelines() {
		pipe.setState(State.PLAYING);
		System.out.println("Outbound Pipeline Started");
	}

	public void stopPipelines() {
		pipe.setState(State.NULL);
	}

	public void pausePipelines() {
		pipe.setState(State.PAUSED);
	}

	@Override
	public void run() {
		if (active) {
			startActivePipe();
			System.out.println("Outbound Active Pipe");
		} else {
			startPassivePipe();
			System.out.println("Outbound Passive Pipe");
		}

		pipe.setState(State.PLAYING);
		System.out.println("Outbound Pipeline Started");
	}

}
