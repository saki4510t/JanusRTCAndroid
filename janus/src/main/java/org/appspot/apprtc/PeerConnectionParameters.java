package org.appspot.apprtc;
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
import android.util.Log;

import static org.appspot.apprtc.AppRTCConst.*;

/**
 * Peer connection parameters.
 */
public class PeerConnectionParameters {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = PeerConnectionParameters.class.getSimpleName();

	public final boolean videoCallEnabled;
	public final boolean loopback;
	public final boolean tracing;
	public final int videoWidth;
	public final int videoHeight;
	public final int videoFps;
	public final int videoMaxBitrate;
	public final String videoCodec;
	public final boolean videoCodecHwAcceleration;
	public final boolean videoFlexfecEnabled;
	public final int audioSource;
	public final int audioFormat;
	public final int audioStartBitrate;
	public final String audioCodec;
	public final boolean noAudioProcessing;
	public final boolean aecDump;
	public final boolean saveInputAudioToFile;
	public final boolean useOpenSLES;
	public final boolean disableBuiltInAEC;
	public final boolean disableBuiltInAGC;
	public final boolean disableBuiltInNS;
	public final boolean disableWebRtcAGCAndHPF;
	public final boolean enableRtcEventLog;
	public final DataChannelParameters dataChannelParameters;
	
	public PeerConnectionParameters(boolean videoCallEnabled, boolean loopback, boolean tracing,
		int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
		boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled,
		int audioSource, int audioFormat, int audioStartBitrate, String audioCodec,
		boolean noAudioProcessing, boolean aecDump, boolean saveInputAudioToFile,
		boolean useOpenSLES, boolean disableBuiltInAEC, boolean disableBuiltInAGC,
		boolean disableBuiltInNS, boolean disableWebRtcAGCAndHPF, boolean enableRtcEventLog,
		DataChannelParameters dataChannelParameters) {

		this.videoCallEnabled = videoCallEnabled;
		this.loopback = loopback;
		this.tracing = tracing;
		this.videoWidth = videoWidth;
		this.videoHeight = videoHeight;
		this.videoFps = videoFps;
		this.videoMaxBitrate = videoMaxBitrate;
		this.videoCodec = videoCodec;
		this.videoFlexfecEnabled = videoFlexfecEnabled;
		this.videoCodecHwAcceleration = videoCodecHwAcceleration;
		this.audioSource = audioSource;
		this.audioFormat = audioFormat;
		this.audioStartBitrate = audioStartBitrate;
		this.audioCodec = audioCodec;
		this.noAudioProcessing = noAudioProcessing;
		this.aecDump = aecDump;
		this.saveInputAudioToFile = saveInputAudioToFile;
		this.useOpenSLES = useOpenSLES;
		this.disableBuiltInAEC = disableBuiltInAEC;
		this.disableBuiltInAGC = disableBuiltInAGC;
		this.disableBuiltInNS = disableBuiltInNS;
		this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF;
		this.enableRtcEventLog = enableRtcEventLog;
		this.dataChannelParameters = dataChannelParameters;
	}

	public String getSdpVideoCodecName() {

		switch (this.videoCodec) {
		case VIDEO_CODEC_VP8:
			return VIDEO_CODEC_VP8;
		case VIDEO_CODEC_VP9:
			return VIDEO_CODEC_VP9;
		case VIDEO_CODEC_H264:
		case VIDEO_CODEC_H264_HIGH:
		case VIDEO_CODEC_H264_BASELINE:
			return VIDEO_CODEC_H264;
		default:
			return VIDEO_CODEC_VP8;
		}
	}

	public String getFieldTrials() {

		String fieldTrials = "";
		if (this.videoFlexfecEnabled) {
			fieldTrials += VIDEO_FLEXFEC_FIELDTRIAL;
			if (DEBUG) Log.d(TAG, "Enable FlexFEC field trial.");
		}
		fieldTrials += VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL;
		if (this.disableWebRtcAGCAndHPF) {
			fieldTrials += DISABLE_WEBRTC_AGC_FIELDTRIAL;
			if (DEBUG) Log.d(TAG, "Disable WebRTC AGC field trial.");
		}
		if (VIDEO_CODEC_H264_HIGH.equals(this.videoCodec)) {
			// TODO(magjed): Strip High from SDP when selecting Baseline instead of using field trial.
			fieldTrials += VIDEO_H264_HIGH_PROFILE_FIELDTRIAL;
		}
		return fieldTrials;
	}
	

}
