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
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.serenegiant.media.AudioRecordCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static org.appspot.apprtc.AppRTCConst.*;

/**
 * Peer connection parameters.
 */
public class PeerConnectionParameters {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = PeerConnectionParameters.class.getSimpleName();

	/**
	 * PeerConnectionParameters生成のためのビルダークラス
	 */
	public static class Builder {
		private boolean videoCallEnabled;
		private boolean loopback = false;
		private boolean tracing = false;
		private int videoWidth;
		private int videoHeight;
		private int videoFps = 0;
		private int videoMaxBitrate = 0;	// 0: 制限無し
		private String videoCodec = VIDEO_CODEC_VP8;
		private boolean videoCodecHwAcceleration = true;
		private boolean videoFlexfecEnabled = false;
		private int audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
		private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		private int audioStartBitrate = 0;	// 0: 制限無し
		private String audioCodec = AUDIO_CODEC_OPUS;
		private boolean noAudioProcessing = false;
		private boolean aecDump = false;
		private boolean saveInputAudioToFile = false;
		private boolean useOpenSLES = false;
		private boolean disableBuiltInAEC = false;
		private boolean disableBuiltInAGC = false;
		private boolean disableBuiltInNS = false;
		private boolean disableWebRtcAGCAndHPF = false;
		private boolean enableRtcEventLog = false;
		@Nullable
		private DataChannelParameters dataChannelParameters;

		/**
		 * デフォルトコンストラクタ
		 */
		public Builder() {
		}

		/**
		 * 既存のBuilderの内容を引き継いで新しいBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		public Builder(@NonNull final Builder src) {
			videoCallEnabled = src.videoCallEnabled;
			loopback = src.loopback;
			tracing = src.tracing;
			videoWidth = src.videoWidth;
			videoHeight = src.videoHeight;
			videoFps = src.videoFps;
			videoMaxBitrate = src.videoMaxBitrate;
			videoCodec = src.videoCodec;
			videoCodecHwAcceleration = src.videoCodecHwAcceleration;
			videoFlexfecEnabled = src.videoFlexfecEnabled;
			audioSource = src.audioSource;
			audioFormat = src.audioFormat;
			audioStartBitrate = src.audioStartBitrate;
			audioCodec = src.audioCodec;
			noAudioProcessing = src.noAudioProcessing;
			aecDump = src.aecDump;
			saveInputAudioToFile = src.saveInputAudioToFile;
			useOpenSLES = src.useOpenSLES;
			disableBuiltInAEC = src.disableBuiltInAEC;
			disableBuiltInAGC = src.disableBuiltInAGC;
			disableBuiltInNS = src.disableBuiltInNS;
			disableWebRtcAGCAndHPF = src.disableWebRtcAGCAndHPF;
			enableRtcEventLog = src.enableRtcEventLog;
			dataChannelParameters = src.dataChannelParameters;
		}

		/**
		 * 映像伝送するかどうかを設定
		 * @param videoCallEnabled
		 * @return
		 */
		public Builder setVideoCallEnabled(final boolean videoCallEnabled) {
			this.videoCallEnabled = videoCallEnabled;
			return this;
		}

		/**
		 * ループバック接続するかどうかを設定
		 * @param loopback
		 * @return
		 */
		public Builder setLoopback(final boolean loopback) {
			this.loopback = loopback;
			return this;
		}

		/**
		 * トレースログ出力するかどうかを設定
		 * @param tracing
		 * @return
		 */
		public Builder setTracing(final boolean tracing) {
			this.tracing = tracing;
			return this;
		}

		/**
		 * 映像を伝送する場合の映像サイズ(幅)を設定
		 * @param videoWidth
		 * @return
		 */
		public Builder setVideoWidth(final int videoWidth) {
			this.videoWidth = videoWidth;
			return this;
		}

		/**
		 * 映像を伝送する場合の映像サイズ(高さ)を設定
		 * @param videoHeight
		 * @return
		 */
		public Builder setVideoHeight(final int videoHeight) {
			this.videoHeight = videoHeight;
			return this;
		}

		/**
		 * 映像を伝送する場合のフレームレートを設定
		 * @param videoFps
		 * @return
		 */
		public Builder setVideoFps(final int videoFps) {
			this.videoFps = videoFps;
			return this;
		}

