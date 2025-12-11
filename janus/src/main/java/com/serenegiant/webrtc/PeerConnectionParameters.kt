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

import android.media.AudioFormat
import android.media.MediaRecorder
import android.util.Log
import com.serenegiant.media.AudioRecordCompat
import com.serenegiant.media.AudioRecordCompat.AudioFormats

/**
 * Peer connection parameters.
 */
class PeerConnectionParameters(
	@JvmField val videoCallEnabled: Boolean,
	@JvmField val loopback: Boolean,
	@JvmField val tracing: Boolean,
	@JvmField val videoWidth: Int,
	@JvmField val videoHeight: Int,
	@JvmField val videoFps: Int,
	@JvmField val videoMaxBitrate: Int,
	@JvmField val videoCodec: String,
	@JvmField val videoCodecHwAcceleration: Boolean,
	@JvmField val videoFlexfecEnabled: Boolean,
	@JvmField val audioSource: Int,
	@JvmField val audioFormat: Int,
	@JvmField val audioStartBitrate: Int,
	@JvmField val audioCodec: String,
	@JvmField val noAudioProcessing: Boolean,
	@JvmField val aecDump: Boolean,
	@JvmField val saveInputAudioToFile: Boolean,
	@JvmField val useOpenSLES: Boolean,
	@JvmField val disableBuiltInAEC: Boolean,
	@JvmField val disableBuiltInAGC: Boolean,
	@JvmField val disableBuiltInNS: Boolean,
	@JvmField val disableWebRtcAGCAndHPF: Boolean,
	@JvmField val enableRtcEventLog: Boolean,
	@JvmField val dataChannelParameters: DataChannelParameters?
) {
	/**
	 * PeerConnectionParameters生成のためのビルダークラス
	 */
	class Builder {
		private var videoCallEnabled = false
		private var loopback = false
		private var tracing = false
		private var videoWidth = 0
		private var videoHeight = 0
		private var videoFps = 0
		private var videoMaxBitrate = 0 // 0: 制限無し
		private var videoCodec = AppRTCConst.VIDEO_CODEC_VP8
		private var videoCodecHwAcceleration = true
		private var videoFlexfecEnabled = false
		private var audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
		private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
		private var audioStartBitrate = 0 // 0: 制限無し
		private var audioCodec = AppRTCConst.AUDIO_CODEC_OPUS
		private var noAudioProcessing = false
		private var aecDump = false
		private var saveInputAudioToFile = false
		private var useOpenSLES = false
		private var disableBuiltInAEC = false
		private var disableBuiltInAGC = false
		private var disableBuiltInNS = false
		private var disableWebRtcAGCAndHPF = false
		private var enableRtcEventLog = false
		private var dataChannelParameters: DataChannelParameters? = null

		/**
		 * デフォルトコンストラクタ
		 */
		constructor()

		/**
		 * 既存のBuilderの内容を引き継いで新しいBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		constructor(src: Builder) {
			videoCallEnabled = src.videoCallEnabled
			loopback = src.loopback
			tracing = src.tracing
			videoWidth = src.videoWidth
			videoHeight = src.videoHeight
			videoFps = src.videoFps
			videoMaxBitrate = src.videoMaxBitrate
			videoCodec = src.videoCodec
			videoCodecHwAcceleration = src.videoCodecHwAcceleration
			videoFlexfecEnabled = src.videoFlexfecEnabled
			audioSource = src.audioSource
			audioFormat = src.audioFormat
			audioStartBitrate = src.audioStartBitrate
			audioCodec = src.audioCodec
			noAudioProcessing = src.noAudioProcessing
			aecDump = src.aecDump
			saveInputAudioToFile = src.saveInputAudioToFile
			useOpenSLES = src.useOpenSLES
			disableBuiltInAEC = src.disableBuiltInAEC
			disableBuiltInAGC = src.disableBuiltInAGC
			disableBuiltInNS = src.disableBuiltInNS
			disableWebRtcAGCAndHPF = src.disableWebRtcAGCAndHPF
			enableRtcEventLog = src.enableRtcEventLog
			dataChannelParameters = src.dataChannelParameters
		}

		/**
		 * 映像伝送するかどうかを設定
		 * @param videoCallEnabled
		 * @return
		 */
		fun setVideoCallEnabled(videoCallEnabled: Boolean): Builder {
			this.videoCallEnabled = videoCallEnabled
			return this
		}

		/**
		 * ループバック接続するかどうかを設定
		 * @param loopback
		 * @return
		 */
		fun setLoopback(loopback: Boolean): Builder {
			this.loopback = loopback
			return this
		}

		/**
		 * トレースログ出力するかどうかを設定
		 * @param tracing
		 * @return
		 */
		fun setTracing(tracing: Boolean): Builder {
			this.tracing = tracing
			return this
		}

		/**
		 * 映像を伝送する場合の映像サイズ(幅)を設定
		 * @param videoWidth
		 * @return
		 */
		fun setVideoWidth(videoWidth: Int): Builder {
			this.videoWidth = videoWidth
			return this
		}

		/**
		 * 映像を伝送する場合の映像サイズ(高さ)を設定
		 * @param videoHeight
		 * @return
		 */
		fun setVideoHeight(videoHeight: Int): Builder {
			this.videoHeight = videoHeight
			return this
		}

		/**
		 * 映像を伝送する場合のフレームレートを設定
		 * @param videoFps
		 * @return
		 */
		fun setVideoFps(videoFps: Int): Builder {
			this.videoFps = videoFps
			return this
		}

		/**
		 * 映像を伝送する場合の最大伝送帯域を設定
		 * @param videoMaxBitrate
		 * @return
		 */
		fun setVideoMaxBitrate(videoMaxBitrate: Int): Builder {
			this.videoMaxBitrate = videoMaxBitrate
			return this
		}

		/**
		 * 映像を伝送する場合のコーデックを設定
		 * @param videoCodec
		 * @return
		 */
		fun setVideoCodec(videoCodec: String?): Builder {
			if (videoCodec != null) {
				this.videoCodec = videoCodec
			} else {
				this.videoCodec = AppRTCConst.VIDEO_CODEC_VP8
			}
			return this
		}

		/**
		 * 映像を伝送する場合にハードウエアコーデックを使用するかどうかを設定
		 * @param videoCodecHwAcceleration
		 * @return
		 */
		fun setVideoCodecHwAcceleration(videoCodecHwAcceleration: Boolean): Builder {
			this.videoCodecHwAcceleration = videoCodecHwAcceleration
			return this
		}

		/**
		 * 映像を伝送する場合にFlexFecを有効にするかどうかを設定
		 * @param videoFlexfecEnabled
		 * @return
		 */
		fun setVideoFlexfecEnabled(videoFlexfecEnabled: Boolean): Builder {
			this.videoFlexfecEnabled = videoFlexfecEnabled
			return this
		}

		/**
		 * 音声を伝送する場合に使う音声ソースを設定
		 * デフォルトはMediaRecorder.AudioSource.VOICE_COMMUNICATION
		 * @param audioSource
		 * @return
		 */
		fun setAudioSource(@AudioRecordCompat.AudioSource audioSource: Int): Builder {
			this.audioSource = audioSource
			return this
		}

		/**
		 * 音声を伝送する場合に使う音声ソースの音声フォーマットを設定
		 * デフォルトはAudioFormat.ENCODING_PCM_16BIT
		 * @param audioFormat
		 * @return
		 */
		fun setAudioFormat(@AudioFormats audioFormat: Int): Builder {
			this.audioFormat = audioFormat
			return this
		}

		/**
		 * 音声伝送開始時の音声帯域を設定
		 * @param audioStartBitrate
		 * @return
		 */
		fun setAudioStartBitrate(audioStartBitrate: Int): Builder {
			this.audioStartBitrate = audioStartBitrate
			return this
		}

		/**
		 * 音声伝送に使用するコーデックを設定
		 * デフォルトはOPUS
		 * @param audioCodec
		 * @return
		 */
		fun setAudioCodec(audioCodec: String?): Builder {
			if (audioCodec != null) {
				this.audioCodec = audioCodec
			} else {
				this.audioCodec = AppRTCConst.AUDIO_CODEC_OPUS
			}
			return this
		}

		/**
		 * 音声処理を行わないかどうかを設定
		 * @param noAudioProcessing
		 * @return
		 */
		fun setNoAudioProcessing(noAudioProcessing: Boolean): Builder {
			this.noAudioProcessing = noAudioProcessing
			return this
		}

		/**
		 * Aacダンプをするかどうかを設定
		 * @param aecDump
		 * @return
		 */
		fun setAecDump(aecDump: Boolean): Builder {
			this.aecDump = aecDump
			return this
		}

		/**
		 * 音声データをファイルへ保存するかどうかを設定
		 * @param saveInputAudioToFile
		 * @return
		 */
		fun setSaveInputAudioToFile(saveInputAudioToFile: Boolean): Builder {
			this.saveInputAudioToFile = saveInputAudioToFile
			return this
		}

		/**
		 * Open SL|ESを使って音声データを処理するかどうかを設定
		 * デフォルトはfalse
		 * @param useOpenSLES
		 * @return
		 */
		fun setUseOpenSLES(useOpenSLES: Boolean): Builder {
			this.useOpenSLES = useOpenSLES
			return this
		}

		/**
		 * 内蔵のエコーキャンセラを無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInAEC
		 * @return
		 */
		fun setDisableBuiltInAEC(disableBuiltInAEC: Boolean): Builder {
			this.disableBuiltInAEC = disableBuiltInAEC
			return this
		}

		/**
		 * 内蔵の音声自動ゲイン調整を無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInAGC
		 * @return
		 */
		fun setDisableBuiltInAGC(disableBuiltInAGC: Boolean): Builder {
			this.disableBuiltInAGC = disableBuiltInAGC
			return this
		}

		/**
		 * 内蔵のノイズサプレッサーを無効にするかどうかを設定
		 * デフォルトはfalse
		 * @param disableBuiltInNS
		 * @return
		 */
		fun setDisableBuiltInNS(disableBuiltInNS: Boolean): Builder {
			this.disableBuiltInNS = disableBuiltInNS
			return this
		}

		/**
		 * webrtcの自動ゲイン調整機能とハイパスフィルターを向こうにするかどうかを設定
		 * デフォルトはfalse
		 * @param disableWebRtcAGCAndHPF
		 * @return
		 */
		fun setDisableWebRtcAGCAndHPF(disableWebRtcAGCAndHPF: Boolean): Builder {
			this.disableWebRtcAGCAndHPF = disableWebRtcAGCAndHPF
			return this
		}

		/**
		 * RTCイベントのロギングを有効にするかどうかを設定
		 * デフォルトはfalse
		 * @param enableRtcEventLog
		 * @return
		 */
		fun setEnableRtcEventLog(enableRtcEventLog: Boolean): Builder {
			this.enableRtcEventLog = enableRtcEventLog
			return this
		}

		/**
		 * データチャネルのパラメータを設定
		 * @param dataChannelParameters
		 * @return
		 */
		fun setDataChannelParameters(dataChannelParameters: DataChannelParameters?): Builder {
			this.dataChannelParameters = dataChannelParameters
			return this
		}

		@Throws(IllegalArgumentException::class)
		fun build(): PeerConnectionParameters {
			// FIXME パラメータチェックを追加する
			return PeerConnectionParameters(
				videoCallEnabled, loopback, tracing,
				videoWidth, videoHeight, videoFps, videoMaxBitrate, videoCodec,
				videoCodecHwAcceleration, videoFlexfecEnabled,
				audioSource, audioFormat, audioStartBitrate, audioCodec,
				noAudioProcessing, aecDump, saveInputAudioToFile,
				useOpenSLES, disableBuiltInAEC, disableBuiltInAGC,
				disableBuiltInNS, disableWebRtcAGCAndHPF, enableRtcEventLog,
				dataChannelParameters
			)
		}

		override fun toString(): String {
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
				'}'
		}
	}

	/**
	 * SDP用に映像コーデック名を取得
	 * @return
	 */
	val sdpVideoCodecName: String
		get() {
			return when (this.videoCodec) {
				AppRTCConst.VIDEO_CODEC_VP9 -> AppRTCConst.VIDEO_CODEC_VP9
				AppRTCConst.VIDEO_CODEC_H264, AppRTCConst.VIDEO_CODEC_H264_HIGH, AppRTCConst.VIDEO_CODEC_H264_BASELINE -> AppRTCConst.VIDEO_CODEC_H264
				AppRTCConst.VIDEO_CODEC_VP8 -> AppRTCConst.VIDEO_CODEC_VP8
				else -> AppRTCConst.VIDEO_CODEC_VP8
			}
		}

	val fieldTrials: String
		get() {
			var fieldTrials = ""
			if (this.videoFlexfecEnabled) {
				fieldTrials += AppRTCConst.VIDEO_FLEXFEC_FIELDTRIAL
				if (DEBUG) Log.d(TAG, "Enable FlexFEC field trial.")
			}
			fieldTrials += AppRTCConst.VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
			if (this.disableWebRtcAGCAndHPF) {
				fieldTrials += AppRTCConst.DISABLE_WEBRTC_AGC_FIELDTRIAL
				if (DEBUG) Log.d(TAG, "Disable WebRTC AGC field trial.")
			}
			if (AppRTCConst.VIDEO_CODEC_H264_HIGH == this.videoCodec) {
				// TODO(magjed): Strip High from SDP when selecting Baseline instead of using field trial.
				fieldTrials += AppRTCConst.VIDEO_H264_HIGH_PROFILE_FIELDTRIAL
			}
			return fieldTrials
		}


	companion object {
		private const val DEBUG = false // set false on production
		private val TAG: String = PeerConnectionParameters::class.java.simpleName
	}
}
