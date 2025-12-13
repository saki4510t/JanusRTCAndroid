package com.serenegiant.janus
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2025 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import android.content.Context
import android.media.AudioDeviceInfo
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.serenegiant.janus.JanusClient.ListCallback
import com.serenegiant.janus.TransactionManager.clearTransactions
import com.serenegiant.janus.TransactionManager.handleTransaction
import com.serenegiant.janus.VideoRoomPlugin.VideoRoomCallback
import com.serenegiant.janus.request.CreateSession
import com.serenegiant.janus.request.DestroySession
import com.serenegiant.janus.request.videoroom.ConfigPublisher
import com.serenegiant.janus.request.videoroom.ConfigSubscriber
import com.serenegiant.janus.response.ServerInfo
import com.serenegiant.janus.response.Session
import com.serenegiant.janus.response.videoroom.PublisherInfo
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.serenegiant.janus.response.videoroom.RoomInfo
import com.serenegiant.system.BuildCheck
import com.serenegiant.webrtc.AppRTCConst
import com.serenegiant.webrtc.MediaStreamUtils
import com.serenegiant.webrtc.PeerConnectionParameters
import com.serenegiant.webrtc.RoomConnectionParameters
import com.serenegiant.webrtc.RtcEventLog
import com.serenegiant.webrtc.audio.RecordedAudioToFileController
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnection.SdpSemantics
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStatsReport
import org.webrtc.RtpSender
import org.webrtc.SessionDescription
import org.webrtc.SoftwareVideoDecoderFactory
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoDecoderFactory
import org.webrtc.VideoEncoderFactory
import org.webrtc.VideoSink
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordStartErrorCode
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackStartErrorCode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Janus-gatewayへアクセスするためのヘルパークラス
 * とりあえず自分を含めて3人での通話に対応
 * FIXME 今はpublisherとsubscriberで別々のPeerConnectionを生成しているのを1つにする
 * => 調べた限りではpublisherとsubscriberは別々のPeerConnectionにせざるをえない感じ
 * ただし、1つのsubscriberで複数の相手からのストリーム(マルチストリーム)が
 * できる感じ(今は1つの相手につき1つのsubscriberになっているけど)
 * FIXME 動的にレイアウトを変更するかListView/RecyclerViewに入れるなどしてもう少し多い相手との通話できるようにする
 */