		/**
		 * 映像を伝送する場合の最大伝送帯域を設定
		 * @param videoMaxBitrate
		 * @return
		 */
		public Builder setVideoMaxBitrate(final int videoMaxBitrate) {
			this.videoMaxBitrate = videoMaxBitrate;
			return this;
		}

		/**
		 * 映像を伝送する場合のコーデックを設定
		 * @param videoCodec
		 * @return
		 */
		public Builder setVideoCodec(final String videoCodec) {
			this.videoCodec = videoCodec;
			return this;
		}

		/**
		 * 映像を伝送する場合にハードウエアコーデックを使用するかどうかを設定
		 * @param videoCodecHwAcceleration
		 * @return
		 */
		public Builder setVideoCodecHwAcceleration(final boolean videoCodecHwAcceleration) {
			this.videoCodecHwAcceleration = videoCodecHwAcceleration;
			return this;
		}

		/**
		 * 映像を伝送する場合にFlexFecを有効にするかどうかを設定
		 * @param videoFlexfecEnabled
		 * @return
		 */
		public Builder setVideoFlexfecEnabled(final boolean videoFlexfecEnabled) {
			this.videoFlexfecEnabled = videoFlexfecEnabled;
			return this;
		}

		/**
		 * 音声を伝送する場合に使う音声ソースを設定
		 * デフォルトはMediaRecorder.AudioSource.VOICE_COMMUNICATION
		 * @param audioSource
		 * @return
		 */
		public Builder setAudioSource(@AudioRecordCompat.AudioSource final int audioSource) {
			this.audioSource = audioSource;
			return this;
		}

		/**
		 * 音声を伝送する場合に使う音声ソースの音声フォーマットを設定
		 * デフォルトはAudioFormat.ENCODING_PCM_16BIT
		 * @param audioFormat
		 * @return
		 */
		public Builder setAudioFormat(@AudioRecordCompat.AudioFormats final int audioFormat) {
			this.audioFormat = audioFormat;
			return this;
		}

		/**
		 * 音声伝送開始時の音声帯域を設定
		 * @param audioStartBitrate
		 * @return
		 */
		public Builder setAudioStartBitrate(final int audioStartBitrate) {
			this.audioStartBitrate = audioStartBitrate;
			return this;
		}

		/**
		 * 音声伝送に使用するコーデックを設定
		 * デフォルトはOPUS
		 * @param audioCodec
		 * @return
		 */
		public Builder setAudioCodec(final String audioCodec) {
			this.audioCodec = audioCodec;
			return this;
		}

		/**
		 * 音声処理を行わないかどうかを設定
		 * @param noAudioProcessing
		 * @return
		 */
		public Builder setNoAudioProcessing(final boolean noAudioProcessing) {
			this.noAudioProcessing = noAudioProcessing;
			return this;
		}

		/**
		 * Aacダンプをするかどうかを設定
		 * @param aecDump
		 * @return
		 */
		public Builder setAecDump(final boolean aecDump) {
			this.aecDump = aecDump;
			return this;
		}

		/**
		 * 音声データをファイルへ保存するかどうかを設定
		 * @param saveInputAudioToFile
		 * @return
		 */
		public Builder setSaveInputAudioToFile(final boolean saveInputAudioToFile) {
			this.saveInputAudioToFile = saveInputAudioToFile;
			return this;
		}

		/**
		 * Open SL|ESを使って音声データを処理するかどうかを設定
		 * デフォルトはfalse
		 * @param useOpenSLES
		 * @return
		 */
		public Builder setUseOpenSLES(final boolean useOpenSLES) {
			this.useOpenSLES = useOpenSLES;
			return this;
		}

		/**
		 * 内蔵のエコーキャンセラを無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInAEC
		 * @return
		 */
		public Builder setDisableBuiltInAEC(final boolean disableBuiltInAEC) {
			this.disableBuiltInAEC = disableBuiltInAEC;
			return this;
		}

