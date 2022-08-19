package com.serenegiant.janus;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * VideoSinkを切り替えることができるVideoSink実装
 */
public class ProxyVideoSink implements VideoSink {
	private VideoSink target;

	@Override
	synchronized public void onFrame(final VideoFrame frame) {
		if (target != null) {
			target.onFrame(frame);
		}
	}

	synchronized public void setTarget(final VideoSink target) {
		this.target = target;
	}
}
