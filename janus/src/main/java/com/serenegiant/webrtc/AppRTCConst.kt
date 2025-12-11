package com.serenegiant.webrtc
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

object AppRTCConst {
	const val VIDEO_TRACK_ID: String = "ARDAMSv0"
	const val AUDIO_TRACK_ID: String = "ARDAMSa0"
	const val VIDEO_TRACK_TYPE: String = "video"
	const val VIDEO_CODEC_VP8: String = "VP8"
	const val VIDEO_CODEC_VP9: String = "VP9"
	const val VIDEO_CODEC_H264: String = "H264"
	const val VIDEO_CODEC_H264_BASELINE: String = "H264 Baseline"
	const val VIDEO_CODEC_H264_HIGH: String = "H264 High"
	const val AUDIO_CODEC_OPUS: String = "opus"
	const val AUDIO_CODEC_ISAC: String = "ISAC"
	const val VIDEO_CODEC_PARAM_START_BITRATE: String = "x-google-start-bitrate"
	const val VIDEO_FLEXFEC_FIELDTRIAL: String =
		"WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
	const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL: String = "WebRTC-IntelVP8/Enabled/"
	const val VIDEO_H264_HIGH_PROFILE_FIELDTRIAL: String = "WebRTC-H264HighProfile/Enabled/"
	const val DISABLE_WEBRTC_AGC_FIELDTRIAL: String =
		"WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
	const val AUDIO_CODEC_PARAM_BITRATE: String = "maxaveragebitrate"
	const val AUDIO_ECHO_CANCELLATION_CONSTRAINT: String = "googEchoCancellation"
	const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT: String = "googAutoGainControl"
	const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT: String = "googHighpassFilter"
	const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT: String = "googNoiseSuppression"
	const val DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT: String = "DtlsSrtpKeyAgreement"
	const val HD_VIDEO_WIDTH: Int = 1280
	const val HD_VIDEO_HEIGHT: Int = 720
	const val BPS_IN_KBPS: Int = 1000
	const val RTCEVENTLOG_OUTPUT_DIR_NAME: String = "rtc_event_log"
}
