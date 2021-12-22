package com.serenegiant.janusrtcandroid;
/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.serenegiant.janus.JanusCallback;
import com.serenegiant.janus.JanusRTCClient;

import org.appspot.apprtc.AppRTCAudioManager;
import org.appspot.apprtc.AppRTCAudioManager.AudioDevice;
import org.appspot.apprtc.AppRTCAudioManager.AudioManagerEvents;
import com.serenegiant.janus.JanusClient;
import com.serenegiant.janus.response.PublisherInfo;

import org.appspot.apprtc.RoomConnectionParameters;
import org.appspot.apprtc.DataChannelParameters;
import org.appspot.apprtc.PeerConnectionParameters;
import org.appspot.apprtc.UnhandledExceptionHandler;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends BaseActivity
	implements CallFragment.OnCallEvents {

	private static final boolean DEBUG = true;    // set false on production
	private static final String TAG = "CallActivity";

	public static final String EXTRA_ROOMID = "org.appspot.apprtc.ROOMID";
	public static final String EXTRA_URLPARAMETERS = "org.appspot.apprtc.URLPARAMETERS";
	public static final String EXTRA_USER_NAME = "org.appspot.apprtc.EXTRA_USER_NAME";
	public static final String EXTRA_DISPLAY_NAME = "org.appspot.apprtc.EXTRA_DISPLAY_NAME";
	public static final String EXTRA_LOOPBACK = "org.appspot.apprtc.LOOPBACK";
	public static final String EXTRA_VIDEO_CALL = "org.appspot.apprtc.VIDEO_CALL";
	public static final String EXTRA_SCREENCAPTURE = "org.appspot.apprtc.SCREENCAPTURE";
	public static final String EXTRA_CAMERA2 = "org.appspot.apprtc.CAMERA2";
	public static final String EXTRA_VIDEO_WIDTH = "org.appspot.apprtc.VIDEO_WIDTH";
	public static final String EXTRA_VIDEO_HEIGHT = "org.appspot.apprtc.VIDEO_HEIGHT";
	public static final String EXTRA_VIDEO_FPS = "org.appspot.apprtc.VIDEO_FPS";
	public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED =
		"org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
	public static final String EXTRA_VIDEO_BITRATE = "org.appspot.apprtc.VIDEO_BITRATE";
	public static final String EXTRA_VIDEOCODEC = "org.appspot.apprtc.VIDEOCODEC";
	public static final String EXTRA_HWCODEC_ENABLED = "org.appspot.apprtc.HWCODEC";
	public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "org.appspot.apprtc.CAPTURETOTEXTURE";
	public static final String EXTRA_FLEXFEC_ENABLED = "org.appspot.apprtc.FLEXFEC";
	public static final String EXTRA_AUDIO_BITRATE = "org.appspot.apprtc.AUDIO_BITRATE";
	public static final String EXTRA_AUDIOCODEC = "org.appspot.apprtc.AUDIOCODEC";
	public static final String EXTRA_NOAUDIOPROCESSING_ENABLED =
		"org.appspot.apprtc.NOAUDIOPROCESSING";
	public static final String EXTRA_AECDUMP_ENABLED = "org.appspot.apprtc.AECDUMP";
	public static final String EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED =
		"org.appspot.apprtc.SAVE_INPUT_AUDIO_TO_FILE";
	public static final String EXTRA_OPENSLES_ENABLED = "org.appspot.apprtc.OPENSLES";
	public static final String EXTRA_DISABLE_BUILT_IN_AEC = "org.appspot.apprtc.DISABLE_BUILT_IN_AEC";
	public static final String EXTRA_DISABLE_BUILT_IN_AGC = "org.appspot.apprtc.DISABLE_BUILT_IN_AGC";
	public static final String EXTRA_DISABLE_BUILT_IN_NS = "org.appspot.apprtc.DISABLE_BUILT_IN_NS";
	public static final String EXTRA_DISABLE_WEBRTC_AGC_AND_HPF =
		"org.appspot.apprtc.DISABLE_WEBRTC_GAIN_CONTROL";
	public static final String EXTRA_DISPLAY_HUD = "org.appspot.apprtc.DISPLAY_HUD";
	public static final String EXTRA_TRACING = "org.appspot.apprtc.TRACING";
	public static final String EXTRA_CMDLINE = "org.appspot.apprtc.CMDLINE";
	public static final String EXTRA_RUNTIME = "org.appspot.apprtc.RUNTIME";
	public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "org.appspot.apprtc.VIDEO_FILE_AS_CAMERA";
	public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE =
		"org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE";
	public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH =
		"org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
	public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT =
		"org.appspot.apprtc.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
	public static final String EXTRA_USE_VALUES_FROM_INTENT =
		"org.appspot.apprtc.USE_VALUES_FROM_INTENT";
	public static final String EXTRA_DATA_CHANNEL_ENABLED = "org.appspot.apprtc.DATA_CHANNEL_ENABLED";
	public static final String EXTRA_ORDERED = "org.appspot.apprtc.ORDERED";
	public static final String EXTRA_MAX_RETRANSMITS_MS = "org.appspot.apprtc.MAX_RETRANSMITS_MS";
	public static final String EXTRA_MAX_RETRANSMITS = "org.appspot.apprtc.MAX_RETRANSMITS";
	public static final String EXTRA_PROTOCOL = "org.appspot.apprtc.PROTOCOL";
	public static final String EXTRA_NEGOTIATED = "org.appspot.apprtc.NEGOTIATED";
	public static final String EXTRA_ID = "org.appspot.apprtc.ID";
	public static final String EXTRA_ENABLE_RTCEVENTLOG = "org.appspot.apprtc.ENABLE_RTCEVENTLOG";
	public static final String EXTRA_USE_LEGACY_AUDIO_DEVICE =
		"org.appspot.apprtc.USE_LEGACY_AUDIO_DEVICE";

	private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;

	// List of mandatory application permissions.
	private static final String[] MANDATORY_PERMISSIONS = {
		"android.permission.MODIFY_AUDIO_SETTINGS",
		"android.permission.RECORD_AUDIO",
		"android.permission.INTERNET"};

	// Peer connection statistics callback period in ms.
	private static final int STAT_CALLBACK_PERIOD = 1000;

	private static class ProxyVideoSink implements VideoSink {
		private VideoSink target;

		@Override
		synchronized public void onFrame(final VideoFrame frame) {
			if (target == null) {
				if (DEBUG) Logging.d(TAG, "ProxyVideoSink: Dropping frame in proxy because target is null.");
				return;
			}

			target.onFrame(frame);
		}

		synchronized public void setTarget(final VideoSink target) {
			if (DEBUG) Logging.d(TAG, "ProxyVideoSink#setTarget:" + target);
			this.target = target;
		}
	}

	private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
	private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
	@Nullable
	private JanusClient janusClient;
	@Nullable
	private AppRTCAudioManager audioManager = null;
	@Nullable
	private SurfaceViewRenderer pipRenderer;
	@Nullable
	private SurfaceViewRenderer fullscreenRenderer;
	@Nullable
	private VideoFileRenderer videoFileRenderer;
	private final List<VideoSink> remoteRenderers = new ArrayList<>();
	private Toast logToast;
	private boolean commandLineRun;
	private boolean activityRunning;
	private RoomConnectionParameters roomConnectionParameters;
	@Nullable
	private PeerConnectionParameters peerConnectionParameters;
	private boolean iceConnected;
	private boolean isError;
	private boolean callControlFragmentVisible = true;
	private long callStartedTimeMs = 0;
	private boolean micEnabled = true;
	private boolean screenCaptureEnabled = false;
	private static Intent mediaProjectionPermissionResultData;
	private static int mediaProjectionPermissionResultCode;
	// True if local view is in the fullscreen renderer.
	private boolean isSwappedFeeds;

	// Controls
	private CallFragment callFragment;
	private HudFragment hudFragment;
	private CpuMonitor cpuMonitor;

	@Override
	// TODO(bugs.webrtc.org/8580): LayoutParams.FLAG_TURN_SCREEN_ON and
	// LayoutParams.FLAG_SHOW_WHEN_LOCKED are deprecated.
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (DEBUG) Log.v(TAG, "onCreate:");
		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

		// Set window styles for fullscreen-window size. Needs to be done before
		// adding content.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
			| LayoutParams.FLAG_SHOW_WHEN_LOCKED | LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
		setContentView(R.layout.activity_call);

		iceConnected = false;

		// Create UI controls.
		pipRenderer = findViewById(R.id.pip_video_view);
		fullscreenRenderer = findViewById(R.id.fullscreen_video_view);
		callFragment = new CallFragment();
		hudFragment = new HudFragment();

		// Swap feeds on pip view click.
		pipRenderer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setSwappedFeeds(!isSwappedFeeds);
			}
		});

		// Show/hide call control fragment on view click.
		fullscreenRenderer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				toggleCallControlFragmentVisibility();
			}
		});

		remoteRenderers.add(remoteProxyRenderer);

		final Intent intent = getIntent();
		final EglBase eglBase = EglBase.create();

		// Create video renderers.
		pipRenderer.init(eglBase.getEglBaseContext(), null);
		pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
		final String saveRemoteVideoToFile = intent.getStringExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);

		// When saveRemoteVideoToFile is set we save the video from the remote to a file.
		if (saveRemoteVideoToFile != null) {
			int videoOutWidth = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
			int videoOutHeight = intent.getIntExtra(EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
			try {
				videoFileRenderer = new VideoFileRenderer(
					saveRemoteVideoToFile, videoOutWidth, videoOutHeight, eglBase.getEglBaseContext());
				remoteRenderers.add(videoFileRenderer);
			} catch (IOException e) {
				throw new RuntimeException(
					"Failed to open video file for output: " + saveRemoteVideoToFile, e);
			}
		}
		fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
		fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

		pipRenderer.setZOrderMediaOverlay(true);
		pipRenderer.setEnableHardwareScaler(true /* enabled */);
		fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
		// Start with local feed in fullscreen and swap it to the pip when the call is connected.
		setSwappedFeeds(true /* isSwappedFeeds */);

		// Check for mandatory permissions.
		for (final String permission : MANDATORY_PERMISSIONS) {
			if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				logAndToast("Permission " + permission + " is not granted");
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
		}

		final Uri roomUri = intent.getData();
		if (roomUri == null) {
			logAndToast(getString(R.string.missing_url));
			Log.e(TAG, "Didn't get any URL in intent!");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		// Get Intent parameters.
		final int roomId = intent.getIntExtra(EXTRA_ROOMID, 0);
		if (DEBUG) Log.d(TAG, "Room ID: " + roomId);
		if (roomId == 0) {
			logAndToast(getString(R.string.missing_url));
			Log.e(TAG, "Incorrect room ID in intent!");
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		final boolean loopback = intent.getBooleanExtra(EXTRA_LOOPBACK, false);
		final boolean tracing = intent.getBooleanExtra(EXTRA_TRACING, false);

		int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
		int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);

		screenCaptureEnabled = intent.getBooleanExtra(EXTRA_SCREENCAPTURE, false);
		// If capturing format is not specified for screen capture, use screen resolution.
		if (screenCaptureEnabled && videoWidth == 0 && videoHeight == 0) {
			final DisplayMetrics displayMetrics = getDisplayMetrics();
			videoWidth = displayMetrics.widthPixels;
			videoHeight = displayMetrics.heightPixels;
		}
		DataChannelParameters dataChannelParameters = null;
		if (intent.getBooleanExtra(EXTRA_DATA_CHANNEL_ENABLED, false)) {
			dataChannelParameters = new DataChannelParameters(intent.getBooleanExtra(EXTRA_ORDERED, true),
				intent.getIntExtra(EXTRA_MAX_RETRANSMITS_MS, -1),
				intent.getIntExtra(EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(EXTRA_PROTOCOL),
				intent.getBooleanExtra(EXTRA_NEGOTIATED, false), intent.getIntExtra(EXTRA_ID, -1));
		}
		peerConnectionParameters =
			new PeerConnectionParameters(intent.getBooleanExtra(EXTRA_VIDEO_CALL, true), loopback,
				tracing, videoWidth, videoHeight, intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
				intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(EXTRA_VIDEOCODEC),
				intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
				intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
				intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(EXTRA_AUDIOCODEC),
				intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
				intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
				intent.getBooleanExtra(EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
				intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
				intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
				intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
				intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
				intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
				intent.getBooleanExtra(EXTRA_ENABLE_RTCEVENTLOG, false),
				intent.getBooleanExtra(EXTRA_USE_LEGACY_AUDIO_DEVICE, false),
				dataChannelParameters);
		commandLineRun = intent.getBooleanExtra(EXTRA_CMDLINE, false);
		int runTimeMs = intent.getIntExtra(EXTRA_RUNTIME, 0);

		if (DEBUG) Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

		final PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		if (loopback) {
			options.networkIgnoreMask = 0;
		}

		// Create connection parameters.
		final String urlParameters = intent.getStringExtra(EXTRA_URLPARAMETERS);
		final String userName = intent.getStringExtra(EXTRA_USER_NAME);
		final String displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME);
		roomConnectionParameters =
			new RoomConnectionParameters(roomUri.toString(),
				"janus", roomId,
				loopback, urlParameters,
				userName, displayName);

		janusClient = new JanusRTCClient(getApplicationContext(),
			eglBase, peerConnectionParameters, roomConnectionParameters,
			mJanusCallback);
		janusClient.createPeerConnectionFactory(options);
		
		// Create CPU monitor
		if (CpuMonitor.isSupported()) {
			cpuMonitor = new CpuMonitor(this);
			hudFragment.setCpuMonitor(cpuMonitor);
		}

		// Send intent arguments to fragments.
		callFragment.setArguments(intent.getExtras());
		hudFragment.setArguments(intent.getExtras());
		// Activate call and HUD fragments and start the call.
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(R.id.call_fragment_container, callFragment);
		ft.add(R.id.hud_fragment_container, hudFragment);
		ft.commit();

		// For command line execution run connection for <runTimeMs> and exit.
		if (commandLineRun && runTimeMs > 0) {
			(new Handler()).postDelayed(new Runnable() {
				@Override
				public void run() {
					disconnect();
				}
			}, runTimeMs);
		}

		if (screenCaptureEnabled) {
			startScreenCapture();
		} else {
			startCall();
		}
	}

	@TargetApi(17)
	private DisplayMetrics getDisplayMetrics() {
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		final WindowManager windowManager =
			(WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
		return displayMetrics;
	}

	@TargetApi(19)
	private static int getSystemUiVisibility() {
		int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		return flags;
	}

	@TargetApi(21)
	private void startScreenCapture() {
		if (DEBUG) Log.v(TAG, "startScreenCapture:");
		MediaProjectionManager mediaProjectionManager =
			(MediaProjectionManager) getApplication().getSystemService(
				Context.MEDIA_PROJECTION_SERVICE);
		startActivityForResult(
			mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (DEBUG) Log.v(TAG, "onActivityResult:");
		if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
			return;
		mediaProjectionPermissionResultCode = resultCode;
		mediaProjectionPermissionResultData = data;
		startCall();
	}

	private boolean useCamera2() {
		return Camera2Enumerator.isSupported(this)
			&& getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
	}

	private boolean captureToTexture() {
		return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
	}

	@Nullable
	private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
		final String[] deviceNames = enumerator.getDeviceNames();

		if (DEBUG) Log.v(TAG, "createCameraCapturer:");
		// First, try to find front facing camera
		Logging.d(TAG, "Looking for front facing cameras.");
		for (String deviceName : deviceNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating front facing camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		// Front facing camera not found, try something else
		Logging.d(TAG, "Looking for other cameras.");
		for (String deviceName : deviceNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				Logging.d(TAG, "Creating other camera capturer.");
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}

		return null;
	}

	@TargetApi(21)
	@Nullable
	private VideoCapturer createScreenCapturer() {
		if (DEBUG) Log.v(TAG, "createScreenCapturer:");
		if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
			reportError("User didn't give permission to capture the screen.");
			return null;
		}
		return new ScreenCapturerAndroid(
			mediaProjectionPermissionResultData, new MediaProjection.Callback() {
			@Override
			public void onStop() {
				reportError("User revoked permission to capture the screen.");
			}
		});
	}

	// Activity interfaces
	@Override
	public void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		activityRunning = false;
		// Don't stop the video when using screen capture to allow user to show other apps to the remote
		// end.
		if (janusClient != null && !screenCaptureEnabled) {
			janusClient.stopVideoSource();
		}
		if (cpuMonitor != null) {
			cpuMonitor.pause();
		}
		super.onStop();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");
		activityRunning = true;
		// Video is not paused for screen capture. See onPause.
		if (janusClient != null && !screenCaptureEnabled) {
			janusClient.startVideoSource();
		}
		if (cpuMonitor != null) {
			cpuMonitor.resume();
		}
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		Thread.setDefaultUncaughtExceptionHandler(null);
		disconnect();
		if (logToast != null) {
			logToast.cancel();
		}
		activityRunning = false;
		super.onDestroy();
	}

	// com.serenegiant.janusrtcandroid.CallFragment.OnCallEvents interface implementation.
	@Override
	public void onCallHangUp() {
		if (DEBUG) Log.v(TAG, "onCallHangUp:");
		disconnect();
	}

	@Override
	public void onCameraSwitch() {
		if (DEBUG) Log.v(TAG, "onCameraSwitch:");
		if (janusClient != null) {
			janusClient.switchCamera();
		}
	}

	@Override
	public void onVideoScalingSwitch(ScalingType scalingType) {
		if (DEBUG) Log.v(TAG, "onVideoScalingSwitch:" + scalingType);
		fullscreenRenderer.setScalingType(scalingType);
	}

	@Override
	public void onCaptureFormatChange(int width, int height, int framerate) {
		if (DEBUG) Log.v(TAG, "onCaptureFormatChange:");
		if (janusClient != null) {
			janusClient.changeCaptureFormat(width, height, framerate);
		}
	}

	@Override
	public boolean onToggleMic() {
		if (janusClient != null) {
			micEnabled = !micEnabled;
			janusClient.setAudioEnabled(micEnabled);
		}
		return micEnabled;
	}

	// Helper functions.
	private void toggleCallControlFragmentVisibility() {
		if (DEBUG) Log.v(TAG, "toggleCallControlFragmentVisibility:");
		if (!iceConnected || !callFragment.isAdded()) {
			return;
		}
		// Show/hide call control fragment
		callControlFragmentVisible = !callControlFragmentVisible;
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		if (callControlFragmentVisible) {
			ft.show(callFragment);
			ft.show(hudFragment);
		} else {
			ft.hide(callFragment);
			ft.hide(hudFragment);
		}
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		ft.commit();
	}

	private void startCall() {
		if (DEBUG) Log.v(TAG, "startCall:janusClient=" + janusClient);
		if (janusClient == null) {
			Log.e(TAG, "AppRTC client is not allocated for a call.");
			return;
		}
		callStartedTimeMs = System.currentTimeMillis();

		// Start room connection.
		logAndToast(getString(R.string.connecting_to, roomConnectionParameters.roomUrl));
		janusClient.connectToRoom(roomConnectionParameters);

		// Create and audio manager that will take care of audio routing,
		// audio modes, audio device enumeration etc.
		audioManager = AppRTCAudioManager.create(getApplicationContext());
		// Store existing audio settings and change audio mode to
		// MODE_IN_COMMUNICATION for best possible VoIP performance.
		if (DEBUG) Log.d(TAG, "Starting the audio manager...");
		audioManager.start(new AudioManagerEvents() {
			// This method will be called each time the number of available audio
			// devices has changed.
			@Override
			public void onAudioDeviceChanged(
				final AudioDevice audioDevice,
				final Set<AudioDevice> availableAudioDevices) {

				onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
			}
		});
	}

	// Should be called from UI thread
	private void callConnected() {
		if (DEBUG) Log.v(TAG, "callConnected:");
		final long delta = System.currentTimeMillis() - callStartedTimeMs;
		Log.i(TAG, "Call connected: delay=" + delta + "ms");
		if (janusClient == null || isError) {
			Log.w(TAG, "Call is connected in closed or error state");
			return;
		}
		// Enable statistics callback.
		janusClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
		setSwappedFeeds(false /* isSwappedFeeds */);
	}

	// This method is called when the audio manager reports audio device change,
	// e.g. from wired headset to speakerphone.
	private void onAudioManagerDevicesChanged(
		final AudioDevice device, final Set<AudioDevice> availableDevices) {
		if (DEBUG) Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
			+ "selected: " + device);
		// TODO(henrika): add callback handler.
	}

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnect() {
		if (DEBUG) Log.v(TAG, "disconnect:");
		activityRunning = false;
		remoteProxyRenderer.setTarget(null);
		localProxyVideoSink.setTarget(null);
		if (pipRenderer != null) {
			pipRenderer.release();
			pipRenderer = null;
		}
		if (videoFileRenderer != null) {
			videoFileRenderer.release();
			videoFileRenderer = null;
		}
		if (fullscreenRenderer != null) {
			fullscreenRenderer.release();
			fullscreenRenderer = null;
		}
		if (janusClient != null) {
			janusClient.disconnectFromRoom();
			janusClient = null;
		}
		if (audioManager != null) {
			audioManager.stop();
			audioManager = null;
		}
		if (iceConnected && !isError) {
			setResult(RESULT_OK);
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	private void disconnectWithErrorMessage(final String errorMessage) {
		if (DEBUG) Log.v(TAG, "disconnectWithErrorMessage:" + errorMessage);
		if (commandLineRun || !activityRunning) {
			Log.e(TAG, "Critical error: " + errorMessage);
			disconnect();
		} else {
			new AlertDialog.Builder(this)
				.setTitle(getText(R.string.channel_error_title))
				.setMessage(errorMessage)
				.setCancelable(false)
				.setNeutralButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
							disconnect();
						}
					})
				.create()
				.show();
		}
	}

	// Log |msg| and Toast about it.
	private void logAndToast(final String msg) {
		if (DEBUG) Log.d(TAG, msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}

	private void reportError(final String description) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!isError) {
					isError = true;
					disconnectWithErrorMessage(description);
				}
			}
		});
	}

	@Nullable
	private VideoCapturer createVideoCapturer() {
		if (DEBUG) Log.v(TAG, "createVideoCapturer:");
		final VideoCapturer videoCapturer;
		String videoFileAsCamera = getIntent().getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
		if (videoFileAsCamera != null) {
			try {
				videoCapturer = new FileVideoCapturer(videoFileAsCamera);
			} catch (IOException e) {
				reportError("Failed to open video file for emulated camera");
				return null;
			}
		} else if (screenCaptureEnabled) {
			return createScreenCapturer();
		} else if (useCamera2()) {
			if (!captureToTexture()) {
				reportError(getString(R.string.camera2_texture_only_error));
				return null;
			}

			Logging.d(TAG, "Creating capturer using camera2 API.");
			videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
		} else {
			Logging.d(TAG, "Creating capturer using camera1 API.");
			videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
		}
		if (videoCapturer == null) {
			reportError("Failed to open camera");
			return null;
		}
		return videoCapturer;
	}

	private void setSwappedFeeds(boolean isSwappedFeeds) {
		if (DEBUG) Log.v(TAG, "setSwappedFeeds:" + isSwappedFeeds);
		this.isSwappedFeeds = isSwappedFeeds;
		localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
		remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
		fullscreenRenderer.setMirror(isSwappedFeeds);
		pipRenderer.setMirror(!isSwappedFeeds);
	}

	// -----Implementation of com.serenegiant.janus.JanusClient.AppRTCSignalingEvents ---------------
	// All callbacks are invoked from websocket signaling looper thread and
	// are routed to UI thread.
	private void onConnectedToRoomInternal(final boolean initiator) {
		if (DEBUG) Log.v(TAG, "onConnectedToRoomInternal:");
		final long delta = System.currentTimeMillis() - callStartedTimeMs;

		logAndToast("Creating peer connection, delay=" + delta + "ms");

		if (initiator) {
			logAndToast("Creating OFFER...");
		} else {
			logAndToast("Creating ANSWER...");
		}
	}

	private final JanusCallback mJanusCallback
		= new JanusCallback() {

		@NonNull
		public OkHttpClient.Builder setupOkHttp(
			@NonNull final OkHttpClient.Builder builder,
			final boolean isLongPoll,
			final long connectionTimeout,
			final long readTimeoutMs, final long writeTimeoutMs) {

			if (DEBUG) Log.v(TAG, "setupOkHttp:");
			return builder;
		}
	
		@NonNull
		public Retrofit.Builder setupRetrofit(@NonNull final Retrofit.Builder builder) {

			if (DEBUG) Log.v(TAG, "setupRetrofit:");
			return builder;
		}
	
		@Override
		public void onConnectServer(@NonNull final JanusRTCClient client) {
			if (DEBUG) Log.v(TAG, "onConnectServer:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					VideoCapturer videoCapturer = null;
					if (peerConnectionParameters.videoCallEnabled) {
						videoCapturer = createVideoCapturer();
					}
					client.createPeerConnection(
						localProxyVideoSink, remoteRenderers, videoCapturer);
				}
			});
		}
		
		@Override
		public List<PeerConnection.IceServer> getIceServers(
			@NonNull final JanusRTCClient client) {

			return new ArrayList<PeerConnection.IceServer>();
		}

		@Override
		public void onConnectedToRoom(final boolean initiator) {
			if (DEBUG) Log.v(TAG, "onConnectedToRoom:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onConnectedToRoomInternal(initiator);
				}
			});
		}

		@Override
		public void onDisconnected() {
			if (DEBUG) Log.v(TAG, "onDisconnected:");
			finish();
		}
	
		@Override
		public void onIceConnected() {
			if (DEBUG) Log.v(TAG, "onIceConnected:");
			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("ICE connected, delay=" + delta + "ms");
					iceConnected = true;
					callConnected();
				}
			});
		}
	
		@Override
		public void onIceDisconnected() {
			if (DEBUG) Log.v(TAG, "onIceDisconnected:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("ICE disconnected");
					iceConnected = false;
					disconnect();
				}
			});
		}
		
		@Override
		public void onEnter(final PublisherInfo info) {
			if (DEBUG) Log.v(TAG, "onEnter:" + info);
		}
		
		@Override
		public void onLeave(final PublisherInfo info, final int numUsers) {
			if (DEBUG) Log.v(TAG, "onLeave:" + info + ",numUsers=" + numUsers);
		}
		
		@Override
		public void onRemoteDescription(final SessionDescription sdp) {
			if (DEBUG) Log.v(TAG, "onRemoteDescription:");
			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (janusClient == null) {
						Log.e(TAG, "Received remote SDP for non-initialized peer connection.");
						return;
					}
					logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
				}
			});
		}
	
		@Override
		public void onRemoteIceCandidate(final IceCandidate candidate) {
			if (DEBUG) Log.v(TAG, "onRemoteIceCandidate:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (janusClient == null) {
						Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
					}
				}
			});
		}
	
		@Override
		public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
			if (DEBUG) Log.v(TAG, "onRemoteIceCandidatesRemoved:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (janusClient == null) {
						Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
					}
				}
			});
		}
	
		@Override
		public void onChannelClose() {
			if (DEBUG) Log.v(TAG, "onChannelClose:");
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					logAndToast("Remote end hung up; dropping PeerConnection");
					disconnect();
				}
			});
		}
		
		@Override
		public void onEvent(@NonNull final JSONObject event) {
			if (DEBUG) Log.v(TAG, "onEvent:" + event);
		}
		
		@Override
		public void onChannelError(final String description) {
			reportError(description);
		}

	};
}
