package com.serenegiant.janusrtcandroid
/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 - 2022 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.serenegiant.janus.JanusCallback
import com.serenegiant.janus.JanusVideoRoomClient
import com.serenegiant.janus.ProxyVideoSink
import com.serenegiant.janus.VideoRoomClient
import com.serenegiant.janus.request.videoroom.ConfigPublisher
import com.serenegiant.janus.response.videoroom.PublisherInfo
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.serenegiant.janusrtcandroid.CallFragment.OnCallEvents
import com.serenegiant.janusrtcandroid.CpuMonitor.Companion.isSupported
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.appspot.apprtc.*
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import org.webrtc.RendererCommon.ScalingType
import retrofit2.Retrofit
import java.io.IOException

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
class CallActivity : BaseActivity(), OnCallEvents {
	private val remoteProxyRenderer1 = ProxyVideoSink()
	private val remoteProxyRenderer2 = ProxyVideoSink()
	private val localProxyVideoSink = ProxyVideoSink()
	private lateinit var pipRenderer1: SurfaceViewRenderer
	private lateinit var pipRenderer2: SurfaceViewRenderer
	private lateinit var fullscreenRenderer: SurfaceViewRenderer
	private var janusClient: VideoRoomClient? = null
	private var audioManager: IAppRTCAudioManager? = null
	private var videoFileRenderer: VideoFileRenderer? = null
	private val remoteRenderers: MutableList<VideoSink> = ArrayList()
	private var logToast: Toast? = null
	private var commandLineRun = false
	private var activityRunning = false
	private var roomConnectionParameters: RoomConnectionParameters? = null
	private var peerConnectionParameters: PeerConnectionParameters? = null
	private var iceConnected = false
	private var isError = false
	private var callControlFragmentVisible = true
	private var callStartedTimeMs: Long = 0
	private var micEnabled = true
	private var screenCaptureEnabled = false
	private var currentFullIx = 0 // 0: 自分, 1: リモート1, 2: リモート2

	// Controls
	private lateinit var callFragment: CallFragment
	private lateinit var hudFragment: HudFragment
	private var cpuMonitor: CpuMonitor? = null
	private var mNumUsers = 0

	// TODO(bugs.webrtc.org/8580): LayoutParams.FLAG_TURN_SCREEN_ON and
	// LayoutParams.FLAG_SHOW_WHEN_LOCKED are deprecated.
	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
		Thread.setDefaultUncaughtExceptionHandler(UnhandledExceptionHandler(this))

		// Set window styles for fullscreen-window size. Needs to be done before
		// adding content.
		requestWindowFeature(Window.FEATURE_NO_TITLE)
		window.addFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN
				or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
		)
		window.decorView.systemUiVisibility = systemUiVisibility
		setContentView(R.layout.activity_call)
		iceConnected = false
		currentFullIx = -1

		// Create UI controls.
		pipRenderer1 = findViewById(R.id.pip_video_view1)
		pipRenderer2 = findViewById(R.id.pip_video_view2)
		fullscreenRenderer = findViewById(R.id.fullscreen_video_view)
		callFragment = CallFragment()
		hudFragment = HudFragment()

		// Swap feeds on pip view click.
		pipRenderer1.setOnClickListener { setSwappedFeeds(1) }
		pipRenderer2.setOnClickListener { setSwappedFeeds(2) }

		// Show/hide call control fragment on view click.
		fullscreenRenderer.setOnClickListener { toggleCallControlFragmentVisibility() }
		remoteRenderers.add(remoteProxyRenderer1)
		remoteRenderers.add(remoteProxyRenderer2)
		val intent = intent
		val eglBase = EglBase.create()

		// Create video renderers.
		pipRenderer1.init(eglBase.eglBaseContext, null)
		pipRenderer1.setScalingType(ScalingType.SCALE_ASPECT_FIT)
		pipRenderer2.init(eglBase.eglBaseContext, null)
		pipRenderer2.setScalingType(ScalingType.SCALE_ASPECT_FIT)

		// FIXME リモート毎に割り振らないといけないのでここではセットしない
		//       必要ならgetRemoteVideoSinkで割り振る
