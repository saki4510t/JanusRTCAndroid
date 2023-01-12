package com.serenegiant.janus;
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2022 saki t_saki@serenegiant.com
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

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.serenegiant.janus.request.CreateSession;
import com.serenegiant.janus.request.DestroySession;
import com.serenegiant.janus.request.videoroom.ConfigPublisher;
import com.serenegiant.janus.request.videoroom.ConfigSubscriber;
import com.serenegiant.janus.response.videoroom.RoomEvent;
import com.serenegiant.janus.response.videoroom.PublisherInfo;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;
import com.serenegiant.janus.response.videoroom.RoomInfo;
import com.serenegiant.system.BuildCheck;

import org.appspot.apprtc.AppRTCConst;
import org.appspot.apprtc.PeerConnectionParameters;
import org.appspot.apprtc.RecordedAudioToFileController;
import org.appspot.apprtc.RoomConnectionParameters;
import org.appspot.apprtc.RtcEventLog;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpParameters;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.RequiresApi;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.serenegiant.janus.Const.*;
import static com.serenegiant.janus.Utils.*;

/**
 * Janus-gatewayへアクセスするためのヘルパークラス
 * とりあえず自分を含めて3人での通話に対応
 * FIXME 今はpublisherとsubscriberで別々のPeerConnectionを生成しているのを1つにする
 *       => 調べた限りではpublisherとsubscriberは別々のPeerConnectionにせざるをえない感じ
 *          ただし、1つのsubscriberで複数の相手からのストリーム(マルチストリーム)が
 *          できる感じ(今は1つの相手につき1つのsubscriberになっているけど)
 * FIXME 動的にレイアウトを変更するかListView/RecyclerViewに入れるなどしてもう少し多い相手との通話できるようにする
 * FIXME RxJavaを使うように変える
 */
public class JanusVideoRoomClient implements VideoRoomClient {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = JanusVideoRoomClient.class.getSimpleName();
	
	private static final PeerConnection.SdpSemantics SDP_SEMANTICS
		= PeerConnection.SdpSemantics.UNIFIED_PLAN;

	private static enum ConnectionState {
		UNINITIALIZED,
		READY,	// janus-gateway server is ready to access
		CONNECTED,
		CLOSED,
		ERROR }

	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final EglBase rootEglBase;
	@NonNull
	private final PeerConnectionParameters peerConnectionParameters;
	@NonNull
	private final RoomConnectionParameters roomConnectionParameters;
	@NonNull
	private final JanusCallback mCallback;
	private final boolean dataChannelEnabled;
//--------------------------------------------------------------------------------
	@NonNull
	private final Timer statsTimer = new Timer();
	@Nullable
	private PeerConnectionFactory factory;
	private boolean videoCapturerStopped;
	private boolean isError;
	@Nullable
	private SurfaceTextureHelper surfaceTextureHelper;
	/**
	 * Implements the WebRtcAudioRecordSamplesReadyCallback interface and writes
	 * recorded audio samples to an output file.
	 */
	@Nullable
	private RecordedAudioToFileController saveRecordedAudioToFile = null;
	// ローカル映像・音声
	private int videoWidth;
	private int videoHeight;
	private int videoFps;
	/** enableAudio is set to true if audio should be sent. */
	private boolean enableAudio = true;
	/**
	 * enableVideo is set to true if video should be rendered and sent.
	 */
	private boolean renderVideo = true;
	@Nullable
	private VideoSink localRender;
	@Nullable
	private VideoTrack localVideoTrack;
	@Nullable
	private RtpSender localVideoSender;
	@Nullable
	private VideoCapturer videoCapturer;
	@Nullable
	private VideoSource videoSource;
	@Nullable
	private AudioSource audioSource;
	@Nullable
	private AudioTrack localAudioTrack;
	private MediaStream mLocalStream;
	private JavaAudioDeviceModule mAudioDeviceModule;
	/**
	 * リモート映像・音声のpluginのfeedIdとVideoSinkHolderのマップ
	 */
	private final Map<Long, VideoSinkHolder> remoteVideoSinkMap = new HashMap<>();
//--------------------------------------------------------------------------------

	private VideoRoomAPI mJanus;
	private LongPoll mLongPoll;
	@NonNull
	private final List<Call<?>> mCurrentCalls = new ArrayList<>();
	@NonNull
	private final Map<Long, VideoRoomPlugin> mAttachedPlugins
		= new ConcurrentHashMap<>();
	private ConnectionState mConnectionState;
	private ServerInfo mServerInfo;
	private Session mSession;
	/**
	 * 音声入力デバイスのヒント
	 */
	@Nullable
	private AudioDeviceInfo mPreferredInputDevice = null;

	/**
	 * コンストラクタ
	 * apiName = "janus", roomId = 1234
	 * @param appContext
	 * @param baseUrl
	 * @param eglBase
	 * @param peerConnectionParameters
	 * @param callback
	 */
	public JanusVideoRoomClient(
		@NonNull final Context appContext,
		@NonNull final String baseUrl,
		@NonNull final EglBase eglBase,
		@NonNull final PeerConnectionParameters peerConnectionParameters,
		@NonNull final RoomConnectionParameters roomConnectionParameters,
		@NonNull final JanusCallback callback) {

		this(appContext, eglBase,
			peerConnectionParameters, roomConnectionParameters, callback);
	}
	
	/**
	 * コンストラクタ
	 * @param appContext
	 * @param eglBase
	 * @param peerConnectionParameters
	 * @param roomConnectionParameters
	 * @param callback
	 */
	public JanusVideoRoomClient(
		@NonNull final Context appContext,
		@NonNull final EglBase eglBase,
		@NonNull final PeerConnectionParameters peerConnectionParameters,
		@NonNull final RoomConnectionParameters roomConnectionParameters,
		@NonNull final JanusCallback callback) {

		this.mWeakContext = new WeakReference<>(appContext);
		this.rootEglBase = eglBase;
		this.peerConnectionParameters = peerConnectionParameters;
		this.roomConnectionParameters = roomConnectionParameters;
		this.mCallback = callback;

		this.mConnectionState = ConnectionState.UNINITIALIZED;
		this.dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null;

		final String fieldTrials = peerConnectionParameters.getFieldTrials();
		executor.execute(() -> {
			if (DEBUG) Log.d(TAG,
				"Initialize WebRTC. Field trials: " + fieldTrials + " Enable video HW acceleration: "
					+ peerConnectionParameters.videoCodecHwAcceleration);
			PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder(appContext)
					.setFieldTrials(fieldTrials)
//					.setEnableVideoHwAcceleration(peerConnectionParameters.videoCodecHwAcceleration)
					.setEnableInternalTracer(true)
					.createInitializationOptions());
		});
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			release();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * disconnect and release related resources
	 */
	public void release() {
		disconnectFromRoom();
	}

//================================================================================
// implementations of com.serenegiant.janus.JanusClient interface

	/**
	 * This function should only be called once.
	 */
	@Override
	public void createPeerConnectionFactory(
		@NonNull final PeerConnectionFactory.Options options) {

		if (DEBUG) Log.v(TAG, "createPeerConnectionFactory:");
		if (factory != null) {
			throw new IllegalStateException("PeerConnectionFactory has already been constructed");
		}
		executor.execute(() -> createPeerConnectionFactoryInternal(options));
	}
	