		/**
		 * 内蔵の音声自動ゲイン調整を無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInAGC
		 * @return
		 */
		public Builder setDisableBuiltInAGC(final boolean disableBuiltInAGC) {
			this.disableBuiltInAGC = disableBuiltInAGC;
			return this;
		}

		/**
		 * 内蔵のノイズサプレッサーを無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInNS
		 * @return
		 */
		public Builder setDisableBuiltInNS(final boolean disableBuiltInNS) {
			this.disableBuiltInNS = disableBuiltInNS;
			return this;
		}

		/**
		 * webrtcの自動ゲイン調整機能とハイパスフィルターを向こうにするかどうかを設定
		 * デフォルトはfalse
		 * @param disableWebRtcAGCAndHPF
		 * @return
		 */
		public Builder setDisableWebRtcAGCAndHPF(final boolean disableWebRtcAGCAndHPF) {
			this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF;
			return this;
		}

		/**
		 * RTCイベントのロギングを有効にするかどうかを設定
		 * デフォルトはfalse
		 * @param enableRtcEventLog
		 * @return
		 */
		public Builder setEnableRtcEventLog(final boolean enableRtcEventLog) {
			this.enableRtcEventLog = enableRtcEventLog;
			return this;
		}

		/**
		 * データチャネルのパラメータを設定
		 * @param dataChannelParameters
		 * @return
		 */
		public Builder setDataChannelParameters(@Nullable final DataChannelParameters dataChannelParameters) {
			this.dataChannelParameters = dataChannelParameters;
			return this;
		}

		@NonNull
		public PeerConnectionParameters build() throws IllegalArgumentException {
			// FIXME パラメータチェックを追加する
			return new PeerConnectionParameters(videoCallEnabled, loopback, tracing,
					videoWidth, videoHeight, videoFps, videoMaxBitrate, videoCodec,
					videoCodecHwAcceleration, videoFlexfecEnabled,
					audioSource, audioFormat, audioStartBitrate, audioCodec,
					noAudioProcessing, aecDump, saveInputAudioToFile,
					useOpenSLES, disableBuiltInAEC, disableBuiltInAGC,
					disableBuiltInNS, disableWebRtcAGCAndHPF, enableRtcEventLog,
					dataChannelParameters);
		}

		@NonNull
		@Override
		public String toString() {
			return "Builder{" +
				"videoCallEnabled=" + videoCallEnabled +
				", loopback=" + loopback +
				", tracing=" + tracing +
				", videoWidth=" + videoWidth +
				", videoHeight=" + videoHeight +
				", videoFps=" + videoFps +
				", videoMaxBitrate=" + videoMaxBitrate +
				", videoCodec='" + videoCodec + '\'' +
				", videoCodecHwAcceleration=" + videoCodecHwAcceleration +
				", videoFlexfecEnabled=" + videoFlexfecEnabled +
				", audioSource=" + audioSource +
				", audioFormat=" + audioFormat +
				", audioStartBitrate=" + audioStartBitrate +
				", audioCodec='" + audioCodec + '\'' +
				", noAudioProcessing=" + noAudioProcessing +
				", aecDump=" + aecDump +
				", saveInputAudioToFile=" + saveInputAudioToFile +
				", useOpenSLES=" + useOpenSLES +
				", disableBuiltInAEC=" + disableBuiltInAEC +
				", disableBuiltInAGC=" + disableBuiltInAGC +
				", disableBuiltInNS=" + disableBuiltInNS +
				", disableWebRtcAGCAndHPF=" + disableWebRtcAGCAndHPF +
				", enableRtcEventLog=" + enableRtcEventLog +
				", dataChannelParameters=" + dataChannelParameters +
				'}';
		}
	}

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

	/**
	 * FIXME Builderを使うようにするためprivateかprotectedに変更する
	 */
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

	/**
	 * SDP用に映像コーデック名を取得
	 * @return
	 */
	public String getSdpVideoCodecName() {

		switch (this.videoCodec) {
		case VIDEO_CODEC_VP9:
			return VIDEO_CODEC_VP9;
		case VIDEO_CODEC_H264:
		case VIDEO_CODEC_H264_HIGH:
		case VIDEO_CODEC_H264_BASELINE:
			return VIDEO_CODEC_H264;
		case VIDEO_CODEC_VP8:
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
