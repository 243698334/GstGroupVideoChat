package com.kevinychen.gstgroupvideochat.streamer;

import org.gstreamer.Bin;
import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.Pipeline;
import org.gstreamer.State;
import org.gstreamer.swing.VideoComponent;

import com.kevinychen.gstgroupvideochat.core.PeerManager.PeerVideoMode;

public class InboundPipeline implements Runnable {

	private static final String VideoRTPCapsHeader = "application/x-rtp,"
			+ "media=(string)video, clock-rate=(int)90000," + "encoding-name=(string)JPEG, payload=(int)96,"
			+ "ssrc=(uint)1672890742, clock-base=(uint)3321466829," + "seqnum-base=(uint)40515";

	private static final String AudioRTPCapsHeader = "application/x-rtp,"
			+ "media=(string)audio, clock-rate=(int)8000," + "encoding-name=(string)PCMA, payload=(int)8";

	private VideoComponent videoComponent;
	
	private String host;
	private int port;

	private boolean active;

	private Pipeline pipe;

	private Element videoSrc;
	private Element videoDec;
	private Element videoRTPDepay;
	private Element videoSink;

	private Element audioSrc;
	private Element audioDec;
	private Element audioRTPDepay;
	private Element audioSink;

	private Bin rtpBin;

	private Element videoRTCPsrc;
	private Element videoRTCPsink;

	private Element audioRTCPsrc;
	private Element audioRTCPsink;

	public InboundPipeline(VideoComponent videoComponent, String host, int port, PeerVideoMode mode) {

		this.videoComponent = videoComponent;
		this.host = host;
		this.port = port;
		
		System.out.println("Inbound port = " + port);

		this.active = true;
	}

	private void acceptActivePipe() {

		setupPipe();

		setupRTPbin();
		addRTPbinToPipe();

		setupVideoSource();
		setupVideoRTPDepay();
		setupVideoDecoder();
		setupVideoSink();

		setupAudioSource();
		setupAudioRTPDepay();
		setupAudioDecoder();
		setupAudioSink();

		setupRTPvideoElements();
		linkAndaddVideoElementsToPipe();
		linkVideoToRTPbin();

		setupRTPaudioElements();
		linkAndaddAudioElementsToPipe();
		linkAudioToRTPbin();

		setupRTPbinPadAddedCallback();
	}

	private void acceptPassivePipe() {

		setupPipe();

		setupRTPbin();
		addRTPbinToPipe();

		setupVideoSource();
		setupVideoRTPDepay();
		setupVideoDecoder();
		setupVideoSink();

		setupRTPvideoElements();
		linkAndaddVideoElementsToPipe();
		linkVideoToRTPbin();

		setupRTPbinPadAddedCallback();
	}

	private void setupPipe() {
		pipe = new Pipeline("defaultPipelineName");
	}

	private void setupRTPbin() {
		rtpBin = (Bin) ElementFactory.make("gstrtpbin", "RTPbin");
	}

	private void addRTPbinToPipe() {
		pipe.add(rtpBin);
	}

	private void setupVideoSource() {
		videoSrc = ElementFactory.make("udpsrc", "VideoSrc");
		videoSrc.set("port", port);
		videoSrc.setCaps(Caps.fromString(VideoRTPCapsHeader));
	}

	private void setupVideoRTPDepay() {
		videoRTPDepay = ElementFactory.make("rtpjpegdepay", "VideoDepay");
	}

	private void setupVideoDecoder() {
		videoDec = ElementFactory.make("jpegdec", "VideoDecoder");
	}

	private void setupVideoSink() {
		videoSink = videoComponent.getElement();
	}

	private void setupAudioSource() {
		audioSrc = ElementFactory.make("udpsrc", "AudioSrc");

		audioSrc.set("port", (port + 3));
		audioSrc.setCaps(Caps.fromString(AudioRTPCapsHeader));
	}

	private void setupAudioRTPDepay() {
		audioRTPDepay = ElementFactory.make("rtppcmadepay", "AudioDepay");
	}

	private void setupAudioDecoder() {
		audioDec = ElementFactory.make("alawdec", "AudioDecoder");
	}

	private void setupAudioSink() {
		audioSink = ElementFactory.make("autoaudiosink", "AudioSink");
	}

	private void setupRTPvideoElements() {
		videoRTCPsrc = ElementFactory.make("udpsrc", "VideoRTCPsrc");
		videoRTCPsrc.set("port", (port + 1));

		videoRTCPsink = ElementFactory.make("udpsink", "VideoRTCPsink");
		videoRTCPsink.set("host", host);
		videoRTCPsink.set("port", port + 2);
	}

	private void linkAndaddVideoElementsToPipe() {
		pipe.addMany(videoSrc, videoRTCPsrc, videoRTCPsink, videoRTPDepay, videoDec, videoSink);
		Element.linkMany(videoRTPDepay, videoDec, videoSink);
	}

	private void linkVideoToRTPbin() {
		Pad sink = rtpBin.getRequestPad("recv_rtp_sink_0");
		Pad src = videoSrc.getStaticPad("src");
		src.link(sink);

		sink = rtpBin.getRequestPad("recv_rtcp_sink_0");
		src = videoRTCPsrc.getStaticPad("src");
		src.link(sink);

		src = rtpBin.getRequestPad("send_rtcp_src_0");
		sink = videoRTCPsink.getStaticPad("sink");
		src.link(sink);
	}

	private void setupRTPaudioElements() {
		audioRTCPsrc = ElementFactory.make("udpsrc", "AudioRTCPsrc");
		audioRTCPsrc.set("port", (port + 4));

		audioRTCPsink = ElementFactory.make("udpsink", "AudioRTCPsink");
		audioRTCPsink.set("host", host);
		audioRTCPsink.set("port", port + 5);
	}

	private void linkAndaddAudioElementsToPipe() {
		pipe.addMany(audioSrc, audioRTCPsrc, audioRTCPsink, audioRTPDepay, audioDec, audioSink);
		Element.linkMany(audioRTPDepay, audioDec, audioSink);
	}

	private void linkAudioToRTPbin() {
		Pad sink = rtpBin.getRequestPad("recv_rtp_sink_1");
		Pad src = audioSrc.getStaticPad("src");
		src.link(sink);

		sink = rtpBin.getRequestPad("recv_rtcp_sink_1");
		src = audioRTCPsrc.getStaticPad("src");
		src.link(sink);

		src = rtpBin.getRequestPad("send_rtcp_src_1");
		sink = audioRTCPsink.getStaticPad("sink");
		src.link(sink);
	}

	private void setupRTPbinPadAddedCallback() {
		rtpBin.connect(new Element.PAD_ADDED() {
			@Override
			public void padAdded(Element element, Pad pad) {
				String padName = pad.getName();
				System.out.println(padName);
				if (padName.indexOf("recv_rtp_src_0") >= 0) {
					System.out.println("Link " + padName + " to videoRTPDepay!");
					Pad sink = videoRTPDepay.getStaticPad("sink");
					pad.link(sink);
				} else if (padName.indexOf("recv_rtp_src_1") >= 0) {
					System.out.println("Link " + padName + " to audioRTPDepay!");
					Pad sink = audioRTPDepay.getStaticPad("sink");
					pad.link(sink);
				}
			}
		});
	}

	public void startPipelines() {
		pipe.setState(State.PLAYING);
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
			acceptActivePipe();
			System.out.println("Inbound Active Pipe");
		} else {
			acceptPassivePipe();
			System.out.println("Inbound Passive Pipe");
		}

		pipe.setState(State.PLAYING);
		System.out.println("Inbound Pipeline Started");
	}
}