class JanusVideoRoomClient(
	appContext: Context,
	private val rootEglBase: EglBase,
	private val peerConnectionParameters: PeerConnectionParameters,
	private val roomConnectionParameters: RoomConnectionParameters,
	private val mCallback: JanusCallback
) : VideoRoomClient {
	private enum class ConnectionState {
		UNINITIALIZED,
		READY,  // janus-gateway server is ready to access
		CONNECTED,
		CLOSED,
		ERROR
	}

	private val mWeakContext = WeakReference(appContext)
	private val dataChannelEnabled: Boolean
	// Executorのワーカースレッド上で実行するためのCoroutineScope
	private val mScope = CoroutineScope(SupervisorJob() + Utils.executor.asCoroutineDispatcher() +  CoroutineName("scope_$TAG"))

	//--------------------------------------------------------------------------------
	private val statsTimer = Timer()
	private var factory: PeerConnectionFactory? = null
	private var videoCapturerStopped = false
	private var isError = false
	private var surfaceTextureHelper: SurfaceTextureHelper? = null

	/**
	 * Implements the WebRtcAudioRecordSamplesReadyCallback interface and writes
	 * recorded audio samples to an output file.
	 */
	private var saveRecordedAudioToFile: RecordedAudioToFileController? = null

	// ローカル映像・音声
	private var videoWidth = 0
	private var videoHeight = 0
	private var videoFps = 0

	/** enableAudio is set to true if audio should be sent.  */
	private var enableAudio = true

	/**
	 * enableVideo is set to true if video should be rendered and sent.
	 */
	private var renderVideo = true
	private var localRender: VideoSink? = null
	private var localVideoTrack: VideoTrack? = null
	private var localVideoSender: RtpSender? = null
	private var videoCapturer: VideoCapturer? = null
	private var videoSource: VideoSource? = null
	private var audioSource: AudioSource? = null
	private var localAudioTrack: AudioTrack? = null
	private var mLocalStream: MediaStream? = null
	private var mAudioDeviceModule: JavaAudioDeviceModule? = null

	private val mVideoSinkLock = ReentrantLock()
	/**
	 * リモート映像・音声のpluginのfeedIdとVideoSinkHolderのマップ
	 */
	private val remoteVideoSinkMap = mutableMapOf<Long, VideoSinkHolder>()

	//--------------------------------------------------------------------------------
	private var mJanus: VideoRoomAPI? = null
	private var mLongPoll: LongPoll? = null
	private val mCallLock = ReentrantLock()
	private val mCurrentCalls = mutableListOf<Call<*>>()
	private var mLastJob: Job? = null
	private val mPluginLock = ReentrantLock()
	private val mAttachedPlugins = mutableMapOf<Long, VideoRoomPlugin>()
	private var mConnectionState: ConnectionState
	private var mServerInfo: ServerInfo? = null
	private var mSession: Session? = null

	/**
	 * 音声入力デバイスのヒント
	 */
	private var mPreferredInputDevice: AudioDeviceInfo? = null

	/**
	 * コンストラクタ
	 * apiName = "janus", roomId = 1234
	 * @param appContext
	 * @param baseUrl
	 * @param eglBase
	 * @param peerConnectionParameters
	 * @param callback
	 */
	constructor(
		appContext: Context,
		baseUrl: String,
		eglBase: EglBase,
		peerConnectionParameters: PeerConnectionParameters,
		roomConnectionParameters: RoomConnectionParameters,
		callback: JanusCallback
	) : this(
		appContext, eglBase,
		peerConnectionParameters, roomConnectionParameters, callback
	)

	/**
	 * disconnect and release related resources
	 */
	fun release() {
		disconnectFromRoom()
		mScope.cancel()
	}

	//================================================================================
	// implementations of com.serenegiant.janus.JanusClient interface
	/**
	 * This function should only be called once.
	 */
	override fun createPeerConnectionFactory(
		options: PeerConnectionFactory.Options?
	) {
		if (DEBUG) Log.v(TAG, "createPeerConnectionFactory:")
		check(factory == null) { "PeerConnectionFactory has already been constructed" }
		mScope.launch { createPeerConnectionFactoryInternal(options) }
	}

	/**
	 * Publisher用のPeerConnectionを生成する
	 * @param localRender
	 * @param videoCapturer
	 */
	override fun createPeerConnection(
		localRender: VideoSink,
		videoCapturer: VideoCapturer?
	) {
		if (DEBUG) Log.v(TAG, "createPeerConnection:")
		this.localRender = localRender
		this.videoCapturer = videoCapturer
		mScope.launch {
			try {
				createMediaConstraintsInternal()
				createPublisherInternal()
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
				reportError(e)
			}
		}
	}

	override fun startVideoSource() {
		if (DEBUG) Log.v(TAG, "startVideoSource:")
		mScope.launch {
			if (videoCapturer != null && videoCapturerStopped) {
				if (DEBUG) Log.d(TAG, "Restart video source.")
				videoCapturer!!.startCapture(videoWidth, videoHeight, videoFps)
				videoCapturerStopped = false
			}
		}
	}

	override fun stopVideoSource() {
		if (DEBUG) Log.v(TAG, "stopVideoSource:")
		mScope.launch {
			if (videoCapturer != null && !videoCapturerStopped) {
				if (DEBUG) Log.d(TAG, "Stop video source.")
				try {
					videoCapturer!!.stopCapture()
				} catch (e: InterruptedException) {
					// ignore
				}
				videoCapturerStopped = true
			}
		}
	}

	private var mTimerTask: TimerTask? = null
	override fun enableStatsEvents(enable: Boolean, periodMs: Int) {
		if (DEBUG) Log.v(TAG, "enableStatsEvents:")
		if (enable) {
			cancelTimerTask()
			mTimerTask = object : TimerTask() {
				override fun run() {
					mScope.launch { this@JanusVideoRoomClient.stats() }
				}
			}
			try {
				statsTimer.schedule(mTimerTask, 0, periodMs.toLong())
			} catch (e: Exception) {
				Log.e(TAG, "Can not schedule statistics timer", e)
			}
		} else {
			cancelTimerTask()
		}
	}

	private fun cancelTimerTask() {
		if (mTimerTask != null) {
			mTimerTask!!.cancel()
			mTimerTask = null
		}
	}

	override fun switchCamera() {
		if (DEBUG) Log.v(TAG, "switchCamera:")
		mScope.launch { this@JanusVideoRoomClient.switchCameraInternal() }
	}

	override fun changeCaptureFormat(width: Int, height: Int, framerate: Int) {
		if (DEBUG) Log.v(TAG, "changeCaptureFormat:")
		mScope.launch {
			changeCaptureFormatInternal(
				width,
				height,
				framerate
			)
		}
	}

	override fun setAudioEnabled(enable: Boolean) {
		if (DEBUG) Log.v(TAG, "setAudioEnabled:")
		mScope.launch {
			enableAudio = enable
			if (localAudioTrack != null) {
				localAudioTrack!!.setEnabled(enableAudio)
			}
		}
	}

	override fun setVideoEnabled(enable: Boolean) {
		if (DEBUG) Log.v(TAG, "setVideoEnabled:")
		mScope.launch {
			renderVideo = enable
			if (localVideoTrack != null) {
				localVideoTrack!!.setEnabled(renderVideo)
			}
			for (holder in remoteVideoSinkMap.values) {
				holder.setEnabled(renderVideo)
			}
		}
	}

	/**
	 * request list of available room
	 * @param callback
	 */
	override fun requestRoomList(callback: ListCallback<List<RoomInfo?>?>) {
		if (DEBUG) Log.v(TAG, "list:")
		mScope.launch {
			listRoomInternal(
				roomConnectionParameters.roomUrl,
				roomConnectionParameters.apiName, callback
			)
		}
	}

	override fun connectToRoom(connectionParameters: RoomConnectionParameters) {
		if (DEBUG) Log.v(TAG, "connectToRoom:")
		mScope.launch {
			connectToRoomInternal()
		}
	}

	override fun disconnectFromRoom() {
		cancelTimerTask()
		statsTimer.cancel()
		if (mConnectionState != ConnectionState.CLOSED) {
			if (DEBUG) Log.v(TAG, "disconnectFromRoom:")
			cancelCall()
			mScope.launch {
				disconnectFromRoomInternal()
			}
		}
	}

	/**
	 * PublisherのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分のパブリッシャーのプラグインIDのはず
	 * @return
	 */
	override val publishers: Collection<Long>
		get() {
			val result: MutableList<Long> = ArrayList()
			mPluginLock.withLock {
				for (plugin in mAttachedPlugins.values) {
					if (plugin is VideoRoomPlugin.Publisher) {
						result.add(plugin.pluginId())
					}
				}
			}
			return result
		}

	/**
	 * SubscriberのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分がサブスクライブしているリモートに対応するプラグインIDのはず
	 * @return
	 */
	override val subscribers: Collection<Long>
		get() {
			val result: MutableList<Long> = ArrayList()
			mPluginLock.withLock {
				for (plugin in mAttachedPlugins.values) {
					if (plugin is VideoRoomPlugin.Subscriber) {
						result.add(plugin.pluginId())
					}
				}
			}
			return result
		}

	/**
	 * 全てのPublisherを設定する
	 * @param config
	 * @return
	 */
	override suspend fun configure(config: ConfigPublisher): Boolean {
		if (DEBUG) Log.v(TAG, "configure:$config")
		var result = false
		if (mConnectionState == ConnectionState.CONNECTED) {
			val plugins = mPluginLock.withLock {
				mAttachedPlugins.values.toList()
			}
			for (plugin in plugins) {
				if (plugin is VideoRoomPlugin.Publisher) {
					result = result or plugin.configure(config)
				}
			}
		}
		return result
	}

	/**
	 * 指定したプラグインIDが一致する最初のPublisherを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	override suspend fun configure(pluginId: Long, config: ConfigPublisher): Boolean {
		if (DEBUG) Log.v(TAG, "configure:id=$pluginId,$config"
		)
		if (mConnectionState == ConnectionState.CONNECTED) {
			val plugins = mPluginLock.withLock {
				mAttachedPlugins.values.toList()
			}
			for (plugin in plugins) {
				if ((plugin is VideoRoomPlugin.Publisher)
					&& (plugin.pluginId() == pluginId)
				) {
					return plugin.configure(config)
				}
			}
		}
		return false
	}

	/**
	 * 全てのSubscriberを設定する
	 * @param config
	 * @return
	 */
	override suspend fun configure(config: ConfigSubscriber): Boolean {
		if (DEBUG) Log.v(TAG, "configure:$config")
		var result = false
		if (mConnectionState == ConnectionState.CONNECTED) {
			val plugins = mPluginLock.withLock {
				mAttachedPlugins.values.toList()
			}
			for (plugin in plugins) {
				if (plugin is VideoRoomPlugin.Subscriber) {
					result = result or plugin.configure(config)
				}
			}
		}
		return result
	}

	/**
	 * 指定したプラグインIDが一致する最初のSubscriberを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	override suspend fun configure(pluginId: Long, config: ConfigSubscriber): Boolean {
		if (DEBUG) Log.v(TAG, "configure:id=$pluginId,$config")
		if (mConnectionState == ConnectionState.CONNECTED) {
			val plugins = mPluginLock.withLock {
				mAttachedPlugins.values.toList()
			}
			for (plugin in plugins) {
				if ((plugin is VideoRoomPlugin.Subscriber)
					&& (plugin.pluginId() == pluginId)
				) {
					return plugin.configure(config)
				}
			}
		}
		return false
	}

	/**
	 * 通話時の音声入力デバイスのヒントを設定
	 * @param preferredInputDevice
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	fun setPreferredInputDevice(preferredInputDevice: AudioDeviceInfo?) {
		mPreferredInputDevice = preferredInputDevice
		if (mAudioDeviceModule != null) {
			mAudioDeviceModule!!.setPreferredInputDevice(preferredInputDevice)
		}
	}

	/**
	 * 指定したパブリッシャーからの音声をミュートする
	 * @param info
	 * @param mute
	 */
	fun setMute(info: PublisherInfo, mute: Boolean) {
		val holder = getHolder(info)
		if (holder != null) {
			MediaStreamUtils.setMute(holder.mMediaStream, mute)
		}
	}

	/**
	 * 指定したパブリッシャーからの音量を設定する
	 * @param info
	 * @param volume
	 */
	fun setVolume(info: PublisherInfo, volume: Double) {
		val holder = getHolder(info)
		if (holder != null) {
			MediaStreamUtils.setVolume(holder.mMediaStream, volume)
		}
	}

	/**
	 * 指定したパブリッシャーに対応するVideoSinkHolderを取得する
	 * @param info
	 * @return
	 */
	fun getHolder(info: PublisherInfo): VideoSinkHolder? {
		val feedId = info.id!!
		return if (remoteVideoSinkMap.containsKey(feedId)) remoteVideoSinkMap[feedId] else null
	}

	//================================================================================
	private fun createPeerConnectionFactoryInternal(
		options: PeerConnectionFactory.Options?
	) {
		if (DEBUG) Log.v(TAG, "createPeerConnectionFactoryInternal:")
		isError = false

		if (peerConnectionParameters.tracing) {
			PeerConnectionFactory.startInternalTracingCapture(
				(Environment.getExternalStorageDirectory().absolutePath
					+ File.separator + "webrtc-trace.txt")
			)
		}


		// It is possible to save a copy in raw PCM format on a file by checking
		// the "Save input audio to file" checkbox in the Settings UI. A callback
		// interface is set when this flag is enabled. As a result, a copy of recorded
		// audio samples are provided to this client directly from the native audio
		// layer in Java.
		if (peerConnectionParameters.saveInputAudioToFile) {
			if (!peerConnectionParameters.useOpenSLES) {
				if (DEBUG) Log.d(TAG, "Enable recording of microphone input audio to file")
				saveRecordedAudioToFile = RecordedAudioToFileController(Utils.executor)
			} else {
				// TODO(henrika): ensure that the UI reflects that if OpenSL ES is selected,
				// then the "Save input audio to file" option shall be grayed out.
				Log.e(TAG, "Recording of input audio is not supported for OpenSL ES")
			}
		}

		val adm = createJavaAudioDevice()
		if (BuildCheck.isAPI23() && (adm is JavaAudioDeviceModule)) {
			mAudioDeviceModule = adm
			// XXX JavaAudioDeviceModule#setPreferredInputDeviceもそこから呼び出される
			//     WebRtcAudioRecord#setPreferredDeviceの引数もnullableで
			//     #setPreferredDevice内ではnullチェックをしているにも関わらず
			//     AudioDeviceInfo#getIdが呼び出されてヌルポでクラッシュするので
			//     自前でnullチェックを追加
			if (mPreferredInputDevice != null) {
				adm.setPreferredInputDevice(mPreferredInputDevice)
			}
		}


		// Create peer connection factory.
		if (options != null && DEBUG) {
			Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask)
		}
		val enableH264HighProfile =
			AppRTCConst.VIDEO_CODEC_H264_HIGH == peerConnectionParameters.videoCodec
		val encoderFactory: VideoEncoderFactory
		val decoderFactory: VideoDecoderFactory

		if (peerConnectionParameters.videoCodecHwAcceleration) {
			encoderFactory = DefaultVideoEncoderFactory(
				rootEglBase.eglBaseContext, true,  /* enableIntelVp8Encoder */enableH264HighProfile
			)
			decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
		} else {
			encoderFactory = SoftwareVideoEncoderFactory()
			decoderFactory = SoftwareVideoDecoderFactory()
		}

		factory = PeerConnectionFactory.builder()
			.setOptions(options)
			.setAudioDeviceModule(adm)
			.setVideoEncoderFactory(encoderFactory)
			.setVideoDecoderFactory(decoderFactory)
			.createPeerConnectionFactory()
		// Set INFO libjingle logging.
		// NOTE: this _must_ happen while |factory| is alive!
		Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR)

		if (DEBUG) Log.d(TAG, "Peer connection factory created.")
	}

	private fun createJavaAudioDevice(): AudioDeviceModule {
		if (DEBUG) Log.v(TAG, "createJavaAudioDevice:")
		// Enable/disable OpenSL ES playback.
		if (!peerConnectionParameters.useOpenSLES) {
			Log.w(TAG, "External OpenSLES ADM not implemented yet.")
			// TODO(magjed): Add support for external OpenSLES ADM.
		}


		// Set audio record error callbacks.
		val audioRecordErrorCallback
			: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
			override fun onWebRtcAudioRecordInitError(errorMessage: String) {
				Log.e(TAG, "onWebRtcAudioRecordInitError: $errorMessage")
				reportError(RuntimeException(errorMessage))
			}

			override fun onWebRtcAudioRecordStartError(
				errorCode: AudioRecordStartErrorCode,
				errorMessage: String
			) {
				Log.e(TAG, "onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
				reportError(RuntimeException(errorMessage))
			}

			override fun onWebRtcAudioRecordError(errorMessage: String) {
				Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
				reportError(RuntimeException(errorMessage))
			}
		}

		val audioTrackErrorCallback
			: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
			override fun onWebRtcAudioTrackInitError(errorMessage: String) {
				Log.e(TAG, "onWebRtcAudioTrackInitError: $errorMessage")
				reportError(RuntimeException(errorMessage))
			}

			override fun onWebRtcAudioTrackStartError(
				errorCode: AudioTrackStartErrorCode,
				errorMessage: String
			) {
				Log.e(TAG, "onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
				reportError(RuntimeException(errorMessage))
			}

			override fun onWebRtcAudioTrackError(errorMessage: String) {
				Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
				reportError(RuntimeException(errorMessage))
			}
		}

		return JavaAudioDeviceModule.builder(context)
			.setAudioSource(peerConnectionParameters.audioSource)
			.setAudioFormat(peerConnectionParameters.audioFormat)
			.setSamplesReadyCallback(saveRecordedAudioToFile)
			.setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
			.setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
			.setAudioRecordErrorCallback(audioRecordErrorCallback)
			.setAudioTrackErrorCallback(audioTrackErrorCallback)
			.createAudioDeviceModule()
	}

	private val isVideoCallEnabled: Boolean
		get() = peerConnectionParameters.videoCallEnabled
			&& (videoCapturer != null)

	private fun createMediaConstraintsInternal() {
		if (DEBUG) Log.v(TAG, "createMediaConstraintsInternal:")
		// Create video constraints if video call is enabled.
		if (isVideoCallEnabled) {
			videoWidth = peerConnectionParameters.videoWidth
			videoHeight = peerConnectionParameters.videoHeight
			videoFps = peerConnectionParameters.videoFps


			// If video resolution is not specified, default to HD.
			if (videoWidth == 0 || videoHeight == 0) {
				videoWidth = AppRTCConst.HD_VIDEO_WIDTH
				videoHeight = AppRTCConst.HD_VIDEO_HEIGHT
			}


			// If fps is not specified, default to 30.
			if (videoFps == 0) {
				videoFps = 30
			}
			Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps)
		}
	}

	/**
	 * PeerConnection生成用のヘルパーメソッド
	 * @param rtcConfig
	 * @param observer
	 * @return
	 */
	private fun createPeerConnection(
		rtcConfig: RTCConfiguration,
		observer: PeerConnection.Observer
	): PeerConnection? {
		return factory!!.createPeerConnection(rtcConfig, observer)
	}

	/**
	 * XXX パブリッシャー側は当面マルチストリーム対応しない予定
	 */
	private fun createPublisherInternal() {
		if (DEBUG) Log.v(TAG, "createPublisherInternal:")
		val context = context
		if ((context == null) || (factory == null) || isError) {
			Log.e(TAG, "PeerConnection factory is not created")
			return
		}
		if (DEBUG) Log.d(TAG, "Create peer connection.")


//		if (isVideoCallEnabled()) {
//			factory.setVideoHwAccelerationOptions(
//				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
//		}

		// Create audio constraints.
		val audioConstraints = MediaConstraints()
		// added for audio performance measurements
		if (peerConnectionParameters.noAudioProcessing) {
			if (DEBUG) Log.d(TAG, "Disabling audio processing")
			audioConstraints.mandatory.add(
				MediaConstraints.KeyValuePair(
					AppRTCConst.AUDIO_ECHO_CANCELLATION_CONSTRAINT,
					"false"
				)
			)
			audioConstraints.mandatory.add(
				MediaConstraints.KeyValuePair(
					AppRTCConst.AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT,
					"false"
				)
			)
			audioConstraints.mandatory.add(
				MediaConstraints.KeyValuePair(
					AppRTCConst.AUDIO_HIGH_PASS_FILTER_CONSTRAINT,
					"false"
				)
			)
			audioConstraints.mandatory.add(
				MediaConstraints.KeyValuePair(
					AppRTCConst.AUDIO_NOISE_SUPPRESSION_CONSTRAINT,
					"false"
				)
			)
		}
		// Create SDP constraints.
		val sdpMediaConstraints = MediaConstraints()
		sdpMediaConstraints.mandatory.add(
			MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false")
		)
		sdpMediaConstraints.mandatory.add(
			MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false")
		)
		sdpMediaConstraints.optional.add(
			MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true")
		)

		val rtcConfig =
			RTCConfiguration(mCallback.getIceServers(this))
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
		rtcConfig.continualGatheringPolicy =
			PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA
		rtcConfig.sdpSemantics = SDP_SEMANTICS

		val mediaStreamLabels = listOf("ARDAMS")

		val publisher = VideoRoomPlugin.Publisher(
			mJanus!!, mSession!!,
			mVideoRoomCallback,
			peerConnectionParameters,
			roomConnectionParameters,
			sdpMediaConstraints,
			isVideoCallEnabled
		)

		val peerConnection: PeerConnection?
		var dataChannel: DataChannel? = null
		if (SDP_SEMANTICS == SdpSemantics.UNIFIED_PLAN) {
			peerConnection = createPeerConnection(rtcConfig, publisher)
			val dataChannelParameters = peerConnectionParameters.dataChannelParameters
			if (dataChannelEnabled && (dataChannelParameters != null)) {
				val init = DataChannel.Init()
				init.ordered = dataChannelParameters.ordered
				init.negotiated = dataChannelParameters.negotiated
				init.maxRetransmits = dataChannelParameters.maxRetransmits
				init.maxRetransmitTimeMs = dataChannelParameters.maxRetransmitTimeMs
				init.id = dataChannelParameters.id
				init.protocol = dataChannelParameters.protocol
				dataChannel = peerConnection!!.createDataChannel("ApprtcDemo data", init)
			}

			if (isVideoCallEnabled) {
				peerConnection!!.addTrack(createVideoTrack(videoCapturer!!), mediaStreamLabels)
				// Publisherは送信のみなのでリモートビデオトラックは不要
			}
			peerConnection!!.addTrack(createAudioTrack(audioConstraints), mediaStreamLabels)
			if (isVideoCallEnabled) {
				findVideoSender(peerConnection)
			}
		} else {
			var videoTrack: VideoTrack? = null
			var stream: MediaStream? = null
			if (isVideoCallEnabled) {
				videoTrack = createVideoTrack(videoCapturer!!)
			}
			val audioTrack = createAudioTrack(audioConstraints)
			if ((videoTrack != null) || (audioTrack != null)) {
				stream = factory!!.createLocalMediaStream("ARDAMS")
				if (audioTrack != null) {
					stream.addTrack(audioTrack)
				}
				if (videoTrack != null) {
					stream.addTrack(videoTrack)
				}
			}
			peerConnection = createPeerConnection(rtcConfig, publisher)
			if (stream != null) {
				peerConnection!!.addStream(stream)
				mLocalStream = stream
			}
			val dataChannelParameters = peerConnectionParameters.dataChannelParameters
			if (dataChannelEnabled && (dataChannelParameters != null)) {
				val init = DataChannel.Init()
				init.ordered = dataChannelParameters.ordered
				init.negotiated = dataChannelParameters.negotiated
				init.maxRetransmits = dataChannelParameters.maxRetransmits
				init.maxRetransmitTimeMs = dataChannelParameters.maxRetransmitTimeMs
				init.id = dataChannelParameters.id
				init.protocol = dataChannelParameters.protocol
				dataChannel = peerConnection!!.createDataChannel("ApprtcDemo data", init)
			}
		}

		if (peerConnectionParameters.aecDump) {
			try {
				val aecDumpFileDescriptor =
					ParcelFileDescriptor.open(
						File(
							(Environment.getExternalStorageDirectory().path
								+ File.separator + "Download/audio.aecdump")
						),
						(ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
							or ParcelFileDescriptor.MODE_TRUNCATE)
					)
				factory!!.startAecDump(aecDumpFileDescriptor.detachFd(), -1)
			} catch (e: IOException) {
				Log.e(TAG, "Can not open aecdump file", e)
			}
		}

		if (saveRecordedAudioToFile != null) {
			if (saveRecordedAudioToFile!!.start()) {
				if (DEBUG) Log.d(TAG, "Recording input audio to file is activated")
			}
		}

		var rtcEventLog: RtcEventLog? = null
		if (peerConnectionParameters.enableRtcEventLog) {
			rtcEventLog = RtcEventLog(peerConnection!!)
			rtcEventLog.start(createRtcEventLogOutputFile())
		} else {
			if (DEBUG) Log.d(TAG, "com.serenegiant.webrtc.RtcEventLog is disabled.")
		}
		if (DEBUG) Log.d(TAG, "Peer connection created.")

		publisher.setPeerConnection(peerConnection!!, dataChannel, rtcEventLog)
		publisher.attach()
	}

	/**
	 * Subscriberを生成
	 * FIXME マルチストリーム対応を追加する
	 * @param info
	 */
	protected fun createSubscriber(
		info: PublisherInfo
	) {
		if (DEBUG) Log.v(TAG, "createSubscriber:")

		val context = context
		if ((context == null) || (factory == null) || isError) {
			Log.e(TAG, "createSubscriber:PeerConnection factory is not created")
			return
		}
		if (DEBUG) Log.d(TAG, "createSubscriber:Create peer connection.")


//		if (isVideoCallEnabled()) {
//			factory.setVideoHwAccelerationOptions(
//				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
//		}

		// Create SDP constraints.
		val sdpMediaConstraints = MediaConstraints()
		sdpMediaConstraints.mandatory.add(
			MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
		)
		sdpMediaConstraints.mandatory.add(
			MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
		)
		sdpMediaConstraints.optional.add(
			MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true")
		)

		val rtcConfig =
			RTCConfiguration(mCallback.getIceServers(this))
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
		rtcConfig.continualGatheringPolicy =
			PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA
		rtcConfig.sdpSemantics = SDP_SEMANTICS

		// FIXME マルチストリームの時はここで既存のSubscriberを検索して
		//       なければ新規追加、あればトラックを追加みたいになるのかな？
		val subscriber = VideoRoomPlugin.Subscriber(
			mJanus!!, mSession!!, mVideoRoomCallback,
			peerConnectionParameters,
			roomConnectionParameters,
			sdpMediaConstraints,
			info, isVideoCallEnabled
		)

		val peerConnection: PeerConnection?
		var dataChannel: DataChannel? = null
		if (SDP_SEMANTICS == SdpSemantics.UNIFIED_PLAN) {
			peerConnection = createPeerConnection(rtcConfig, subscriber)
			val dataChannelParameters = peerConnectionParameters.dataChannelParameters
			if (dataChannelEnabled && (dataChannelParameters != null)) {
				val init = DataChannel.Init()
				init.ordered = dataChannelParameters.ordered
				init.negotiated = dataChannelParameters.negotiated
				init.maxRetransmits = dataChannelParameters.maxRetransmits
				init.maxRetransmitTimeMs = dataChannelParameters.maxRetransmitTimeMs
				init.id = dataChannelParameters.id
				init.protocol = dataChannelParameters.protocol
				dataChannel = peerConnection!!.createDataChannel("ApprtcDemo data", init)
			}

			if (isVideoCallEnabled) {
				// We can add the renderers right away because we don't need to wait for an
				// answer to get the remote track.
				val remoteVideoTrack = getRemoteVideoTrack(peerConnection!!)
				if (remoteVideoTrack != null) {
					mVideoSinkLock.withLock {
						var holder = getHolder(info.id!!)
						if (holder == null) {
							val remoteVideoSinks = mCallback.getRemoteVideoSink(info)
							if (remoteVideoSinks.isNotEmpty()) {
								remoteVideoTrack.setEnabled(renderVideo)
								Log.i(TAG, "createSubscriber: create VideoSinkHolder")
								holder =
									VideoSinkHolder(info.id, remoteVideoTrack, remoteVideoSinks)
								remoteVideoSinkMap.put(info.id, holder)
							} else if (DEBUG) {
								Log.v(TAG, "createSubscriber:remote video sinks are empty")
							} else {

							}
						} else {
							Log.w(TAG, "createSubscriber: unexpectedly video sink holder is already exists!")
						}
					}
				}
			}
		} else {
			peerConnection = createPeerConnection(rtcConfig, subscriber)
			val dataChannelParameters = peerConnectionParameters.dataChannelParameters
			if (dataChannelEnabled && (dataChannelParameters != null)) {
				val init = DataChannel.Init()
				init.ordered = dataChannelParameters.ordered
				init.negotiated = dataChannelParameters.negotiated
				init.maxRetransmits = dataChannelParameters.maxRetransmits
				init.maxRetransmitTimeMs = dataChannelParameters.maxRetransmitTimeMs
				init.id = dataChannelParameters.id
				init.protocol = dataChannelParameters.protocol
				dataChannel = peerConnection!!.createDataChannel("ApprtcDemo data", init)
			}
		}

		if (saveRecordedAudioToFile != null) {
			if (saveRecordedAudioToFile!!.start()) {
				if (DEBUG) Log.d(TAG, "createSubscriber:Recording input audio to file is activated")
			}
		}

		var rtcEventLog: RtcEventLog? = null
		if (peerConnectionParameters.enableRtcEventLog) {
			rtcEventLog = RtcEventLog(peerConnection!!)
			rtcEventLog.start(createRtcEventLogOutputFile())
		} else {
			if (DEBUG) Log.d(TAG, "createSubscriber: RtcEventLog is disabled.")
		}
		if (DEBUG) Log.d(TAG, "createSubscriber: Peer connection created.")

		subscriber.setPeerConnection(peerConnection!!, dataChannel, rtcEventLog)
		subscriber.attach()
	}

	private fun createRtcEventLogOutputFile(): File {
		if (DEBUG) Log.v(TAG, "createRtcEventLogOutputFile:")
		val dateFormat: DateFormat = SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault())
		val date = Date()
		val outputFileName = "event_log_" + dateFormat.format(date) + ".log"
		return File(
			context!!.getDir(AppRTCConst.RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE),
			outputFileName
		)
	}

	private fun createAudioTrack(audioConstraints: MediaConstraints): AudioTrack? {
		if (DEBUG) Log.v(TAG, "createAudioTrack:")
		audioSource = factory!!.createAudioSource(audioConstraints)
		val track = factory!!.createAudioTrack(AppRTCConst.AUDIO_TRACK_ID, audioSource)
		localAudioTrack = track
		track.setEnabled(enableAudio)
		return localAudioTrack
	}

	private fun createVideoTrack(capturer: VideoCapturer): VideoTrack? {
		if (DEBUG) Log.v(TAG, "createVideoTrack:")
		surfaceTextureHelper =
			SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
		val source = factory!!.createVideoSource(capturer.isScreencast)
		videoSource = source
		capturer.initialize(surfaceTextureHelper, context, source.getCapturerObserver())
		capturer.startCapture(videoWidth, videoHeight, videoFps)

		val track = factory!!.createVideoTrack(AppRTCConst.VIDEO_TRACK_ID, source)
		localVideoTrack = track
		track.setEnabled(renderVideo)
		track.addSink(localRender)
		return localVideoTrack
	}

	private fun findVideoSender(peerConnection: PeerConnection) {
		if (DEBUG) Log.v(TAG, "findVideoSender:")
		for (sender in peerConnection.senders) {
			if (sender.track() != null) {
				val trackType = sender.track()!!.kind()
				if (trackType == AppRTCConst.VIDEO_TRACK_TYPE) {
					if (DEBUG) Log.d(TAG, "Found video sender.")
					localVideoSender = sender
				}
			}
		}
	}

	/**
	 * Returns the remote VideoTrack, assuming there is only one.
	 * @param peerConnection
	 * @return
	 */
	private fun getRemoteVideoTrack(peerConnection: PeerConnection): VideoTrack? {
		if (DEBUG) Log.v(TAG, "getRemoteVideoTrack:")
		for (transceiver in peerConnection.transceivers) {
			val track = transceiver.receiver.track()
			if (DEBUG) Log.v(TAG, "getRemoteVideoTrack:transceiver=$transceiver,track=$track")
			if (track is VideoTrack) {
				return track
			}
		}
		return null
	}

	private fun switchCameraInternal() {
		if (DEBUG) Log.v(TAG, "switchCameraInternal:")
		if (videoCapturer is CameraVideoCapturer) {
			if (!isVideoCallEnabled || isError) {
				Log.e(TAG, "Failed to switch camera. Video: $isVideoCallEnabled. Error : $isError")
				return  // No video is sent or only one camera is available or error happened.
			}
			if (DEBUG) Log.d(TAG, "Switch camera")
			val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
			cameraVideoCapturer.switchCamera(null)
		} else {
			if (DEBUG) Log.d(TAG, "Will not switch camera, video caputurer is not a camera")
		}
	}

	private fun changeCaptureFormatInternal(width: Int, height: Int, framerate: Int) {
		if (DEBUG) Log.v(TAG, "changeCaptureFormatInternal:")
		if (!isVideoCallEnabled || isError || videoCapturer == null) {
			Log.e(TAG, "Failed to change capture format. Video: $isVideoCallEnabled. Error : $isError")
			return
		}
		if (DEBUG) Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate)
		videoSource!!.adaptOutputFormat(width, height, framerate)
	}

	@SuppressWarnings("deprecation")
	private fun stats(): Unit {
		// TODO(sakal): getStats is deprecated.
		if (DEBUG) Log.v(TAG, "getStats:")
		mPluginLock.withLock {
			for (plugin in mAttachedPlugins.values) {
				plugin.requestStats()
			}
		}
		// FIXME 未実装 PublisherのPeerConnectionから取得する
//		if (peerConnection == null || isError) {
//			return;
//		}
//		boolean success = peerConnection.getStats(new StatsObserver() {
//			@Override
//			public void onComplete(final StatsReport[] reports) {
//				events.onPeerConnectionStatsReady(reports);
//			}
//		}, null);
//		if (!success) {
//			Log.e(TAG, "getStats() returns false!");
//		}
	}

	private val context: Context?
		//--------------------------------------------------------------------------------
		get() = mWeakContext.get()

	private fun onAddRemoteStream(
		info: PublisherInfo,
		remoteStream: MediaStream
	) {
		if (DEBUG) Log.v(TAG, "onAddRemoteStream:${getHolder(info.id!!)}")
		if (isVideoCallEnabled
			&& (remoteStream != null) && !remoteStream.videoTracks.isEmpty()
		) {
			val remoteVideoTrack = remoteStream.videoTracks[0]
			var holder: VideoSinkHolder?
			mVideoSinkLock.withLock {
				holder = getHolder(info.id!!)
				if (holder == null) {
					if (remoteVideoTrack != null) {
						val remoteVideoSinks = mCallback.getRemoteVideoSink(info)
						if (!remoteVideoSinks.isEmpty()) {
							remoteVideoTrack.setEnabled(renderVideo)
							if (DEBUG) Log.v(TAG, "onAddRemoteStream:create VideoSinkHolder")
							holder = VideoSinkHolder(info.id, remoteVideoTrack, remoteVideoSinks)
							remoteVideoSinkMap[info.id] = holder!!
						} else if (DEBUG) {
							Log.v(TAG, "onAddRemoteStream:remote video sinks are empty")
						}
					}
				}
			}
			if (holder != null) {
				holder!!.setMediaStream(remoteStream)
			}
		}
	}

	private fun onRemoveRemoteStream(
		info: PublisherInfo,
		remoteStream: MediaStream
	) {
		if (DEBUG) Log.v(TAG, "onAddRemoteStream:${getHolder(info.id!!)}")
		mVideoSinkLock.withLock {
			val removed = remoteVideoSinkMap.remove(info.id)
			if (DEBUG) Log.v(TAG, "onAddRemoteStream:removed=$removed")
		}
	}

	private fun getHolder(feedId: Long): VideoSinkHolder? {
		return if (remoteVideoSinkMap.containsKey(feedId)) remoteVideoSinkMap[feedId] else null
	}

	/**
	 * set call that is currently in progress
	 * @param call
	 */
	private fun addCall(call: Call<*>) {
		mCallLock.withLock {
			mCurrentCalls.add(call)
		}
	}

	private fun removeCall(call: Call<*>) {
		mCallLock.withLock {
			mCurrentCalls.remove(call)
		}
		if (!call.isCanceled) {
			try {
				call.cancel()
			} catch (e: Exception) {
				Log.w(TAG, e)
			}
		}
	}

	/**
	 * cancel call if call is in progress
	 */
	private fun cancelCall() {
		mCallLock.withLock {
			for (call in mCurrentCalls) {
				if (!call.isCanceled) {
					try {
						call.cancel()
					} catch (e: Exception) {
						Log.w(TAG, e)
					}
				}
			}
			mCurrentCalls.clear()
		}
	}

	//--------------------------------------------------------------------------------
	private fun addPlugin(plugin: VideoRoomPlugin) {
		mPluginLock.withLock {
			mAttachedPlugins.put(plugin.pluginId(), plugin)
		}
	}

	private fun removePlugin(plugin: VideoRoomPlugin) {
		val key = plugin.pluginId()
		mScope.launch {
			mPluginLock.withLock {
				mAttachedPlugins.remove(key)
			}
		}
	}

	private fun getPlugin(key: Long): VideoRoomPlugin? {
		mPluginLock.withLock {
			if (mAttachedPlugins.containsKey(key)) {
				return mAttachedPlugins[key]
			}
		}
		return null
	}

	private fun leavePlugin(leavePlugin: Long, numUsers: Int) {
		if (DEBUG) Log.v(TAG, "leavePlugin:$leavePlugin")
		var found: VideoRoomPlugin? = null

		mPluginLock.withLock {
			// feederIdが一致するSubscriberを探す
			for ((_, plugin) in mAttachedPlugins) {
				if (plugin is VideoRoomPlugin.Subscriber) {
					if (leavePlugin == plugin.feedId) {
						found = plugin
						break
					}
				}
			}
		}
		if (DEBUG) Log.v(TAG, "leavePlugin:found=$found")
		found?.let {
			// feederIdが一致するSubscriberが見つかった時はdetachする
			val subscriber: VideoRoomPlugin = it
			mScope.launch {
				subscriber.detach()
				mCallback.onLeave(
					(subscriber as VideoRoomPlugin.Subscriber).publisherInfo, numUsers
				)
			}
		}
	}

	//--------------------------------------------------------------------------------
	/**
	 * notify error
	 * @param t
	 */
	private fun reportError(t: Throwable) {
		Log.w(TAG, t)
		cancelCall()
		clearTransactions()
		try {
			mScope.launch {
				if (mConnectionState != ConnectionState.ERROR) {
					mConnectionState = ConnectionState.ERROR
					mCallback.onChannelError(t)
				}
			}
		} catch (e: Exception) {
			// ignore, will be already released.
		}
	}

	private fun listRoomInternal(
		roomUrl: String,
		apiName: String,
		callback: ListCallback<List<RoomInfo?>?>
	) {
		if (DEBUG) Log.v(TAG, "listRoomInternal:")
//		final VideoRoomAPI api = setupRetrofit(
//			setupHttpClient(false, HTTP_READ_TIMEOUT_MS, HTTP_WRITE_TIMEOUT_MS, DEFAULT_BUILDER_CALLBACK),
//			roomUrl, DEFAULT_BUILDER_CALLBACK).create(VideoRoomAPI.class);
		// FIXME 未実装
	}

	/**
	 * Connects to room - function runs on a local looper thread.
	 */
	private fun connectToRoomInternal() {
		if (DEBUG) Log.v(TAG, "connectToRoomInternal:")
		// 通常のRESTアクセス用APIインターフェースを生成
		val api = Utils.setupRetrofit(
			Utils.setupHttpClient(
				false,
				Const.HTTP_READ_TIMEOUT_MS,
				Const.HTTP_WRITE_TIMEOUT_MS,
				mCallback
			),
			roomConnectionParameters.roomUrl, mCallback
		).create(VideoRoomAPI::class.java)
		mJanus = api
		// long poll用APIインターフェースを生成
		mLongPoll = Utils.setupRetrofit(
			Utils.setupHttpClient(
				true,
				Const.HTTP_READ_TIMEOUT_MS_LONG_POLL,
				Const.HTTP_WRITE_TIMEOUT_MS,
				mCallback
			),
			roomConnectionParameters.roomUrl, mCallback
		).create(LongPoll::class.java)
		try {
			requestServerInfo(api)
		} catch (e: Exception) {
			Log.w(TAG, e)
		}
	}

	/**
	 * Disconnect from room and send bye messages - runs on a local looper thread.
	 */
	private fun disconnectFromRoomInternal() {
		if (DEBUG) Log.v(TAG, "disconnectFromRoomInternal:state=$mConnectionState")
		cancelCall()
		if ((mConnectionState == ConnectionState.CONNECTED)) {
			if (DEBUG) Log.d(TAG, "Closing room.")
			detachAll()
		}
		destroy()
	}

	//--------------------------------------------------------------------
	private fun requestServerInfo(api: VideoRoomAPI) {
		if (DEBUG) Log.v(TAG, "requestServerInfo:")
		mLastJob?.cancel()
		mLastJob = mScope.launch {
			try {
				// Janus-gatewayサーバー情報を取得
				val info = api.getInfo(roomConnectionParameters.apiName)
				mServerInfo = info
				if (DEBUG) Log.v(TAG, "requestServerInfo:success,$info")
				mScope.launch {
					createSession()
				}
			} catch (e: Exception) {
				reportError(e)
			}
		}
	}

	private fun createSession() {
		if (DEBUG) Log.v(TAG, "createSession:")
		val janus = mJanus
		if (janus != null) {
			mLastJob?.cancel()
			mScope.launch {
				try {
					// サーバー情報を取得できたらセッションを生成
					val session = mJanus!!.createSession(
						roomConnectionParameters.apiName, CreateSession())
					mSession = session
					if ("success" == mSession!!.janus) {
						mConnectionState = ConnectionState.READY
						// セッションを生成できた＼(^o^)／
						if (DEBUG) Log.v(TAG, "createSession:success,$session")
						// パブリッシャーをVideoRoomプラグインにアタッチ
						mScope.launch {
							longPoll()
							mCallback.onConnectServer(this@JanusVideoRoomClient)
						}
					} else {
						mSession = null
						reportError(RuntimeException("unexpected result:$session"))
					}
				} catch (e: Exception) {
					reportError(e)
				}
			}
		} else {
			reportError(RuntimeException("Unexpectedly mJanus is null"))
		}
	}

	/**
	 * detachAll from VideoRoom plugin
	 */
	private fun detachAll() {
		if (DEBUG) Log.v(TAG, "detachAll:")
		cancelCall()
		mConnectionState = ConnectionState.CLOSED
		mPluginLock.withLock {
			for ((_, value) in mAttachedPlugins) {
				value.detach()
			}
			mAttachedPlugins.clear()
		}
	}

	/**
	 * destroy session
	 */
	private fun destroy() {
		if (DEBUG) Log.v(TAG, "destroy:")
		cancelCall()
		detachAll()
		val session = mSession
		mSession = null
		val janus = mJanus
		if ((session != null) && (janus != null)) {
			mLastJob?.cancel()
			try {
				mLastJob = mScope.launch {
					janus.destroySession(
						roomConnectionParameters.apiName, session.id(),
						DestroySession(session, null)
					)
				}
			} catch (e: Exception) {
				reportError(e)
			}
		}
		mServerInfo = null
		mConnectionState = ConnectionState.CLOSED
		clearTransactions()
		mJanus = null
		mLocalStream = null
		remoteVideoSinkMap.clear()
		cancelTimerTask()
		statsTimer.cancel()
		if (DEBUG) Log.d(TAG, "Closing audio source.")
		audioSource?.dispose()
		audioSource = null
		mAudioDeviceModule = null
		if (DEBUG) Log.d(TAG, "Stopping capture.")
		if (videoCapturer != null) {
			try {
				videoCapturer!!.stopCapture()
			} catch (e: InterruptedException) {
				throw RuntimeException(e)
			}
			videoCapturerStopped = true
			videoCapturer!!.dispose()
			videoCapturer = null
		}
		if (DEBUG) Log.d(TAG, "Closing video source.")
		videoSource?.dispose()
		videoSource = null
		if (saveRecordedAudioToFile != null) {
			if (DEBUG) Log.d(TAG, "Closing audio file for recorded input audio.")
			saveRecordedAudioToFile?.stop()
			saveRecordedAudioToFile = null
		}
		localRender = null
		if (factory != null && peerConnectionParameters.aecDump) {
			factory!!.stopAecDump()
		}
		try {
			surfaceTextureHelper?.dispose()
		} catch (e: Exception) {
			if (DEBUG) Log.w(TAG, e)
		}
		surfaceTextureHelper = null
		if (DEBUG) Log.d(TAG, "Closing peer connection factory.")
		factory?.dispose()
		factory = null
		rootEglBase.release()
		if (DEBUG) Log.d(TAG, "Closing peer connection done.")
		mCallback.onDisconnected()
		PeerConnectionFactory.stopInternalTracingCapture()
		PeerConnectionFactory.shutdownInternalTracer()
	}

	private fun setVideoMaxBitrate(maxBitrateKbps: Int) {
		if (DEBUG) Log.v(TAG, "maxBitrateKbps:")
		mScope.launch {
			if (localVideoSender == null || isError) {
				return@launch
			}
			if (DEBUG) Log.d(TAG, "Requested max video bitrate: $maxBitrateKbps")
			if (localVideoSender == null) {
				Log.w(TAG, "Sender is not ready.")
				return@launch
			}

			val parameters = localVideoSender!!.parameters
			if (parameters.encodings.size == 0) {
				Log.w(TAG, "RtpParameters are not ready.")
				return@launch
			}

			for (encoding in parameters.encodings) {
				// Null value means no limit.
				encoding.maxBitrateBps =
					if (maxBitrateKbps == 0) null else maxBitrateKbps * AppRTCConst.BPS_IN_KBPS
			}
			if (!localVideoSender!!.setParameters(parameters)) {
				Log.e(TAG, "RtpSender.setParameters failed.")
			}
			if (DEBUG) Log.d(TAG, "Configured max video bitrate to: $maxBitrateKbps")
		}
	}

	//--------------------------------------------------------------------------------
	/**
	 * JanusPluginからのコールバックリスナーの実装
	 */
	private val mVideoRoomCallback
		: VideoRoomCallback = object : VideoRoomCallback {
		override fun onAttach(plugin: JanusPlugin) {
			if (DEBUG) Log.v(TAG, "onAttach:$plugin" )
			if (plugin is VideoRoomPlugin) {
				addPlugin(plugin)
			}
		}

		override fun onDetach(plugin: JanusPlugin) {
			if (DEBUG) Log.v(TAG, "onDetach:$plugin")

			if (plugin is VideoRoomPlugin) {
				removePlugin(plugin)
			}
		}

		override fun onJoin(
			plugin: VideoRoomPlugin,
			room: RoomEvent
		) {
			if (DEBUG) Log.v(TAG, "onJoin:$plugin")
			if (plugin is VideoRoomPlugin.Publisher) {
				mConnectionState = ConnectionState.CONNECTED
				handleOnJoin(plugin, room)
				plugin.createOffer()
			} else if (plugin is VideoRoomPlugin.Subscriber) {
				handleOnJoin(plugin, room)
				plugin.createAnswer()
			}
		}

		override fun onEnter(plugin: VideoRoomPlugin) {
			if (DEBUG) Log.v(TAG, "onEnter:$plugin")
			if (plugin is VideoRoomPlugin.Subscriber) {
				mCallback.onEnter(plugin.publisherInfo)
			}
		}

		override fun onLeave(
			plugin: VideoRoomPlugin,
			pluginId: Long, numUsers: Int
		) {
			if (DEBUG) Log.v(TAG, "onLeave:$plugin,leave=$pluginId")

			mScope.launch { leavePlugin(pluginId, numUsers) }
		}

		override fun onAddRemoteStream(
			plugin: VideoRoomPlugin,
			stream: MediaStream
		) {
			if (DEBUG) Log.v(TAG, "onAddRemoteStream:$plugin")
			if (plugin is VideoRoomPlugin.Subscriber) {
				mScope.launch {
					this@JanusVideoRoomClient.onAddRemoteStream(
						plugin.publisherInfo, stream
					)
				}
			}
		}

		override fun onRemoveStream(
			plugin: VideoRoomPlugin,
			stream: MediaStream
		) {
			if (DEBUG) Log.v(TAG, "onRemoveStream:$plugin")
			if (plugin is VideoRoomPlugin.Subscriber) {
				mScope.launch {
					this@JanusVideoRoomClient.onRemoveRemoteStream(
						plugin.publisherInfo, stream
					)
				}
			}
		}

		override fun onRemoteIceCandidate(
			plugin: VideoRoomPlugin,
			candidate: IceCandidate
		) {
			if (DEBUG) Log.v(TAG, ("onRemoteIceCandidate:$plugin$candidate".trimIndent()))
			mScope.launch { mCallback.onRemoteIceCandidate(candidate) }
		}

		override fun onIceConnected(plugin: VideoRoomPlugin) {
			if (DEBUG) Log.v(TAG, "onIceConnected:$plugin")
			if (plugin is VideoRoomPlugin.Publisher) {
				// 複数のSubscriberが存在しうるのでPublisherからのイベントのみハンドリング
				mScope.launch {	mCallback.onIceConnected() }
			}
		}

		override fun onIceDisconnected(plugin: VideoRoomPlugin) {
			if (DEBUG) Log.v(TAG, "onIceDisconnected:$plugin")
			if (plugin is VideoRoomPlugin.Publisher) {
				// 複数のSubscriberが存在しうるのでPublisherからのイベントのみハンドリング
				mScope.launch { mCallback.onIceDisconnected() }
			}
		}

		override fun onLocalDescription(
			plugin: VideoRoomPlugin,
			sdp: SessionDescription
		) {
			if (DEBUG) Log.v(TAG, "onLocalDescription:$plugin")
//			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			mScope.launch {
//				logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
				if (peerConnectionParameters.videoMaxBitrate > 0) {
					if (DEBUG) Log.d(TAG, "Set video maximum bitrate: ${peerConnectionParameters.videoMaxBitrate}")
					setVideoMaxBitrate(
						peerConnectionParameters.videoMaxBitrate
					)
				}
			}
		}

		override fun createSubscriber(
			plugin: VideoRoomPlugin,
			info: PublisherInfo
		) {
			if (DEBUG) Log.v(TAG, "createSubscriber:$plugin")
			mScope.launch {
				if (mCallback.onNewPublisher(info)) {
					this@JanusVideoRoomClient.createSubscriber(info)
				}
			}
		}

		override fun onRemoteDescription(
			plugin: VideoRoomPlugin,
			sdp: SessionDescription
		) {
			if (DEBUG) Log.v(TAG, ("onRemoteDescription:$plugin$sdp".trimIndent()))

			mScope.launch { mCallback.onRemoteDescription(sdp) }
		}

		override fun onPeerConnectionStatsReady(
			plugin: VideoRoomPlugin,
			report: RTCStatsReport
		) {
			mScope.launch {
				mCallback.onPeerConnectionStatsReady(
					plugin is VideoRoomPlugin.Publisher, report
				)
			}
		}

		override fun onError(
			plugin: VideoRoomPlugin,
			t: Throwable
		) {
			reportError(t)
		}
	}

	//--------------------------------------------------------------------------------
	/**
	 * TransactionManagerからのコールバック
	 */
	private val mTransactionCallback
		: TransactionCallback = object : TransactionCallback {
		override fun onReceived(
			transaction: String,
			json: JSONObject
		): Boolean {
			if (DEBUG) Log.v(TAG, "onReceived:$json")
			return false
		}
	}

	/**
	 * コンストラクタ
	 * @param appContext
	 * @param eglBase
	 * @param peerConnectionParameters
	 * @param roomConnectionParameters
	 * @param callback
	 */
	init {
		this.mConnectionState = ConnectionState.UNINITIALIZED
		this.dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null

		val fieldTrials = peerConnectionParameters.fieldTrials
		mScope.launch {
			if (DEBUG) Log.d(TAG, ("Initialize WebRTC. Field trials: $fieldTrials Enable video HW acceleration: ${peerConnectionParameters.videoCodecHwAcceleration}"))
			PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder(appContext)
					.setFieldTrials(fieldTrials)
//					.setEnableVideoHwAcceleration(peerConnectionParameters.videoCodecHwAcceleration)
					.setEnableInternalTracer(true)
					.createInitializationOptions()
			)
		}
	}

	/**
	 * long poll asynchronously
	 */
	private fun longPoll() {
		if (DEBUG) Log.v(TAG, "longPoll:")
		if (mSession == null) return
		// XXX long pollは従来通りCallを使った実装のままにしておく
		val call = mLongPoll!!.getEvent(
			roomConnectionParameters.apiName, mSession!!.id()
		)
		addCall(call)
		call.enqueue(object : Callback<ResponseBody> {
			override fun onResponse(
				call: Call<ResponseBody>,
				response: Response<ResponseBody>
			) {
				if (DEBUG) Log.v(TAG, "longPoll:onResponse")
				removeCall(call)
				if ((mConnectionState == ConnectionState.READY)
					|| (mConnectionState == ConnectionState.CONNECTED)
				) {
					try {
						mScope.launch {
							handleLongPoll(call, response)
						}
						recall(call)
					} catch (e: Exception) {
						reportError(e)
					}
				} else {
					// 通話終了時に毎回unexpected state:CLOSEDと出てしまうのでDEBUGフラグがtrueの時のみ出力
					if (DEBUG) Log.w(TAG, "unexpected state:$mConnectionState")
				}
			}

			override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
				if (DEBUG) Log.v(TAG, "longPoll:onFailure=$t")
				removeCall(call)
				// FIXME タイムアウトの時は再度long pollする？
				if (t !is IOException || "Canceled" != t.message) {
					reportError(t)
				}
				if (mConnectionState != ConnectionState.ERROR) {
					recall(call)
				}
			}

			fun recall(call: Call<ResponseBody>) {
				val newCall = call.clone()
				addCall(newCall)
				newCall.enqueue(this)
			}
		})
	}

	/**
	 * long pollによるjanus-gatewayサーバーからの受信イベントの処理の実体
	 * @param call
	 * @param response
	 */
	private fun handleLongPoll(
		call: Call<ResponseBody>,
		response: Response<ResponseBody>
	) {
		if (DEBUG) Log.v(TAG, "handleLongPoll:")
		val responseBody = response.body()
		if (response.isSuccessful && (responseBody != null)) {
			try {
				val body = JSONObject(responseBody.string())
				try {
					responseBody.close()
				} catch (e: Exception) {
					if (DEBUG) Log.w(TAG, e)
				}
				val transaction = body.optString("transaction")
				val sender = body.optLong("sender")
				if (!TextUtils.isEmpty(transaction)) {
					// トランザクションコールバックでの処理を試みる
					// WebRTCイベントはトランザクションがない
					if (handleTransaction(transaction, body)) {
						return  // 処理済みの時はここで終了
					}
				}
				val plugin = getPlugin(sender)
				if (plugin != null) {
					if (DEBUG) Log.v(TAG, "handlePluginEvent: try handle message on plugin specified by sender")
					if (plugin.onReceived("", body)) {
						return
					}
				}

				if (DEBUG) Log.v(TAG, "handleLongPoll:unhandled transaction")
				val janus = body.optString("janus")
				if (!TextUtils.isEmpty(janus)) {
					when (janus) {
						"ack" -> {
							// do nothing
						}
						"keepalive" -> {
							// サーバー側がタイムアウト(30秒？)した時は{"janus": "keepalive"}が来る
							// do nothing
						}
						"event" -> {
							// プラグインイベント
							handlePluginEvent(body)
						}
						"detached" -> {
							// FIXME #detachAllを呼ぶ？
						}
						"media",
						"webrtcup",
						"slowlink",
						"hangup" -> {
							// event for WebRTC
							handleWebRTCEvent(body)
						}
						"error" -> {
							reportError(RuntimeException("error response $response"))
						}
						else -> {
							Log.d(TAG, "handleLongPoll:unknown event:$body")
						}
					}
				}
			} catch (e: JSONException) {
				reportError(e)
			} catch (e: IOException) {
				reportError(e)
			}
		}
	}

	/**
	 * プラグインイベントの処理
	 * @param body
	 */
	private fun handlePluginEvent(body: JSONObject) {
		if (DEBUG) Log.v(TAG, "handlePluginEvent:$body")
		val gson = Gson()
		val event = gson.fromJson(body.toString(), RoomEvent::class.java)

		if (DEBUG) Log.v(TAG, "handlePluginEvent: unhandled event")
	}

	private fun handleOnJoin(
		plugin: VideoRoomPlugin,
		room: RoomEvent
	) {
		if (DEBUG) Log.v(TAG, "handleOnJoin:")
		// roomにjoinできた
		// Fire connection and signaling events.
		room.plugindata?.data?.let { data ->
			mCallback.onConnectedToRoom(true, data)
		}
	}

	/**
	 * WebRTC関係のメッセージの処理
	 * @param body
	 */
	private fun handleWebRTCEvent(body: JSONObject) {
		if (DEBUG) Log.v(TAG, "handleWebRTCEvent:$body")
		when (body.optString("janus")) {
			"media", "webrtcup", "slowlink" -> mCallback.onEvent(body)
			"hangup" -> mCallback.onChannelClose()
			else -> {}
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG = JanusVideoRoomClient::class.java.simpleName

		private val SDP_SEMANTICS = SdpSemantics.UNIFIED_PLAN
	}
}