//		val saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)
//		// When saveRemoteVideoToFile is set we save the video from the remote to a file.
//		if (saveRemoteVideoToFile != null) {
//			int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
//			int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
//			try {
//				videoFileRenderer = new VideoFileRenderer(
//					saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
//				remoteRenderers.add(videoFileRenderer);
//			} catch (IOException e) {
//				throw new RuntimeException(
//					"Failed to open video file for output: " + saveRemoteVideoToFile, e);
//			}
//		}
		fullscreenRenderer.init(eglBase.eglBaseContext, null)
		fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL)
		pipRenderer1.setZOrderMediaOverlay(true)
		pipRenderer1.setEnableHardwareScaler(true /* enabled */)
		pipRenderer2.setZOrderMediaOverlay(true)
		pipRenderer2.setEnableHardwareScaler(true /* enabled */)
		fullscreenRenderer.setEnableHardwareScaler(false /* enabled */)
		// Start with local feed in fullscreen and swap it to the pip when the call is connected.
		setSwappedFeeds(0)

		// Check for mandatory permissions.
		for (permission in MANDATORY_PERMISSIONS) {
			if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				logAndToast("Permission $permission is not granted")
				setResult(RESULT_CANCELED)
				finish()
				return
			}
		}
		val roomUri = intent.data
		if (roomUri == null) {
			logAndToast(getString(R.string.missing_url))
			Log.e(TAG, "Didn't get any URL in intent!")
			setResult(RESULT_CANCELED)
			finish()
			return
		}

		// Get Intent parameters.
		val roomId = intent.getLongExtra(EXTRA_ROOMID, 0)
		if (DEBUG) Log.d(TAG, "Room ID: $roomId")
		if (roomId == 0L) {
			logAndToast(getString(R.string.missing_url))
			Log.e(TAG, "Incorrect room ID in intent!")
			setResult(RESULT_CANCELED)
			finish()
			return
		}
		val loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false)
		val tracing = intent.getBooleanExtra(EXTRA_TRACING, false)
		var videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0)
		var videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0)
		screenCaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false)
		// If capturing format is not specified for screen capture, use screen resolution.
		if (screenCaptureEnabled && videoWidth == 0 && videoHeight == 0) {
			val displayMetrics = displayMetrics
			videoWidth = displayMetrics.widthPixels
			videoHeight = displayMetrics.heightPixels
		}
		peerConnectionParameters = PeerConnectionParameters.Builder().apply {
			setVideoCallEnabled(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true))
			setLoopback(loopback)
			setTracing(tracing)
			setVideoWidth(videoWidth)
			setVideoHeight(videoHeight)
			setVideoFps(intent.getIntExtra(EXTRA_VIDEO_FPS, 0))
			setVideoMaxBitrate(intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0))
			setVideoCodec(intent.getStringExtra(EXTRA_VIDEOCODEC))
			setVideoCodecHwAcceleration(intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true))
			setVideoFlexfecEnabled(intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false))
			setAudioSource(intent.getIntExtra(EXTRA_AUDIO_SOURCE, MediaRecorder.AudioSource.VOICE_RECOGNITION))
			setAudioFormat(intent.getIntExtra(EXTRA_AUDIO_FORMAT, AudioFormat.ENCODING_PCM_16BIT))
			setAudioStartBitrate(intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0))
			setAudioCodec(intent.getStringExtra(EXTRA_AUDIOCODEC))
			setNoAudioProcessing(intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false))
			setAecDump(intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false))
			setSaveInputAudioToFile(intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false))
			setUseOpenSLES(intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false))
			setDisableBuiltInAEC(intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false))
			setDisableBuiltInAGC(intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false))
			setDisableBuiltInNS(intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false))
			setDisableWebRtcAGCAndHPF(intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false))
			setEnableRtcEventLog(intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false))
			if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
				setDataChannelParameters(
					DataChannelParameters.Builder()
					.apply {
						setOrdered(intent.getBooleanExtra(EXTRA_ORDERED, true))
						setMaxRetransmitTimeMs(intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1))
						setMaxRetransmits(intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1))
						setProtocol(intent.getStringExtra(EXTRA_PROTOCOL))
						setNegotiated(intent.getBooleanExtra(EXTRA_NEGOTIATED, false))
						setId(intent.getIntExtra(EXTRA_ID, -1))
					}.run {
						build()
					}
				)
			}
		}.run {
			build()
		}
		commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false)
		val runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0)
		if (DEBUG) Log.d(
			TAG,
			"VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'"
		)
		val options = PeerConnectionFactory.Options()
		if (loopback) {
			options.networkIgnoreMask = 0
		}

		// Create connection parameters.
		val urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS)
		val userName = intent.getStringExtra(EXTRA_USER_NAME)
		val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)
		roomConnectionParameters = RoomConnectionParameters.Builder()
			.apply {
				setRoomUrl(roomUri.toString())
				setApiName("janus")
				setRoomId(roomId)
				setLoopback(loopback)
				setUrlParameters(urlParameters)
				setUserName(userName)
				setDisplayName(displayName)
			}.run {
				build()
			}
		janusClient = JanusVideoRoomClient(
			applicationContext,
			eglBase, peerConnectionParameters!!, roomConnectionParameters!!,
			mJanusCallback
		)
		janusClient!!.createPeerConnectionFactory(options)

		// Create CPU monitor
		if (isSupported) {
			cpuMonitor = CpuMonitor(this)
			hudFragment.setCpuMonitor(cpuMonitor)
		}

		// Send intent arguments to fragments.
		callFragment.arguments = intent.extras
		hudFragment.arguments = intent.extras
		// Activate call and HUD fragments and start the call.
		val ft = supportFragmentManager.beginTransaction()
		ft.add(R.id.call_fragment_container, callFragment)
		ft.add(R.id.hud_fragment_container, hudFragment)
		ft.commit()

		// For command line execution run connection for <runTimeMs> and exit.
		if (commandLineRun && runTimeMs > 0) {
			lifecycleScope.launch {
				delay(runTimeMs.toLong())
				disconnect()
			}
		}
		if (screenCaptureEnabled) {
			startScreenCapture()
		} else {
			startCall()
		}
	}

	private val displayMetrics: DisplayMetrics
		get() {
			val displayMetrics = DisplayMetrics()
			val windowManager = application.getSystemService(WINDOW_SERVICE) as WindowManager
			windowManager.defaultDisplay.getRealMetrics(displayMetrics)
			return displayMetrics
		}

	private fun startScreenCapture() {
		if (DEBUG) Log.v(TAG, "startScreenCapture:")
		val mediaProjectionManager = application.getSystemService(
			MEDIA_PROJECTION_SERVICE
		) as MediaProjectionManager
		startActivityForResult(
			mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE
		)
	}

	@Deprecated("Deprecated in Java")
	public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (DEBUG) Log.v(TAG, "onActivityResult:")
		if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) return
		mediaProjectionPermissionResultCode = resultCode
		mediaProjectionPermissionResultData = data
		startCall()
	}

	private fun useCamera2(): Boolean {
		return (Camera2Enumerator.isSupported(this)
			&& intent.getBooleanExtra(EXTRA_CAMERA2, true))
	}

	private fun captureToTexture(): Boolean {
		return intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false)
	}

	private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
		val deviceNames = enumerator.deviceNames
		if (DEBUG) Log.v(TAG, "createCameraCapturer:")
		// First, try to find front facing camera
		Logging.d(TAG, "Looking for front facing cameras.")
		for (deviceName in deviceNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating front facing camera capturer.")
				val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
				if (videoCapturer != null) {
					return videoCapturer
				}
			}
		}

		// Front facing camera not found, try something else
		Logging.d(TAG, "Looking for other cameras.")
		for (deviceName in deviceNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating other camera capturer.")
				val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
				if (videoCapturer != null) {
					return videoCapturer
				}
			}
		}
		return null
	}

	private fun createScreenCapturer(): VideoCapturer? {
		if (DEBUG) Log.v(TAG, "createScreenCapturer:")
		if (mediaProjectionPermissionResultCode != RESULT_OK) {
			reportError("User didn't give permission to capture the screen.")
			return null
		}
		return ScreenCapturerAndroid(
			mediaProjectionPermissionResultData, object : MediaProjection.Callback() {
				override fun onStop() {
					reportError("User revoked permission to capture the screen.")
				}
			})
	}

	// Activity interfaces
	public override fun onStop() {
		if (DEBUG) Log.v(TAG, "onStop:")
		activityRunning = false
		// Don't stop the video when using screen capture to allow user to show other apps to the remote
		// end.
		if (janusClient != null && !screenCaptureEnabled) {
			janusClient!!.stopVideoSource()
		}
		if (cpuMonitor != null) {
			cpuMonitor!!.pause()
		}
		super.onStop()
	}

	public override fun onStart() {
		super.onStart()
		if (DEBUG) Log.v(TAG, "onStart:")
		activityRunning = true
		// Video is not paused for screen capture. See onPause.
		if (janusClient != null && !screenCaptureEnabled) {
			janusClient!!.startVideoSource()
		}
		if (cpuMonitor != null) {
			cpuMonitor!!.resume()
		}
	}

	override fun onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:")
		Thread.setDefaultUncaughtExceptionHandler(null)
		disconnect()
		if (logToast != null) {
			logToast!!.cancel()
		}
		activityRunning = false
		super.onDestroy()
	}

	// com.serenegiant.janusrtcandroid.CallFragment.OnCallEvents interface implementation.
	override fun onCallHangUp() {
		if (DEBUG) Log.v(TAG, "onCallHangUp:")
		disconnect()
	}

	override fun onCameraSwitch() {
		if (DEBUG) Log.v(TAG, "onCameraSwitch:")
		if (janusClient != null) {
			janusClient!!.switchCamera()
		}
	}

	override fun onVideoScalingSwitch(scalingType: ScalingType?) {
		if (DEBUG) Log.v(TAG, "onVideoScalingSwitch:$scalingType")
		fullscreenRenderer.setScalingType(scalingType)
	}

	override fun onCaptureFormatChange(width: Int, height: Int, framerate: Int) {
		if (DEBUG) Log.v(TAG, "onCaptureFormatChange:")
		if (janusClient != null) {
			janusClient!!.changeCaptureFormat(width, height, framerate)
		}
	}

	override fun onToggleMic(): Boolean {
		if (janusClient != null) {
			micEnabled = !micEnabled
			janusClient!!.setAudioEnabled(micEnabled)
		}
		return micEnabled
	}

	// Helper functions.
	private fun toggleCallControlFragmentVisibility() {
		if (DEBUG) Log.v(TAG, "toggleCallControlFragmentVisibility:")
		if (!iceConnected || !callFragment.isAdded) {
			return
		}
		// Show/hide call control fragment
		callControlFragmentVisible = !callControlFragmentVisible
		val ft = supportFragmentManager.beginTransaction()
		if (callControlFragmentVisible) {
			ft.show(callFragment)
			ft.show(hudFragment)
		} else {
			ft.hide(callFragment)
			ft.hide(hudFragment)
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
		ft.commit()
	}

	private fun startCall() {
		if (DEBUG) Log.v(TAG, "startCall:janusClient=$janusClient")
		if (janusClient == null) {
			Log.e(TAG, "AppRTC client is not allocated for a call.")
			return
		}
		callStartedTimeMs = System.currentTimeMillis()

		// Start room connection.
		logAndToast(getString(R.string.connecting_to, roomConnectionParameters!!.roomUrl))
		janusClient!!.connectToRoom(roomConnectionParameters!!)

		// Create and audio manager that will take care of audio routing,
		// audio modes, audio device enumeration etc.
		audioManager = AppRTCAudioManager2.create(applicationContext)
		// Store existing audio settings and change audio mode to
		// MODE_IN_COMMUNICATION for best possible VoIP performance.
		if (DEBUG) Log.d(TAG, "Starting the audio manager...")
		audioManager!!.start { audioDevice, availableAudioDevices ->
			// This method will be called each time the number of available audio
			// devices has changed.
			onAudioManagerDevicesChanged(audioDevice, availableAudioDevices)
		}
	}

	// Should be called from UI thread
	private fun callConnected() {
		if (DEBUG) Log.v(TAG, "callConnected:")
		val delta = System.currentTimeMillis() - callStartedTimeMs
		Log.i(TAG, "Call connected: delay=" + delta + "ms")
		if (janusClient == null || isError) {
			Log.w(TAG, "Call is connected in closed or error state")
			return
		}
		// Enable statistics callback.
		janusClient!!.enableStatsEvents(STAT_ENABLED, STAT_CALLBACK_PERIOD)
		setSwappedFeeds(1)
	}

	// This method is called when the audio manager reports audio device change,
	// e.g. from wired headset to speakerphone.
	private fun onAudioManagerDevicesChanged(
		device: IAppRTCAudioManager.AudioDevice,
		availableDevices: Set<IAppRTCAudioManager.AudioDevice>
	) {
		if (DEBUG) Log.d(
			TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
				+ "selected: " + device
		)
		// TODO(henrika): add callback handler.
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private fun disconnect() {
		if (DEBUG) Log.v(TAG, "disconnect:")
		activityRunning = false
		remoteProxyRenderer1.setTarget(null)
		remoteProxyRenderer2.setTarget(null)
		localProxyVideoSink.setTarget(null)
		pipRenderer1.release()
		pipRenderer2.release()
		if (videoFileRenderer != null) {
			videoFileRenderer!!.release()
			videoFileRenderer = null
		}
		fullscreenRenderer.release()
		if (janusClient != null) {
			janusClient!!.disconnectFromRoom()
			janusClient = null
		}
		if (audioManager != null) {
			audioManager!!.stop()
			audioManager = null
		}
		if (iceConnected && !isError) {
			setResult(RESULT_OK)
		} else {
			setResult(RESULT_CANCELED)
		}
		finish()
	}

	private fun disconnectWithErrorMessage(errorMessage: String?) {
		if (DEBUG) Log.v(TAG, "disconnectWithErrorMessage:$errorMessage")
		if (commandLineRun || !activityRunning) {
			Log.e(TAG, "Critical error: $errorMessage")
			disconnect()
		} else {
			AlertDialog.Builder(this)
				.setTitle(getText(R.string.channel_error_title))
				.setMessage(errorMessage)
				.setCancelable(false)
				.setNeutralButton(
					R.string.ok
				) { dialog, _ ->
					dialog.cancel()
					disconnect()
				}
				.create()
				.show()
		}
	}

	// Log |msg| and Toast about it.
	private fun logAndToast(msg: String) {
		if (DEBUG) Log.d(TAG, msg)
		if (logToast != null) {
			logToast!!.cancel()
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
		logToast!!.show()
	}

	private fun reportError(description: String?) {
		lifecycleScope.launch {
			if (!isError) {
				isError = true
				disconnectWithErrorMessage(description)
			}
		}
	}

	private fun createVideoCapturer(): VideoCapturer? {
		if (DEBUG) Log.v(TAG, "createVideoCapturer:")
		val videoCapturer: VideoCapturer?
		val videoFileAsCamera = intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA)
		videoCapturer = if (videoFileAsCamera != null) {
			try {
				FileVideoCapturer(videoFileAsCamera)
			} catch (e: IOException) {
				reportError("Failed to open video file for emulated camera")
				return null
			}
		} else if (screenCaptureEnabled) {
			return createScreenCapturer()
		} else if (useCamera2()) {
			if (!captureToTexture()) {
				reportError(getString(R.string.camera2_texture_only_error))
				return null
			}
			Logging.d(TAG, "Creating capturer using camera2 API.")
			createCameraCapturer(Camera2Enumerator(this))
		} else {
			Logging.d(TAG, "Creating capturer using camera1 API.")
			createCameraCapturer(Camera1Enumerator(captureToTexture()))
		}
		if (videoCapturer == null) {
			reportError("Failed to open camera")
			return null
		}
		return videoCapturer
	}

	/**
	 * 映像表示を切り替え
	 * @param feedIx メイン表示 0: 自分, 1: リモート1, 2: リモート2
	 */
	private fun setSwappedFeeds(feedIx: Int) {
		if (DEBUG) Log.v(TAG, "setSwappedFeeds:$feedIx")
		if (currentFullIx != feedIx) {
			currentFullIx = feedIx
			when (feedIx) {
				1 -> {
					localProxyVideoSink.setTarget(pipRenderer1)
					remoteProxyRenderer1.setTarget(fullscreenRenderer)
					remoteProxyRenderer2.setTarget(pipRenderer2)
					fullscreenRenderer.setMirror(false)
					pipRenderer1.setMirror(true)
					pipRenderer2.setMirror(false)
				}
				2 -> {
					localProxyVideoSink.setTarget(pipRenderer2)
					remoteProxyRenderer1.setTarget(pipRenderer1)
					remoteProxyRenderer2.setTarget(fullscreenRenderer)
					fullscreenRenderer.setMirror(true)
					pipRenderer1.setMirror(false)
					pipRenderer2.setMirror(true)
				}
				0 -> {
					localProxyVideoSink.setTarget(fullscreenRenderer)
					remoteProxyRenderer1.setTarget(pipRenderer1)
					remoteProxyRenderer2.setTarget(pipRenderer2)
					fullscreenRenderer.setMirror(true)
					pipRenderer1.setMirror(false)
					pipRenderer2.setMirror(false)
				}
				else -> {}
			}
		}
	}

	// -----Implementation of com.serenegiant.janus.JanusClient.AppRTCSignalingEvents ---------------
	// All callbacks are invoked from websocket signaling looper thread and
	// are routed to UI thread.
	private fun onConnectedToRoomInternal(initiator: Boolean) {
		if (DEBUG) Log.v(TAG, "onConnectedToRoomInternal:")
		val delta = System.currentTimeMillis() - callStartedTimeMs
		logAndToast("Creating peer connection, delay=" + delta + "ms")
		if (initiator) {
			logAndToast("Creating OFFER...")
		} else {
			logAndToast("Creating ANSWER...")
		}
	}

	private val mJanusCallback: JanusCallback = object : JanusCallback {
		override fun setupOkHttp(
			builder: OkHttpClient.Builder,
			isLongPoll: Boolean,
			connectionTimeout: Long,
			readTimeoutMs: Long, writeTimeoutMs: Long): OkHttpClient.Builder {
			if (DEBUG) Log.v(TAG, "setupOkHttp:")
			return builder
		}

		override fun setupRetrofit(builder: Retrofit.Builder): Retrofit.Builder {
			if (DEBUG) Log.v(TAG, "setupRetrofit:")
			return builder
		}

		override fun onConnectServer(client: JanusVideoRoomClient) {
			if (DEBUG) Log.v(TAG, "onConnectServer:")
			lifecycleScope.launch {
				var videoCapturer: VideoCapturer? = null
				if (peerConnectionParameters!!.videoCallEnabled) {
					videoCapturer = createVideoCapturer()
				}
				client.createPeerConnection(
					localProxyVideoSink, videoCapturer
				)
			}
		}

		override fun getIceServers(
			client: JanusVideoRoomClient
		): List<IceServer> {
			return ArrayList()
		}

		override fun onConnectedToRoom(initiator: Boolean, room: RoomEvent.Data) {
			if (DEBUG) Log.v(TAG, "onConnectedToRoom:$room")
			lifecycleScope.launch { onConnectedToRoomInternal(initiator) }
		}

		override fun onDisconnected() {
			if (DEBUG) Log.v(TAG, "onDisconnected:")
			finish()
		}

		override fun onIceConnected() {
			if (DEBUG) Log.v(TAG, "onIceConnected:")
			val delta = System.currentTimeMillis() - callStartedTimeMs
			lifecycleScope.launch {
				logAndToast("ICE connected, delay=" + delta + "ms")
				iceConnected = true
				callConnected()
			}
		}

		override fun onIceDisconnected() {
			if (DEBUG) Log.v(TAG, "onIceDisconnected:")
			lifecycleScope.launch {
				logAndToast("ICE disconnected")
				iceConnected = false
				disconnect()
			}
		}

		override fun onNewPublisher(info: PublisherInfo): Boolean {
			return true // true: 受け入れる(通話する)
		}

		override fun onEnter(info: PublisherInfo) {
			if (DEBUG) Log.v(TAG, "onEnter:$info,publishers=${janusClient?.publishers},subscribers=${janusClient?.subscribers}")
			mNumUsers++
			lifecycleScope.launch(Dispatchers.IO) {
				delay(1000L)
				val r = janusClient?.configure(ConfigPublisher(2000000))
				if (DEBUG) Log.v(TAG, "onEnter:configure=$r")
			}
		}

		override fun onLeave(info: PublisherInfo, numUsers: Int) {
			mNumUsers--
			if (DEBUG) Log.v(TAG, "onLeave:$info,numUsers=$numUsers/$mNumUsers")
			if (mNumUsers <= 0) {
				lifecycleScope.launch(Dispatchers.IO) {
					val r = janusClient?.configure(ConfigPublisher(1000000))
					if (DEBUG) Log.v(TAG, "onLeave:configure=$r")
				}
			}
		}

		override fun getRemoteVideoSink(info: PublisherInfo): List<VideoSink> {
			if (DEBUG) Log.v(TAG, "getRemoteVideoSink:$info")
			return if (mNumUsers <= remoteRenderers.size) {
				if (DEBUG) Log.v(
					TAG,
					"getRemoteVideoSink:add remote video sink"
				)
				remoteRenderers.subList(mNumUsers - 1, mNumUsers)
			} else {
				if (DEBUG) Log.v(
					TAG,
					"getRemoteVideoSink:out of range"
				)
				emptyList()
			}
		}

		override fun onRemoteDescription(sdp: SessionDescription) {
			if (DEBUG) Log.v(TAG, "onRemoteDescription:")
			val delta = System.currentTimeMillis() - callStartedTimeMs
			lifecycleScope.launch {
				if (janusClient == null) {
					Log.e(TAG, "Received remote SDP for non-initialized peer connection.")
					return@launch
				}
				logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms")
			}
		}

		override fun onRemoteIceCandidate(candidate: IceCandidate) {
			if (DEBUG) Log.v(TAG, "onRemoteIceCandidate:")
			lifecycleScope.launch {
				if (janusClient == null) {
					Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.")
				}
			}
		}

		override fun onRemoteIceCandidatesRemoved(candidates: Array<IceCandidate>) {
			if (DEBUG) Log.v(TAG, "onRemoteIceCandidatesRemoved:")
			lifecycleScope.launch {
				if (janusClient == null) {
					Log.e(
						TAG,
						"Received ICE candidate removals for a non-initialized peer connection."
					)
				}
			}
		}

		override fun onChannelClose() {
			if (DEBUG) Log.v(TAG, "onChannelClose:")
			if (mNumUsers <= 0) {
				// パブリッシャーが全員退室したときは自分も退室する
				lifecycleScope.launch {
					logAndToast("Remote end hung up; dropping PeerConnection")
					disconnect()
				}
			}
		}

		override fun onEvent(event: JSONObject) {
			if (DEBUG) Log.v(TAG, "onEvent:$event")
		}

		override fun onPeerConnectionStatsReady(
			isPublisher: Boolean,
			report: RTCStatsReport
		) {
			Log.d(TAG, "onPeerConnectionStatsReady:isPublisher=$isPublisher,stat=$report")
		}

		override fun onChannelError(t: Throwable) {
			reportError(t.message)
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = CallActivity::class.java.simpleName

		const val EXTRA_ROOMID = "org.appspot.apprtc.ROOMID"
		const val EXTRA_URLPARAMETERS = "org.appspot.apprtc.URLPARAMETERS"
		const val EXTRA_USER_NAME = "org.appspot.apprtc.EXTRA_USER_NAME"
		const val EXTRA_DISPLAY_NAME = "org.appspot.apprtc.EXTRA_DISPLAY_NAME"
		const val EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK"
		const val EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL"
		const val EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE"
		const val EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2"
		const val EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH"
		const val EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT"
		const val EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS"
		const val EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
			"org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER"
		const val EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE"
		const val EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC"
		const val EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC"
		const val EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE"
		const val EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC"
		const val EXTRA_AUDIO_SOURCE = "org.appspot.apprtc.AUDIO_SOURCE"
		const val EXTRA_AUDIO_FORMAT = "org.appspot.apprtc.AUDIO_FORMAT"
		const val EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE"
		const val EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC"
		const val EXTRA_NOAUDIOPROCESSING_ENABLED = "org.appspot.apprtc.NOAUDIOPROCESSING"
		const val EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP"
		const val EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED =
			"org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE"
		const val EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES"
		const val EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC"
		const val EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC"
		const val EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS"
		const val EXTRA_DISABLE_WEBRTC_AGC_AND_HPF =
			"org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL"
		const val EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD"
		const val EXTRA_TRACING = "org.appspot.apprtc.TRACING"
		const val EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE"
		const val EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME"
		const val EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA"
		const val EXTRA_SAVE_REMOTE_VIDEO_TO_FILE = "org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE"
		const val EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
			"org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH"
		const val EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
			"org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT"
		const val EXTRA_USE_VALUES_FROM_INTENT = "org.appspot.apprtc.USE_VALUES_FROM_INTENT"
		const val EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED"
		const val EXTRA_ORDERED = "org.appspot.apprtc.ORDERED"
		const val EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS"
		const val EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS"
		const val EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL"
		const val EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED"
		const val EXTRA_ID = "org.appspot.apprtc.ID"
		const val EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG"
		private const val CAPTURE_PERMISSION_REQUEST_CODE = 1

		// List of mandatory application permissions.
		private val MANDATORY_PERMISSIONS = arrayOf(
			"android.permission.MODIFY_AUDIO_SETTINGS",
			"android.permission.RECORD_AUDIO",
			"android.permission.INTERNET"
		)
		// whether or not to get peer connection statistics
		private const val STAT_ENABLED = false
		// Peer connection statistics callback period in ms.
		private const val STAT_CALLBACK_PERIOD = 1000
		private var mediaProjectionPermissionResultData: Intent? = null
		private var mediaProjectionPermissionResultCode = 0

		private val systemUiVisibility: Int
			get() = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
				or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
	}
}