	/**
	 * Publisher用のPeerConnectionを生成する
	 * @param localRender
	 * @param videoCapturer
	 */
	@Override
	public void createPeerConnection(
		@NonNull final VideoSink localRender,
		@Nullable final VideoCapturer videoCapturer) {

		if (DEBUG) Log.v(TAG, "createPeerConnection:");
		this.localRender = localRender;
		this.videoCapturer = videoCapturer;
		executor.execute(() -> {
			try {
				createMediaConstraintsInternal();
				createPublisherInternal();
			} catch (Exception e) {
				reportError(e);
				throw e;
			}
		});
	}

	@Override
	public void startVideoSource() {
		if (DEBUG) Log.v(TAG, "startVideoSource:");
		executor.execute(() -> {
			if (videoCapturer != null && videoCapturerStopped) {
				if (DEBUG) Log.d(TAG, "Restart video source.");
				videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
				videoCapturerStopped = false;
			}
		});
	}

	@Override
	public void stopVideoSource() {
		if (DEBUG) Log.v(TAG, "stopVideoSource:");
		executor.execute(() -> {
			if (videoCapturer != null && !videoCapturerStopped) {
				if (DEBUG) Log.d(TAG, "Stop video source.");
				try {
					videoCapturer.stopCapture();
				} catch (final InterruptedException e) {
					// ignore
				}
				videoCapturerStopped = true;
			}
		});
	}
	
	private TimerTask mTimerTask;
	@Override
	public void enableStatsEvents(final boolean enable, final int periodMs) {
		if (DEBUG) Log.v(TAG, "enableStatsEvents:");
		if (enable) {
			cancelTimerTask();
			mTimerTask = new TimerTask() {
				@Override
				public void run() {
					executor.execute(() -> getStats());
				}
			};
			try {
				statsTimer.schedule(mTimerTask, 0, periodMs);
			} catch (Exception e) {
				Log.e(TAG, "Can not schedule statistics timer", e);
			}
		} else {
			cancelTimerTask();
		}
	}

	private void cancelTimerTask() {
		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}
	}

	@Override
	public void switchCamera() {
		if (DEBUG) Log.v(TAG, "switchCamera:");
		executor.execute(this::switchCameraInternal);
	}
	
	@Override
	public void changeCaptureFormat(final int width, final int height, final int framerate) {
		if (DEBUG) Log.v(TAG, "changeCaptureFormat:");
		executor.execute(() -> changeCaptureFormatInternal(width, height, framerate));
	}
	
	@Override
	public void setAudioEnabled(final boolean enable) {
		if (DEBUG) Log.v(TAG, "setAudioEnabled:");
		executor.execute(() -> {
			enableAudio = enable;
			if (localAudioTrack != null) {
				localAudioTrack.setEnabled(enableAudio);
			}
		});
	}
	
	@Override
	public void setVideoEnabled(final boolean enable) {
		if (DEBUG) Log.v(TAG, "setVideoEnabled:");
		executor.execute(() -> {
			renderVideo = enable;
			if (localVideoTrack != null) {
				localVideoTrack.setEnabled(renderVideo);
			}
			for (VideoSinkHolder holder: remoteVideoSinkMap.values()) {
				holder.setEnabled(renderVideo);
			}
		});
	}

	/**
	 * request list of available room
	 * @param callback
	 */
	@Override
	public void requestRoomList(@NonNull final ListCallback<List<RoomInfo>> callback) {
		if (DEBUG) Log.v(TAG, "list:");
		executor.execute(() -> {
			listRoomInternal(
				roomConnectionParameters.roomUrl,
				roomConnectionParameters.apiName, callback);
		});
	}

	@Override
	public void connectToRoom(@NonNull final RoomConnectionParameters connectionParameters) {
		if (DEBUG) Log.v(TAG, "connectToRoom:");
		executor.execute(() -> {
			connectToRoomInternal();
		});
	}
	
	@Override
	public void disconnectFromRoom() {
		cancelTimerTask();
		statsTimer.cancel();
		if (mConnectionState != ConnectionState.CLOSED) {
			if (DEBUG) Log.v(TAG, "disconnectFromRoom:");
			cancelCall();
			executor.execute(() -> {
				disconnectFromRoomInternal();
			});
		}
	}

	/**
	 * PublisherのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分のパブリッシャーのプラグインIDのはず
	 * @return
	 */
	@NonNull
	@Override
	public Collection<Long> getPublishers() {
		final List<Long> result = new ArrayList<>();
		synchronized (mAttachedPlugins) {
			for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
				if (plugin instanceof VideoRoomPlugin.Publisher) {
					result.add(plugin.pluginId());
				}
			}
		}
		return result;
	}

	/**
	 * SubscriberのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分がサブスクライブしているリモートに対応するプラグインIDのはず
	 * @return
	 */
	@NonNull
	@Override
	public Collection<Long> getSubscribers() {
		final List<Long> result = new ArrayList<>();
		synchronized (mAttachedPlugins) {
			for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
				if (plugin instanceof VideoRoomPlugin.Subscriber) {
					result.add(plugin.pluginId());
				}
			}
		}
		return result;
	}

	/**
	 * 全てのPublisherを設定する
	 * @param config
	 * @return
	 */
	@Override
	public boolean configure(@NonNull final ConfigPublisher config) {
		if (DEBUG) Log.v(TAG, "configure:" + config);
		boolean result = false;
		if (mConnectionState == ConnectionState.CONNECTED) {
			synchronized (mAttachedPlugins) {
				for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
					if (plugin instanceof VideoRoomPlugin.Publisher) {
						result |= ((VideoRoomPlugin.Publisher) plugin).configure(config);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 指定したプラグインIDが一致する最初のPublisherを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	@Override
	public boolean configure(final long pluginId, @NonNull final ConfigPublisher config) {
		if (DEBUG) Log.v(TAG, "configure:id=" + pluginId + "," + config);
		if (mConnectionState == ConnectionState.CONNECTED) {
			synchronized (mAttachedPlugins) {
				for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
					if ((plugin instanceof VideoRoomPlugin.Publisher)
						&& (plugin.pluginId() == pluginId)) {
						return ((VideoRoomPlugin.Publisher) plugin).configure(config);
					}
				}
			}
		}
		return false;
	}

	/**
	 * 全てのSubscriberを設定する
	 * @param config
	 * @return
	 */
	@Override
	public boolean configure(@NonNull final ConfigSubscriber config) {
		if (DEBUG) Log.v(TAG, "configure:" + config);
		boolean result = false;
		if (mConnectionState == ConnectionState.CONNECTED) {
			synchronized (mAttachedPlugins) {
				for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
					if (plugin instanceof VideoRoomPlugin.Subscriber) {
						result |= ((VideoRoomPlugin.Subscriber) plugin).configure(config);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 指定したプラグインIDが一致する最初のSubscriberを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	@Override
	public boolean configure(final long pluginId, @NonNull final ConfigSubscriber config) {
		if (DEBUG) Log.v(TAG, "configure:id=" + pluginId + "," + config);
		if (mConnectionState == ConnectionState.CONNECTED) {
			synchronized (mAttachedPlugins) {
				for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
					if ((plugin instanceof VideoRoomPlugin.Subscriber)
						&& (plugin.pluginId() == pluginId)) {
						return ((VideoRoomPlugin.Subscriber) plugin).configure(config);
					}
				}
			}
		}
		return false;
	}

	/**
	 * 通話時の音声入力デバイスのヒントを設定
	 * @param preferredInputDevice
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.M)
	public void setPreferredInputDevice(@Nullable final AudioDeviceInfo preferredInputDevice) {
		mPreferredInputDevice = preferredInputDevice;
		if (mAudioDeviceModule != null) {
			mAudioDeviceModule.setPreferredInputDevice(preferredInputDevice);
		}
	}

//================================================================================
	private void createPeerConnectionFactoryInternal(
		@NonNull final PeerConnectionFactory.Options options) {
	
		if (DEBUG) Log.v(TAG, "createPeerConnectionFactoryInternal:");
		isError = false;
		
		if (peerConnectionParameters.tracing) {
			PeerConnectionFactory.startInternalTracingCapture(
				Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
					+ "webrtc-trace.txt");
		}
		
		// It is possible to save a copy in raw PCM format on a file by checking
		// the "Save input audio to file" checkbox in the Settings UI. A callback
		// interface is set when this flag is enabled. As a result, a copy of recorded
		// audio samples are provided to this client directly from the native audio
		// layer in Java.
		if (peerConnectionParameters.saveInputAudioToFile) {
			if (!peerConnectionParameters.useOpenSLES) {
				if (DEBUG) Log.d(TAG, "Enable recording of microphone input audio to file");
				saveRecordedAudioToFile = new RecordedAudioToFileController(executor);
			} else {
				// TODO(henrika): ensure that the UI reflects that if OpenSL ES is selected,
				// then the "Save inut audio to file" option shall be grayed out.
				Log.e(TAG, "Recording of input audio is not supported for OpenSL ES");
			}
		}

		final AudioDeviceModule adm = createJavaAudioDevice();
		if (BuildCheck.isAPI23() && (adm instanceof JavaAudioDeviceModule)) {
			mAudioDeviceModule = (JavaAudioDeviceModule)adm;
			((JavaAudioDeviceModule) adm).setPreferredInputDevice(mPreferredInputDevice);
		}
		
		// Create peer connection factory.
		if (options != null && DEBUG) {
			if (DEBUG) Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
		}
		final boolean enableH264HighProfile =
			AppRTCConst.VIDEO_CODEC_H264_HIGH.equals(peerConnectionParameters.videoCodec);
		final VideoEncoderFactory encoderFactory;
		final VideoDecoderFactory decoderFactory;
		
		if (peerConnectionParameters.videoCodecHwAcceleration) {
			encoderFactory = new DefaultVideoEncoderFactory(
				rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, enableH264HighProfile);
			decoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
		} else {
			encoderFactory = new SoftwareVideoEncoderFactory();
			decoderFactory = new SoftwareVideoDecoderFactory();
		}
		
		factory = PeerConnectionFactory.builder()
			.setOptions(options)
			.setAudioDeviceModule(adm)
			.setVideoEncoderFactory(encoderFactory)
			.setVideoDecoderFactory(decoderFactory)
			.createPeerConnectionFactory();
		// Set INFO libjingle logging.
		// NOTE: this _must_ happen while |factory| is alive!
		Logging.enableLogToDebugOutput(Logging.Severity.LS_ERROR);
		
		if (DEBUG) Log.d(TAG, "Peer connection factory created.");
	}

	private AudioDeviceModule createJavaAudioDevice() {
		if (DEBUG) Log.v(TAG, "createJavaAudioDevice:");
		// Enable/disable OpenSL ES playback.
		if (!peerConnectionParameters.useOpenSLES) {
			Log.w(TAG, "External OpenSLES ADM not implemented yet.");
			// TODO(magjed): Add support for external OpenSLES ADM.
		}
		
		// Set audio record error callbacks.
		final JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback
			= new JavaAudioDeviceModule.AudioRecordErrorCallback() {

			@Override
			public void onWebRtcAudioRecordInitError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioRecordStartError(
				final JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode,
					final String errorMessage) {

				Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioRecordError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
		};
		
		final JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback
			= new JavaAudioDeviceModule.AudioTrackErrorCallback() {

			@Override
			public void onWebRtcAudioTrackInitError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioTrackStartError(
				final JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode,
				final String errorMessage) {

				Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioTrackError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
		};
		
		return JavaAudioDeviceModule.builder(getContext())
			.setAudioSource(peerConnectionParameters.audioSource)
			.setAudioFormat(peerConnectionParameters.audioFormat)
			.setSamplesReadyCallback(saveRecordedAudioToFile)
			.setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
			.setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
			.setAudioRecordErrorCallback(audioRecordErrorCallback)
			.setAudioTrackErrorCallback(audioTrackErrorCallback)
			.createAudioDeviceModule();
	}

	private boolean isVideoCallEnabled() {
		return peerConnectionParameters.videoCallEnabled
			&& (videoCapturer != null);
	}

	private void createMediaConstraintsInternal() {
		if (DEBUG) Log.v(TAG, "createMediaConstraintsInternal:");
		// Create video constraints if video call is enabled.
		if (isVideoCallEnabled()) {
			videoWidth = peerConnectionParameters.videoWidth;
			videoHeight = peerConnectionParameters.videoHeight;
			videoFps = peerConnectionParameters.videoFps;
			
			// If video resolution is not specified, default to HD.
			if (videoWidth == 0 || videoHeight == 0) {
				videoWidth = AppRTCConst.HD_VIDEO_WIDTH;
				videoHeight = AppRTCConst.HD_VIDEO_HEIGHT;
			}
			
			// If fps is not specified, default to 30.
			if (videoFps == 0) {
				videoFps = 30;
			}
			Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps);
		}
	}

	/**
	 * PeerConnection生成用のヘルパーメソッド
	 * @param rtcConfig
	 * @param observer
	 * @return
	 */
	private PeerConnection createPeerConnection(
		PeerConnection.RTCConfiguration rtcConfig,
		PeerConnection.Observer observer) {

		return factory.createPeerConnection(rtcConfig, observer);
	}

	/**
	 * XXX パブリッシャー側は当面マルチストリーム対応しない予定
	 */
	private void createPublisherInternal() {
		if (DEBUG) Log.v(TAG, "createPublisherInternal:");
		final Context context = getContext();
		if ((context == null) || (factory == null) || isError) {
			Log.e(TAG, "Peerconnection factory is not created");
			return;
		}
		if (DEBUG) Log.d(TAG, "Create peer connection.");
		
//		if (isVideoCallEnabled()) {
//			factory.setVideoHwAccelerationOptions(
//				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
//		}
		
		// Create audio constraints.
		final MediaConstraints audioConstraints = new MediaConstraints();
		// added for audio performance measurements
		if (peerConnectionParameters.noAudioProcessing) {
			if (DEBUG) Log.d(TAG, "Disabling audio processing");
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AppRTCConst.AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AppRTCConst.AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AppRTCConst.AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AppRTCConst.AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
		}
		// Create SDP constraints.
		final MediaConstraints sdpMediaConstraints = new MediaConstraints();
		sdpMediaConstraints.mandatory.add(
			new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));
		sdpMediaConstraints.mandatory.add(
			new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
		sdpMediaConstraints.optional.add(
			new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

		final PeerConnection.RTCConfiguration rtcConfig =
			new PeerConnection.RTCConfiguration(mCallback.getIceServers(this));
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
		rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
		rtcConfig.sdpSemantics = SDP_SEMANTICS;

		final List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
		
		final VideoRoomPlugin.Publisher publisher
			= new VideoRoomPlugin.Publisher(
				mJanus, mSession,
				mVideoRoomCallback,
				peerConnectionParameters,
				roomConnectionParameters,
				sdpMediaConstraints,
				isVideoCallEnabled());

		final PeerConnection peerConnection;
		DataChannel dataChannel = null;
		if (SDP_SEMANTICS == PeerConnection.SdpSemantics.UNIFIED_PLAN) {
			peerConnection = createPeerConnection(rtcConfig, publisher);
			
			if (dataChannelEnabled) {
				final DataChannel.Init init = new DataChannel.Init();
				init.ordered = peerConnectionParameters.dataChannelParameters.ordered;
				init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
				init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
				init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
				init.id = peerConnectionParameters.dataChannelParameters.id;
				init.protocol = peerConnectionParameters.dataChannelParameters.protocol;
				dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
			}
	
			if (isVideoCallEnabled()) {
				peerConnection.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels);
				// Publisherは送信のみなのでリモートビデオトラックは不要
			}
			peerConnection.addTrack(createAudioTrack(audioConstraints), mediaStreamLabels);
			if (isVideoCallEnabled()) {
				findVideoSender(peerConnection);
			}
		} else {
			VideoTrack videoTrack = null;
			MediaStream stream = null;
			if (isVideoCallEnabled()) {
				videoTrack = createVideoTrack(videoCapturer);
			}
			final AudioTrack audioTrack = createAudioTrack(audioConstraints);
			if ((videoTrack != null) || (audioTrack != null)) {
				stream = factory.createLocalMediaStream("ARDAMS");
				if (audioTrack != null) {
					stream.addTrack(audioTrack);
				}
				if (videoTrack != null) {
					stream.addTrack(videoTrack);
				}
			}
			peerConnection = createPeerConnection(rtcConfig, publisher);
			if (stream != null) {
				peerConnection.addStream(stream);
				mLocalStream = stream;
			}
			if (dataChannelEnabled) {
				final DataChannel.Init init = new DataChannel.Init();
				init.ordered = peerConnectionParameters.dataChannelParameters.ordered;
				init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
				init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
				init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
				init.id = peerConnectionParameters.dataChannelParameters.id;
				init.protocol = peerConnectionParameters.dataChannelParameters.protocol;
				dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
			}
		}

		if (peerConnectionParameters.aecDump) {
			try {
				final ParcelFileDescriptor aecDumpFileDescriptor =
					ParcelFileDescriptor.open(new File(
						Environment.getExternalStorageDirectory().getPath()
							+ File.separator + "Download/audio.aecdump"),
						ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
							| ParcelFileDescriptor.MODE_TRUNCATE);
				factory.startAecDump(aecDumpFileDescriptor.detachFd(), -1);
			} catch (IOException e) {
				Log.e(TAG, "Can not open aecdump file", e);
			}
		}
		
		if (saveRecordedAudioToFile != null) {
			if (saveRecordedAudioToFile.start()) {
				if (DEBUG) Log.d(TAG, "Recording input audio to file is activated");
			}
		}

		RtcEventLog rtcEventLog = null;
		if (peerConnectionParameters.enableRtcEventLog) {
			rtcEventLog = new RtcEventLog(peerConnection);
			rtcEventLog.start(createRtcEventLogOutputFile());
		} else {
			if (DEBUG) Log.d(TAG, "org.appspot.apprtc.RtcEventLog is disabled.");
		}
		if (DEBUG) Log.d(TAG, "Peer connection created.");

		publisher.setPeerConnection(peerConnection, dataChannel, rtcEventLog);
		publisher.attach();
	}
	
	/**
	 * Subscriberを生成
	 * FIXME マルチストリーム対応を追加する
	 * @param info
	 */
	protected void createSubscriber(
		@NonNull final PublisherInfo info) {
		
		if (DEBUG) Log.v(TAG, "createSubscriber:");

		final Context context = getContext();
		if ((context == null) || (factory == null) || isError) {
			Log.e(TAG, "createSubscriber:Peerconnection factory is not created");
			return;
		}
		if (DEBUG) Log.d(TAG, "createSubscriber:Create peer connection.");
		
//		if (isVideoCallEnabled()) {
//			factory.setVideoHwAccelerationOptions(
//				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
//		}
		
		// Create SDP constraints.
		final MediaConstraints sdpMediaConstraints = new MediaConstraints();
		sdpMediaConstraints.mandatory.add(
			new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		sdpMediaConstraints.mandatory.add(
			new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
		sdpMediaConstraints.optional.add(
			new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

		final PeerConnection.RTCConfiguration rtcConfig =
			new PeerConnection.RTCConfiguration(mCallback.getIceServers(this));
		// TCP candidates are only useful when connecting to a server that supports
		// ICE-TCP.
		rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
		rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
		rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
		rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
		// Use ECDSA encryption.
		rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
		rtcConfig.sdpSemantics = SDP_SEMANTICS;

		// FIXME マルチストリームの時はここで既存のSubscriberを検索して
		//       なければ新規追加、あればトラックを追加みたいになるのかな？
		final VideoRoomPlugin.Subscriber subscriber = new VideoRoomPlugin.Subscriber(
			mJanus, mSession, mVideoRoomCallback,
			peerConnectionParameters,
			roomConnectionParameters,
			sdpMediaConstraints,
			info, isVideoCallEnabled());

		final PeerConnection peerConnection;
		DataChannel dataChannel = null;
		if (SDP_SEMANTICS == PeerConnection.SdpSemantics.UNIFIED_PLAN) {
			peerConnection = createPeerConnection(rtcConfig, subscriber);
			
			if (dataChannelEnabled) {
				final DataChannel.Init init = new DataChannel.Init();
				init.ordered = peerConnectionParameters.dataChannelParameters.ordered;
				init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
				init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
				init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
				init.id = peerConnectionParameters.dataChannelParameters.id;
				init.protocol = peerConnectionParameters.dataChannelParameters.protocol;
				dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
			}
			
			if (isVideoCallEnabled()) {
				// We can add the renderers right away because we don't need to wait for an
				// answer to get the remote track.
				final VideoTrack remoteVideoTrack = getRemoteVideoTrack(peerConnection);
				if (remoteVideoTrack != null) {
					synchronized (remoteVideoSinkMap) {
						VideoSinkHolder holder = getHolder(info.id);
						if (holder == null) {
							final List<VideoSink> remoteVideoSinks = mCallback.getRemoteVideoSink(info);
							if (!remoteVideoSinks.isEmpty()) {
								remoteVideoTrack.setEnabled(renderVideo);
								Log.i(TAG, "createSubscriber: create VideoSinkHolder");
								holder = new VideoSinkHolder(info.id, remoteVideoTrack, remoteVideoSinks);
								remoteVideoSinkMap.put(info.id, holder);
							} else if (DEBUG) {
								Log.v(TAG, "createSubscriber:remote video sinks are empty");
							}
						} else {
							Log.w(TAG, "createSubscriber: unexpectedly video sink holder is already exists!");
						}
					}
				}
			}
		} else {
			peerConnection = createPeerConnection(rtcConfig, subscriber);
			if (dataChannelEnabled) {
				final DataChannel.Init init = new DataChannel.Init();
				init.ordered = peerConnectionParameters.dataChannelParameters.ordered;
				init.negotiated = peerConnectionParameters.dataChannelParameters.negotiated;
				init.maxRetransmits = peerConnectionParameters.dataChannelParameters.maxRetransmits;
				init.maxRetransmitTimeMs = peerConnectionParameters.dataChannelParameters.maxRetransmitTimeMs;
				init.id = peerConnectionParameters.dataChannelParameters.id;
				init.protocol = peerConnectionParameters.dataChannelParameters.protocol;
				dataChannel = peerConnection.createDataChannel("ApprtcDemo data", init);
			}
		}
		
		if (saveRecordedAudioToFile != null) {
			if (saveRecordedAudioToFile.start()) {
				if (DEBUG) Log.d(TAG, "createSubscriber:Recording input audio to file is activated");
			}
		}

		RtcEventLog rtcEventLog = null;
		if (peerConnectionParameters.enableRtcEventLog) {
			rtcEventLog = new RtcEventLog(peerConnection);
			rtcEventLog.start(createRtcEventLogOutputFile());
		} else {
			if (DEBUG) Log.d(TAG, "createSubscriber: RtcEventLog is disabled.");
		}
		if (DEBUG) Log.d(TAG, "createSubscriber: Peer connection created.");

		subscriber.setPeerConnection(peerConnection, dataChannel, rtcEventLog);
		subscriber.attach();
	}

	private File createRtcEventLogOutputFile() {
		if (DEBUG) Log.v(TAG, "createRtcEventLogOutputFile:");
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault());
		Date date = new Date();
		final String outputFileName = "event_log_" + dateFormat.format(date) + ".log";
		return new File(
			getContext().getDir(AppRTCConst.RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE), outputFileName);
	}

	@Nullable
	private AudioTrack createAudioTrack(final MediaConstraints audioConstraints) {
		if (DEBUG) Log.v(TAG, "createAudioTrack:");
		audioSource = factory.createAudioSource(audioConstraints);
		localAudioTrack = factory.createAudioTrack(AppRTCConst.AUDIO_TRACK_ID, audioSource);
		localAudioTrack.setEnabled(enableAudio);
		return localAudioTrack;
	}
	
	@Nullable
	private VideoTrack createVideoTrack(final VideoCapturer capturer) {
		if (DEBUG) Log.v(TAG, "createVideoTrack:");
		surfaceTextureHelper =
			SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
		videoSource = factory.createVideoSource(capturer.isScreencast());
		capturer.initialize(surfaceTextureHelper, getContext(), videoSource.getCapturerObserver());
		capturer.startCapture(videoWidth, videoHeight, videoFps);
		
		localVideoTrack = factory.createVideoTrack(AppRTCConst.VIDEO_TRACK_ID, videoSource);
		localVideoTrack.setEnabled(renderVideo);
		localVideoTrack.addSink(localRender);
		return localVideoTrack;
	}
	
	private void findVideoSender(@NonNull final PeerConnection peerConnection) {
		if (DEBUG) Log.v(TAG, "findVideoSender:");
		for (final RtpSender sender : peerConnection.getSenders()) {
			if (sender.track() != null) {
				String trackType = sender.track().kind();
				if (trackType.equals(AppRTCConst.VIDEO_TRACK_TYPE)) {
					if (DEBUG) Log.d(TAG, "Found video sender.");
					localVideoSender = sender;
				}
			}
		}
	}
	
	/**
	 * Returns the remote VideoTrack, assuming there is only one.
	 * @param peerConnection
	 * @return
	 */
	@Nullable
	private VideoTrack getRemoteVideoTrack(@NonNull final PeerConnection peerConnection) {
		if (DEBUG) Log.v(TAG, "getRemoteVideoTrack:");
		for (final RtpTransceiver transceiver : peerConnection.getTransceivers()) {
			final MediaStreamTrack track = transceiver.getReceiver().track();
			if (DEBUG) Log.v(TAG, "getRemoteVideoTrack:transceiver=" + transceiver + ",track=" + track);
			if (track instanceof VideoTrack) {
				return (VideoTrack) track;
			}
		}
		return null;
	}
	
	private void switchCameraInternal() {
		if (DEBUG) Log.v(TAG, "switchCameraInternal:");
		if (videoCapturer instanceof CameraVideoCapturer) {
			if (!isVideoCallEnabled() || isError) {
				Log.e(TAG,
					"Failed to switch camera. Video: " + isVideoCallEnabled() + ". Error : " + isError);
				return; // No video is sent or only one camera is available or error happened.
			}
			if (DEBUG) Log.d(TAG, "Switch camera");
			CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
			cameraVideoCapturer.switchCamera(null);
		} else {
			if (DEBUG) Log.d(TAG, "Will not switch camera, video caputurer is not a camera");
		}
	}
	
	private void changeCaptureFormatInternal(int width, int height, int framerate) {
		if (DEBUG) Log.v(TAG, "changeCaptureFormatInternal:");
		if (!isVideoCallEnabled() || isError || videoCapturer == null) {
			Log.e(TAG,
				"Failed to change capture format. Video: " + isVideoCallEnabled()
					+ ". Error : " + isError);
			return;
		}
		if (DEBUG) Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
		videoSource.adaptOutputFormat(width, height, framerate);
	}

	@SuppressWarnings("deprecation") // TODO(sakal): getStats is deprecated.
	private void getStats() {
		if (DEBUG) Log.v(TAG, "getStats:");
		synchronized (mAttachedPlugins) {
			for (final VideoRoomPlugin plugin: mAttachedPlugins.values()) {
				plugin.requestStats();
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

//--------------------------------------------------------------------------------
	@Nullable
	private Context getContext() {
		return mWeakContext.get();
	}

	private void onAddRemoteStream(
		@NonNull final PublisherInfo info,
		@NonNull final MediaStream remoteStream) {

		if (DEBUG) Log.v(TAG, "onAddRemoteStream:" + getHolder(info.id));
		if (isVideoCallEnabled()
			&& (remoteStream != null) && !remoteStream.videoTracks.isEmpty()) {

			final VideoTrack remoteVideoTrack = remoteStream.videoTracks.get(0);
			VideoSinkHolder holder;
			synchronized (remoteVideoSinkMap) {
				holder = getHolder(info.id);
				if (holder == null) {
					if (remoteVideoTrack != null) {
						final List<VideoSink> remoteVideoSinks = mCallback.getRemoteVideoSink(info);
						if (!remoteVideoSinks.isEmpty()) {
							remoteVideoTrack.setEnabled(renderVideo);
							if (DEBUG) Log.v(TAG, "onAddRemoteStream:create VideoSinkHolder");
							holder = new VideoSinkHolder(info.id, remoteVideoTrack, remoteVideoSinks);
							remoteVideoSinkMap.put(info.id, holder);
						} else if (DEBUG) {
							Log.v(TAG, "onAddRemoteStream:remote video sinks are empty");
						}
					}
				}
			}
			if (holder != null) {
				holder.setMediaStream(remoteStream);
			}
		}
	}

	private void onRemoveRemoteStream(
		@NonNull final PublisherInfo info,
		@NonNull final MediaStream remoteStream) {

		if (DEBUG) Log.v(TAG, "onAddRemoteStream:" + getHolder(info.id));
		synchronized (remoteVideoSinkMap) {
			final VideoSinkHolder removed = remoteVideoSinkMap.remove(info.id);
			if (DEBUG) Log.v(TAG, "onAddRemoteStream:removed=" + removed);
		}
	}

	@Nullable
	private VideoSinkHolder getHolder(final long feedId) {
		return remoteVideoSinkMap.containsKey(feedId) ? remoteVideoSinkMap.get(feedId) : null;
	}

	/**
	 * set call that is currently in progress
	 * @param call
	 */
	private void addCall(@NonNull final Call<?> call) {
		synchronized (mCurrentCalls) {
			mCurrentCalls.add(call);
		}
	}
	
	private void removeCall(@NonNull final Call<?> call) {
		synchronized (mCurrentCalls) {
			mCurrentCalls.remove(call);
		}
		if (!call.isCanceled()) {
			try {
				call.cancel();
			} catch (final Exception e) {
				Log.w(TAG, e);
			}
		}
	}
	
	/**
	 * cancel call if call is in progress
	 */
	private void cancelCall() {
		synchronized (mCurrentCalls) {
			for (final Call<?> call: mCurrentCalls) {
				if ((call != null) && !call.isCanceled()) {
					try {
						call.cancel();
					} catch (final Exception e) {
						Log.w(TAG, e);
					}
				}
			}
			mCurrentCalls.clear();
		}
	}

//--------------------------------------------------------------------------------
	private void addPlugin(@NonNull final VideoRoomPlugin plugin) {
		synchronized (mAttachedPlugins) {
			mAttachedPlugins.put(plugin.pluginId(), plugin);
		}
	}

	private void removePlugin(@NonNull final VideoRoomPlugin plugin) {
		final long key = plugin.pluginId();
		executor.execute(() -> {
			synchronized (mAttachedPlugins) {
				mAttachedPlugins.remove(key);
			}
		});
	}

	private VideoRoomPlugin getPlugin(final long key) {
		synchronized (mAttachedPlugins) {
			if (mAttachedPlugins.containsKey(key)) {
				return mAttachedPlugins.get(key);
			}
		}
		return null;
	}
	
	private void leavePlugin(final long leavePlugin, final int numUsers) {
		if (DEBUG) Log.v(TAG, "leavePlugin:" + leavePlugin);
		VideoRoomPlugin found = null;
	
		synchronized (mAttachedPlugins) {
			// feederIdが一致するSubscriberを探す
			for (Map.Entry<Long, VideoRoomPlugin> entry: mAttachedPlugins.entrySet()) {
				final VideoRoomPlugin plugin = entry.getValue();
				if (plugin instanceof VideoRoomPlugin.Subscriber) {
					if (leavePlugin == plugin.getFeedId()) {
						found = plugin;
						break;
					}
				}
			}
		}
		if (DEBUG) Log.v(TAG, "leavePlugin:found=" + found);
		if (found != null) {
			// feederIdが一致するSubscriberが見つかった時はdetachする
			final VideoRoomPlugin subscriber = found;
			executor.execute(() -> {
				subscriber.detach();
				mCallback.onLeave(
					((VideoRoomPlugin.Subscriber)subscriber).info, numUsers);
			});
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * notify error
	 * @param t
	 */
	private void reportError(@NonNull final Throwable t) {
		Log.w(TAG, t);
		cancelCall();
		TransactionManager.clearTransactions();
		try {
			executor.execute(() -> {
				if (mConnectionState != ConnectionState.ERROR) {
					mConnectionState = ConnectionState.ERROR;
					mCallback.onChannelError(t);
				}
			});
		} catch (final Exception e) {
			// ignore, will be already released.
		}
	}

	private void listRoomInternal(
		@NonNull final String roomUrl,
		@NonNull final String apiName,
		@NonNull final ListCallback<List<RoomInfo>> callback) {
		if (DEBUG) Log.v(TAG, "listRoomInternal:");
//		final VideoRoomAPI api = setupRetrofit(
//			setupHttpClient(false, HTTP_READ_TIMEOUT_MS, HTTP_WRITE_TIMEOUT_MS, DEFAULT_BUILDER_CALLBACK),
//			roomUrl, DEFAULT_BUILDER_CALLBACK).create(VideoRoomAPI.class);
		// FIXME 未実装
	}

	/**
	 * Connects to room - function runs on a local looper thread.
	 */
	private void connectToRoomInternal() {
		if (DEBUG) Log.v(TAG, "connectToRoomInternal:");
		// 通常のRESTアクセス用APIインターフェースを生成
		final VideoRoomAPI api = setupRetrofit(
			setupHttpClient(false, HTTP_READ_TIMEOUT_MS, HTTP_WRITE_TIMEOUT_MS, mCallback),
			roomConnectionParameters.roomUrl, mCallback).create(VideoRoomAPI.class);
		mJanus = api;
		// long poll用APIインターフェースを生成
		mLongPoll = setupRetrofit(
			setupHttpClient(true, HTTP_READ_TIMEOUT_MS_LONG_POLL, HTTP_WRITE_TIMEOUT_MS, mCallback),
			roomConnectionParameters.roomUrl, mCallback).create(LongPoll.class);
		try {
			requestServerInfo(api);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * Disconnect from room and send bye messages - runs on a local looper thread.
	 */
	private void disconnectFromRoomInternal() {
		if (DEBUG) Log.v(TAG, "disconnectFromRoomInternal:state=" + mConnectionState);
		cancelCall();
		if ((mConnectionState == ConnectionState.CONNECTED)) {

			if (DEBUG) Log.d(TAG, "Closing room.");
			detachAll();
		}
		destroy();
	}
	
//--------------------------------------------------------------------
	private void requestServerInfo(@NonNull final VideoRoomAPI api) {
		if (DEBUG) Log.v(TAG, "requestServerInfo:");
		// Janus-gatewayサーバー情報を取得
		final Call<ServerInfo> call = api.getInfo(roomConnectionParameters.apiName);
		addCall(call);
		call.enqueue(new Callback<ServerInfo>() {
			@Override
			public void onResponse(@NonNull final Call<ServerInfo> call,
				@NonNull final Response<ServerInfo> response) {
			
				if (response.isSuccessful() && (response.body() != null)) {
					removeCall(call);
					mServerInfo = response.body();
					if (DEBUG) Log.v(TAG, "requestServerInfo:success");
					executor.execute(() -> {
						createSession();
					});
				} else {
					reportError(new RuntimeException("unexpected response:" + response));
				}
			}
			
			@Override
			public void onFailure(@NonNull final Call<ServerInfo> call,
				@NonNull final Throwable t) {

				reportError(t);
			}
		});
	}
	
	private void createSession() {
		if (DEBUG) Log.v(TAG, "createSession:");
		// サーバー情報を取得できたらセッションを生成
		final Call<Session> call = mJanus.createSession(
			roomConnectionParameters.apiName, new CreateSession());
		addCall(call);
		call.enqueue(new Callback<Session>() {
			@Override
			public void onResponse(@NonNull final Call<Session> call,
				@NonNull final Response<Session> response) {

				if (response.isSuccessful() && (response.body() != null)) {
					removeCall(call);
					mSession = response.body();
					if ("success".equals(mSession.janus)) {
						mConnectionState = ConnectionState.READY;
						// セッションを生成できた＼(^o^)／
						if (DEBUG) Log.v(TAG, "createSession:success");
						// パブリッシャーをVideoRoomプラグインにアタッチ
						executor.execute(() -> {
							longPoll();
							mCallback.onConnectServer(JanusVideoRoomClient.this);
						});
					} else {
						mSession = null;
						reportError(new RuntimeException("unexpected response:" + response));
					}
				} else {
					reportError(new RuntimeException("unexpected response:" + response));
				}
			}
			
			@Override
			public void onFailure(@NonNull final Call<Session> call,
				@NonNull final Throwable t) {

				reportError(t);
			}
		});
	}

	/**
	 * detachAll from VideoRoom plugin
	 */
	private void detachAll() {
		if (DEBUG) Log.v(TAG, "detachAll:");
		cancelCall();
		mConnectionState = ConnectionState.CLOSED;
		synchronized (mAttachedPlugins) {
			for (final Map.Entry<Long, VideoRoomPlugin> entry:
				mAttachedPlugins.entrySet()) {

				entry.getValue().detach();
			}
			mAttachedPlugins.clear();
		}
	}
	
	/**
	 * destroy session
	 */
	private void destroy() {
		if (DEBUG) Log.v(TAG, "destroy:");
		cancelCall();
		detachAll();
		if (mSession != null) {
			final DestroySession destroy = new DestroySession(mSession, null);
			final Call<Void> call = mJanus.destroySession(
				roomConnectionParameters.apiName, mSession.id(), destroy);
			addCall(call);
			try {
				call.execute();
			} catch (final IOException e) {
				reportError(e);
			}
			removeCall(call);
		}
		mSession = null;
		mServerInfo = null;
		mConnectionState = ConnectionState.CLOSED;
		TransactionManager.clearTransactions();
		mJanus = null;
		mLocalStream = null;
		remoteVideoSinkMap.clear();
		cancelTimerTask();
		statsTimer.cancel();
		if (DEBUG) Log.d(TAG, "Closing audio source.");
		if (audioSource != null) {
			audioSource.dispose();
			audioSource = null;
		}
		mAudioDeviceModule = null;
		if (DEBUG) Log.d(TAG, "Stopping capture.");
		if (videoCapturer != null) {
			try {
				videoCapturer.stopCapture();
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
			videoCapturerStopped = true;
			videoCapturer.dispose();
			videoCapturer = null;
		}
		if (DEBUG) Log.d(TAG, "Closing video source.");
		if (videoSource != null) {
			videoSource.dispose();
			videoSource = null;
		}
		if (saveRecordedAudioToFile != null) {
			if (DEBUG) Log.d(TAG, "Closing audio file for recorded input audio.");
			saveRecordedAudioToFile.stop();
			saveRecordedAudioToFile = null;
		}
		localRender = null;
		if (factory != null && peerConnectionParameters.aecDump) {
			factory.stopAecDump();
		}
		if (surfaceTextureHelper != null) {
			try {
				surfaceTextureHelper.dispose();
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
			}
			surfaceTextureHelper = null;
	    }
		if (DEBUG) Log.d(TAG, "Closing peer connection factory.");
		if (factory != null) {
			factory.dispose();
			factory = null;
		}
		rootEglBase.release();
		if (DEBUG) Log.d(TAG, "Closing peer connection done.");
		mCallback.onDisconnected();
		PeerConnectionFactory.stopInternalTracingCapture();
		PeerConnectionFactory.shutdownInternalTracer();
	}

	private void setVideoMaxBitrate(final int maxBitrateKbps) {
		if (DEBUG) Log.v(TAG, "maxBitrateKbps:");
		executor.execute(() -> {
			if (localVideoSender == null || isError) {
				return;
			}
			if (DEBUG) Log.d(TAG, "Requested max video bitrate: " + maxBitrateKbps);
			if (localVideoSender == null) {
				Log.w(TAG, "Sender is not ready.");
				return;
			}
			
			final RtpParameters parameters = localVideoSender.getParameters();
			if (parameters.encodings.size() == 0) {
				Log.w(TAG, "RtpParameters are not ready.");
				return;
			}
			
			for (RtpParameters.Encoding encoding : parameters.encodings) {
				// Null value means no limit.
				encoding.maxBitrateBps = maxBitrateKbps == 0 ? null : maxBitrateKbps * AppRTCConst.BPS_IN_KBPS;
			}
			if (!localVideoSender.setParameters(parameters)) {
				Log.e(TAG, "RtpSender.setParameters failed.");
			}
			if (DEBUG) Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
		});
	}

//--------------------------------------------------------------------------------
	/**
	 * JanusPluginからのコールバックリスナーの実装
	 */
	private final VideoRoomPlugin.VideoRoomCallback mVideoRoomCallback
		= new VideoRoomPlugin.VideoRoomCallback() {
		@Override
		public void onAttach(@NonNull final JanusPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onAttach:" + plugin);
			if (plugin instanceof VideoRoomPlugin) {
				addPlugin((VideoRoomPlugin) plugin);
			}
		}

		@Override
		public void onDetach(@NonNull final JanusPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onDetach:" + plugin);

			if (plugin instanceof VideoRoomPlugin) {
				removePlugin((VideoRoomPlugin) plugin);
			}
		}

		@Override
		public void onJoin(@NonNull final VideoRoomPlugin plugin,
			@NonNull final RoomEvent room) {

			if (DEBUG) Log.v(TAG, "onJoin:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Publisher) {
				mConnectionState = ConnectionState.CONNECTED;
				handleOnJoin(plugin, room);
				plugin.createOffer();
			} else if (plugin instanceof VideoRoomPlugin.Subscriber) {
				handleOnJoin(plugin, room);
				plugin.createAnswer();
			}
		}

		@Override
		public void onEnter(@NonNull final VideoRoomPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onEnter:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Subscriber) {
				mCallback.onEnter(((VideoRoomPlugin.Subscriber) plugin).info);
			}
		}
		
		@Override
		public void onLeave(@NonNull final VideoRoomPlugin plugin,
			final long pluginId, final int numUsers) {
			
			if (DEBUG) Log.v(TAG, "onLeave:" + plugin + ",leave=" + pluginId);

			executor.execute(() -> leavePlugin(pluginId, numUsers));
		}
		
		@Override
		public void onAddRemoteStream(@NonNull final VideoRoomPlugin plugin,
			@NonNull final MediaStream stream) {

			if (DEBUG) Log.v(TAG, "onAddRemoteStream:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Subscriber) {
				executor.execute(() -> JanusVideoRoomClient.this.onAddRemoteStream(
					((VideoRoomPlugin.Subscriber) plugin).info, stream));
			}
		}
		
		@Override
		public void onRemoveStream(@NonNull final VideoRoomPlugin plugin,
			@NonNull final MediaStream stream) {
		
			if (DEBUG) Log.v(TAG, "onRemoveStream:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Subscriber) {
				executor.execute(() -> JanusVideoRoomClient.this.onRemoveRemoteStream(
					((VideoRoomPlugin.Subscriber) plugin).info, stream));
			}
		}
		
		@Override
		public void onRemoteIceCandidate(@NonNull final VideoRoomPlugin plugin,
			@NonNull final IceCandidate candidate) {

			if (DEBUG) Log.v(TAG, "onRemoteIceCandidate:" + plugin
				+ "\n" + candidate);
			executor.execute(() -> mCallback.onRemoteIceCandidate(candidate));
		}
		
		@Override
		public void onIceConnected(@NonNull final VideoRoomPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onIceConnected:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Publisher) {
				// 複数のSubscriberが存在しうるのでPublisherからのイベントのみハンドリング
				executor.execute(() -> mCallback.onIceConnected());
			}
		}
		
		@Override
		public void onIceDisconnected(@NonNull final VideoRoomPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onIceDisconnected:" + plugin);
			if (plugin instanceof VideoRoomPlugin.Publisher) {
				// 複数のSubscriberが存在しうるのでPublisherからのイベントのみハンドリング
				executor.execute(() -> mCallback.onIceDisconnected());
			}
		}
		
		@Override
		public void onLocalDescription(@NonNull final VideoRoomPlugin plugin,
			@NonNull final SessionDescription sdp) {

			if (DEBUG) Log.v(TAG, "onLocalDescription:" + plugin);
//			final long delta = System.currentTimeMillis() - callStartedTimeMs;
			executor.execute(() -> {
//				logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
				if (peerConnectionParameters.videoMaxBitrate > 0) {
					if (DEBUG) Log.d(TAG, "Set video maximum bitrate: "
						+ peerConnectionParameters.videoMaxBitrate);
					setVideoMaxBitrate(
						peerConnectionParameters.videoMaxBitrate);
				}
			});
		}

		@Override
		public void createSubscriber(@NonNull final VideoRoomPlugin plugin,
			@NonNull final PublisherInfo info) {

			if (DEBUG) Log.v(TAG, "createSubscriber:" + plugin);
			executor.execute(() -> {
				if (mCallback.onNewPublisher(info)) {
					JanusVideoRoomClient.this.createSubscriber(info);
				}
			});
		}
		
		@Override
		public void onRemoteDescription(@NonNull final VideoRoomPlugin plugin,
			@NonNull final SessionDescription sdp) {
			
			if (DEBUG) Log.v(TAG, "onRemoteDescription:" + plugin
				+ "\n" + sdp);

			executor.execute(() -> mCallback.onRemoteDescription(sdp));
		}
		
		@Override
		public void onPeerConnectionStatsReady(@NonNull final VideoRoomPlugin plugin,
			@NonNull final RTCStatsReport report) {
			executor.execute(() -> mCallback.onPeerConnectionStatsReady(
					plugin instanceof VideoRoomPlugin.Publisher, report)
			);
		}

		@Override
		public void onError(@NonNull final VideoRoomPlugin plugin,
			@NonNull final Throwable t) {

			reportError(t);
		}
	};

//--------------------------------------------------------------------------------
	/**
	 * TransactionManagerからのコールバック
	 */
	private final TransactionManager.TransactionCallback mTransactionCallback
		= new TransactionManager.TransactionCallback() {
		@Override
		public boolean onReceived(
			@NonNull final String transaction,
			@NonNull final JSONObject json) {

			if (DEBUG) Log.v(TAG, "onReceived:" + json);
			return false;
		}
	};

	/**
	 * long poll asynchronously
	 */
	private void longPoll() {
		if (DEBUG) Log.v(TAG, "longPoll:");
		if (mSession == null) return;
		final Call<ResponseBody> call = mLongPoll.getEvent(
			roomConnectionParameters.apiName, mSession.id());
		addCall(call);
		call.enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(@NonNull final Call<ResponseBody> call,
				@NonNull final Response<ResponseBody> response) {

				if (DEBUG) Log.v(TAG, "longPoll:onResponse");
				removeCall(call);
				if ((mConnectionState == ConnectionState.READY)
					|| (mConnectionState == ConnectionState.CONNECTED)) {

					try {
						executor.execute(() -> {
							handleLongPoll(call, response);
						});
						recall(call);
					} catch (final Exception e) {
						reportError(e);
					}
				} else {
					Log.w(TAG, "unexpected state:" + mConnectionState);
				}
			}
			
			@Override
			public void onFailure(@NonNull final Call<ResponseBody> call, @NonNull final Throwable t) {
				if (DEBUG) Log.v(TAG, "longPoll:onFailure=" + t);
				removeCall(call);
				// FIXME タイムアウトの時は再度long pollする？
				if (!(t instanceof IOException) || !"Canceled".equals(t.getMessage())) {
					reportError(t);
				}
				if (mConnectionState != ConnectionState.ERROR) {
					recall(call);
				}
			}
			
			private void recall(final Call<ResponseBody> call) {
				final Call<ResponseBody> newCall = call.clone();
				addCall(newCall);
				newCall.enqueue(this);
			}
		});
	}

	/**
	 * long pollによるjanus-gatewayサーバーからの受信イベントの処理の実体
	 * @param call
	 * @param response
	 */
	private void handleLongPoll(@NonNull final Call<ResponseBody> call,
		@NonNull final Response<ResponseBody> response) {
		
		if (DEBUG) Log.v(TAG, "handleLongPoll:");
		final ResponseBody responseBody = response.body();
		if (response.isSuccessful() && (responseBody != null)) {
			try {
				@NonNull
				final JSONObject body = new JSONObject(responseBody.string());
				@Nullable
				final String transaction = body.optString("transaction");
				final long sender = body.optLong("sender");
				if (!TextUtils.isEmpty(transaction)) {
					// トランザクションコールバックでの処理を試みる
					// WebRTCイベントはトランザクションがない
					if (TransactionManager.handleTransaction(transaction, body)) {
						return;	// 処理済みの時はここで終了
					}
				}
				@Nullable
				final VideoRoomPlugin plugin = getPlugin(sender);
				if (plugin != null) {
					if (DEBUG) Log.v(TAG, "handlePluginEvent: try handle message on plugin specified by sender");
					if (plugin.onReceived("", body)) {
						return;
					}
				}

				if (DEBUG) Log.v(TAG, "handleLongPoll:unhandled transaction");
				final String janus = body.optString("janus");
				if (!TextUtils.isEmpty(janus)) {
					switch (janus) {
					case "ack":
						// do nothing
						return;
					case "keepalive":
						// サーバー側がタイムアウト(30秒？)した時は{"janus": "keepalive"}が来る
						// do nothing
						return;
					case "event":
						// プラグインイベント
						handlePluginEvent(body);
						break;
					case "media":
					case "webrtcup":
					case "slowlink":
					case "hangup":
						// event for WebRTC
						handleWebRTCEvent(body);
						break;
					case "error":
						reportError(new RuntimeException("error response " + response));
						break;
					default:
						Log.d(TAG, "handleLongPoll:unknown event:" + body);
						break;
					}
				}
			} catch (final JSONException | IOException e) {
				reportError(e);
			}
		}
	}

	/**
	 * プラグインイベントの処理
	 * @param body
	 */
	private void handlePluginEvent(@NonNull final JSONObject body) {
		if (DEBUG) Log.v(TAG, "handlePluginEvent:" + body);
		final Gson gson = new Gson();
		final RoomEvent event = gson.fromJson(body.toString(), RoomEvent.class);

		if (DEBUG) Log.v(TAG, "handlePluginEvent: unhandled event");
	}
	
	private void handleOnJoin(@NonNull final VideoRoomPlugin plugin,
		final RoomEvent room) {

		if (DEBUG) Log.v(TAG, "handleOnJoin:");
		// roomにjoinできた
		// Fire connection and signaling events.
		mCallback.onConnectedToRoom(true, room.plugindata.data);
	}

	/**
	 * WebRTC関係のメッセージの処理
	 * @param body
	 */
	private void handleWebRTCEvent(@NonNull final JSONObject body) {
		if (DEBUG) Log.v(TAG, "handleWebRTCEvent:" + body);
		switch (body.optString("janus")) {
		case "media":
		case "webrtcup":
		case "slowlink":
			mCallback.onEvent(body);
			break;
		case "hangup":
			mCallback.onChannelClose();
			break;
		default:
			break;
		}
	}

}
