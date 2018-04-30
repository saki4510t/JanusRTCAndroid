package com.serenegiant.janus;

import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.serenegiant.janus.request.Creator;
import com.serenegiant.janus.request.Destroy;
import com.serenegiant.janus.response.EventRoom;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;

import org.appspot.apprtc.PeerConnectionParameters;
import org.appspot.apprtc.RecordedAudioToFileController;
import org.appspot.apprtc.RoomConnectionParameters;
import org.appspot.apprtc.RtcEventLog;
import org.appspot.apprtc.SignalingParameters;
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
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.LegacyAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.serenegiant.janus.Const.*;
import static org.appspot.apprtc.AppRTCConst.*;

public class JanusRTCClient implements JanusClient {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = JanusRTCClient.class.getSimpleName();
	
	private static enum ConnectionState {
		UNINITIALIZED,
		READY,	// janus-gateway server is ready to access
		CONNECTED,
		CLOSED,
		ERROR }

	/**
	 * Executor thread is started once in private ctor and is used for all
	 * peer connection API calls to ensure new peer connection factory is
	 * created on the same thread as previously destroyed factory.
	 */
	static final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final Object mSync = new Object();
	private final WeakReference<Context> mWeakContext;
	@NonNull
	private final EglBase rootEglBase;
	@NonNull
	private final PeerConnectionParameters peerConnectionParameters;
	@NonNull
	private final String baseUrl;
	@NonNull
	private final JanusCallback mCallback;
	private final boolean dataChannelEnabled;
//--------------------------------------------------------------------------------
	private final Timer statsTimer = new Timer();
	@Nullable
	private PeerConnectionFactory factory;
	private boolean preferIsac;
	private boolean videoCapturerStopped;
	private boolean isError;
	/**
	 * Implements the WebRtcAudioRecordSamplesReadyCallback interface and writes
	 * recorded audio samples to an output file.
	 */
	@Nullable
	private RecordedAudioToFileController saveRecordedAudioToFile = null;
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
	private List<VideoRenderer.Callbacks> remoteRenders;
	@Nullable
	private AudioSource audioSource;
	@Nullable
	private AudioTrack localAudioTrack;
	private int videoWidth;
	private int videoHeight;
	private int videoFps;
	private MediaConstraints audioConstraints;
	private MediaConstraints sdpMediaConstraints;
	/** Enable org.appspot.apprtc.RtcEventLog. */
	/**
	 * Queued remote ICE candidates are consumed only after both local and
	 * remote descriptions are set. Similarly local ICE candidates are sent to
	 * remote peer after both local and remote description are set.
	 */
	@Nullable
	private List<IceCandidate> queuedRemoteCandidates;
	private boolean isInitiator;
	/** enableAudio is set to true if audio should be sent. */
	private boolean enableAudio = true;
	/**
	 * enableVideo is set to true if video should be rendered and sent.
	 */
	private boolean renderVideo = true;
//--------------------------------------------------------------------------------

	private VideoRoom mJanus;
	private LongPoll mLongPoll;
	private final List<Call<?>> mCurrentCalls = new ArrayList<>();
	private final Map<BigInteger, JanusPlugin> mAttachedPlugins
		= new ConcurrentHashMap<BigInteger, JanusPlugin>();
	private RoomConnectionParameters connectionParameters;
	private ConnectionState mConnectionState;
	private ServerInfo mServerInfo;
	private Session mSession;

	public JanusRTCClient(@NonNull final Context appContext,
		@NonNull final EglBase eglBase,
		@NonNull final PeerConnectionParameters peerConnectionParameters,
		@NonNull final JanusCallback callback,
		@NonNull final String baseUrl) {

		this.mWeakContext = new WeakReference<>(appContext);
		this.rootEglBase = eglBase;
		this.peerConnectionParameters = peerConnectionParameters;
		this.mCallback = callback;
		this.baseUrl = baseUrl;

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
					.setEnableVideoHwAcceleration(peerConnectionParameters.videoCodecHwAcceleration)
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
	
	public void release() {
		disconnectFromRoom();
	}

//================================================================================
// implementations of com.serenegiant.janus.JanusClient interface

	/**
	 * This function should only be called once.
	 */
	public void createPeerConnectionFactory(
		@Nullable final PeerConnectionFactory.Options options) {

		if (DEBUG) Log.v(TAG, "createPeerConnectionFactory:");
		if (factory != null) {
			throw new IllegalStateException("PeerConnectionFactory has already been constructed");
		}
		executor.execute(() -> createPeerConnectionFactoryInternal(options));
	}
	
	/**
	 * Publisher用のPeerConnectionを生成する
	 * @param localRender
	 * @param remoteRenders
	 * @param videoCapturer
	 */
	public void createPeerConnection(final VideoSink localRender,
		final List<VideoRenderer.Callbacks> remoteRenders,
		final VideoCapturer videoCapturer) {

		if (DEBUG) Log.v(TAG, "createPeerConnection:");
		this.localRender = localRender;
		this.remoteRenders = remoteRenders;
		this.videoCapturer = videoCapturer;
		executor.execute(() -> {
			try {
				createMediaConstraintsInternal();
				createPeerConnectionInternal();
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
				} catch (InterruptedException e) {
				}
				videoCapturerStopped = true;
			}
		});
	}
	
	@Override
	public void setVideoMaxBitrate(final int maxBitrateKbps) {
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
			
			RtpParameters parameters = localVideoSender.getParameters();
			if (parameters.encodings.size() == 0) {
				Log.w(TAG, "RtpParameters are not ready.");
				return;
			}
			
			for (RtpParameters.Encoding encoding : parameters.encodings) {
				// Null value means no limit.
				encoding.maxBitrateBps = maxBitrateKbps == 0 ? null : maxBitrateKbps * BPS_IN_KBPS;
			}
			if (!localVideoSender.setParameters(parameters)) {
				Log.e(TAG, "RtpSender.setParameters failed.");
			}
			if (DEBUG) Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
		});
	}

	@Override
	public void enableStatsEvents(boolean enable, int periodMs) {
		if (DEBUG) Log.v(TAG, "enableStatsEvents:");
		if (enable) {
			try {
				statsTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						executor.execute(() -> getStats());
					}
				}, 0, periodMs);
			} catch (Exception e) {
				Log.e(TAG, "Can not schedule statistics timer", e);
			}
		} else {
			statsTimer.cancel();
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
			// FIXME
//			if (remoteVideoTrack != null) {
//				remoteVideoTrack.setEnabled(renderVideo);
//			}
		});
	}

	@Override
	public void connectToRoom(final RoomConnectionParameters connectionParameters) {
		if (DEBUG) Log.v(TAG, "connectToRoom:");
		this.connectionParameters = connectionParameters;
		executor.execute(() -> {
			connectToRoomInternal();
		});
	}
	
	@Deprecated
	@Override
	public void sendOfferSdp(final SessionDescription sdp) {
		if (DEBUG) Log.v(TAG, "sendOfferSdp:" + sdp);
		executor.execute(() -> {
			sendOfferSdpInternal(sdp);
		});
	}
	
	@Deprecated
	@Override
	public void sendAnswerSdp(final SessionDescription sdp) {
		if (DEBUG) Log.v(TAG, "sendAnswerSdp:" + sdp);
		executor.execute(() -> {
			sendAnswerSdpInternal(sdp);
		});
	}
	
	@Deprecated
	@Override
	public void sendLocalIceCandidate(final IceCandidate candidate) {
		if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:");
		executor.execute(() -> {
			sendLocalIceCandidateInternal(candidate);
		});
	}
	
	@Deprecated
	@Override
	public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
		if (DEBUG) Log.v(TAG, "sendLocalIceCandidateRemovals:");
		executor.execute(() -> {
			// FIXME 未実装
//			final JSONObject json = new JSONObject();
//			jsonPut(json, "type", "remove-candidates");
//			final JSONArray jsonArray = new JSONArray();
//			for (final IceCandidate candidate : candidates) {
//				jsonArray.put(toJsonCandidate(candidate));
//			}
//			jsonPut(json, "candidates", jsonArray);
//			if (initiator) {
//				// Call initiator sends ice candidates to GAE server.
//				if (mConnectionState != WebSocketRTCClient.ConnectionState.CONNECTED) {
//					reportError("Sending ICE candidate removals in non connected state.");
//					return;
//				}
//				sendPostMessage(WebSocketRTCClient.MessageType.MESSAGE, messageUrl, json.toString());
//				if (connectionParameters.loopback) {
//					events.onRemoteIceCandidatesRemoved(candidates);
//				}
//			} else {
//				// Call receiver sends ice candidates to websocket server.
//				wsClient.send(json.toString());
//			}
		});
	}
	
	@Override
	public void disconnectFromRoom() {
		if (DEBUG) Log.v(TAG, "disconnectFromRoom:");
		cancelCall();
		executor.execute(() -> {
			disconnectFromRoomInternal();
		});
	}

//================================================================================
	private void createPeerConnectionFactoryInternal(
		@Nullable final PeerConnectionFactory.Options options) {
	
		if (DEBUG) Log.v(TAG, "createPeerConnectionFactoryInternal:");
		isError = false;
		
		if (peerConnectionParameters.tracing) {
			PeerConnectionFactory.startInternalTracingCapture(
				Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
					+ "webrtc-trace.txt");
		}
		
		// Check if ISAC is used by default.
		preferIsac = peerConnectionParameters.audioCodec != null
			&& peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC);
		
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
		
		final AudioDeviceModule adm = peerConnectionParameters.useLegacyAudioDevice
			? createLegacyAudioDevice()
			: createJavaAudioDevice();
		
		// Create peer connection factory.
		if (options != null && DEBUG) {
			if (DEBUG) Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
		}
		final boolean enableH264HighProfile =
			VIDEO_CODEC_H264_HIGH.equals(peerConnectionParameters.videoCodec);
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
		if (DEBUG) Log.d(TAG, "Peer connection factory created.");
	}

	@SuppressWarnings("deprecation")
	private AudioDeviceModule createLegacyAudioDevice() {
		if (DEBUG) Log.v(TAG, "createLegacyAudioDevice:");
		// Enable/disable OpenSL ES playback.
		if (!peerConnectionParameters.useOpenSLES) {
			if (DEBUG) Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
			WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
		} else {
			if (DEBUG) Log.d(TAG, "Allow OpenSL ES audio if device supports it");
			WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
		}
		
		if (peerConnectionParameters.disableBuiltInAEC) {
			if (DEBUG) Log.d(TAG, "Disable built-in AEC even if device supports it");
			WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
		} else {
			if (DEBUG) Log.d(TAG, "Enable built-in AEC if device supports it");
			WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
		}
		
		if (peerConnectionParameters.disableBuiltInNS) {
			if (DEBUG) Log.d(TAG, "Disable built-in NS even if device supports it");
			WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
		} else {
			if (DEBUG) Log.d(TAG, "Enable built-in NS if device supports it");
			WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
		}
		
		WebRtcAudioRecord.setOnAudioSamplesReady(saveRecordedAudioToFile);
		
		// Set audio record error callbacks.
		WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecord.WebRtcAudioRecordErrorCallback() {
			@Override
			public void onWebRtcAudioRecordInitError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioRecordStartError(
				final WebRtcAudioRecord.AudioRecordStartErrorCode errorCode,
					final String errorMessage) {

				Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioRecordError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
		});
		
		WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.ErrorCallback() {
			@Override
			public void onWebRtcAudioTrackInitError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioTrackStartError(
				final WebRtcAudioTrack.AudioTrackStartErrorCode errorCode,
					final String errorMessage) {

				Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
			
			@Override
			public void onWebRtcAudioTrackError(final String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
				reportError(new RuntimeException(errorMessage));
			}
		});
		
		return new LegacyAudioDeviceModule();
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
				videoWidth = HD_VIDEO_WIDTH;
				videoHeight = HD_VIDEO_HEIGHT;
			}
			
			// If fps is not specified, default to 30.
			if (videoFps == 0) {
				videoFps = 30;
			}
			Logging.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps);
		}
		
		// Create audio constraints.
		audioConstraints = new MediaConstraints();
		// added for audio performance measurements
		if (peerConnectionParameters.noAudioProcessing) {
			if (DEBUG) Log.d(TAG, "Disabling audio processing");
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
			audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
		}
		// Create SDP constraints.
		sdpMediaConstraints = new MediaConstraints();
		sdpMediaConstraints.mandatory.add(
			new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
			"OfferToReceiveVideo", Boolean.toString(isVideoCallEnabled())));
	}
	
	private void createPeerConnectionInternal() {
		if (DEBUG) Log.v(TAG, "createPeerConnectionInternal:");
		final Context context = getContext();
		if ((context == null) || (factory == null) || isError) {
			Log.e(TAG, "Peerconnection factory is not created");
			return;
		}
		if (DEBUG) Log.d(TAG, "Create peer connection.");
		
		PeerConnection peerConnection = null;
		DataChannel dataChannel = null;
		queuedRemoteCandidates = new ArrayList<>();
		
		if (isVideoCallEnabled()) {
			factory.setVideoHwAccelerationOptions(
				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
		}
		
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
		// Enable DTLS for normal calls and disable for loopback calls.
		rtcConfig.enableDtlsSrtp = !peerConnectionParameters.loopback;
		rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
		
		peerConnection = factory.createPeerConnection(rtcConfig, mPeerConnectionObserver);
		
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
		isInitiator = false;
		
		// Set INFO libjingle logging.
		// NOTE: this _must_ happen while |factory| is alive!
		Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO);
		
		final List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
		if (isVideoCallEnabled()) {
			peerConnection.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels);
			// Publisherは送信のみなのでリモートビデオトラックは不要
//			// We can add the renderers right away because we don't need to wait for an
//			// answer to get the remote track.
//			remoteVideoTrack = getRemoteVideoTrack();
//			remoteVideoTrack.setEnabled(renderVideo);
//			for (VideoRenderer.Callbacks remoteRender : remoteRenders) {
//				remoteVideoTrack.addRenderer(new VideoRenderer(remoteRender));
//			}
		}
		peerConnection.addTrack(createAudioTrack(), mediaStreamLabels);
		if (isVideoCallEnabled()) {
			findVideoSender(peerConnection);
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
		// FIXME ここでPeerConnection等を渡してPublisherを生成する
		final JanusPlugin.Publisher publisher
			= new JanusPlugin.Publisher(
				mJanus, mSession, mJanusPluginCallback,
				peerConnection, dataChannel, rtcEventLog);
		// ローカルのPublisherは1つしかないので検索の手間を省くために
		// BigInteger.ZEROをキーとして登録しておく。
		// attachした時点で実際のプラグインのidでも登録される
		addPlugin(BigInteger.ZERO, publisher);
		publisher.attach();
	}

	private File createRtcEventLogOutputFile() {
		if (DEBUG) Log.v(TAG, "createRtcEventLogOutputFile:");
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault());
		Date date = new Date();
		final String outputFileName = "event_log_" + dateFormat.format(date) + ".log";
		return new File(
			getContext().getDir(RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE), outputFileName);
	}

	@Nullable
	private AudioTrack createAudioTrack() {
		if (DEBUG) Log.v(TAG, "createAudioTrack:");
		audioSource = factory.createAudioSource(audioConstraints);
		localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
		localAudioTrack.setEnabled(enableAudio);
		return localAudioTrack;
	}
	
	@Nullable
	private VideoTrack createVideoTrack(final VideoCapturer capturer) {
		if (DEBUG) Log.v(TAG, "createVideoTrack:");
		videoSource = factory.createVideoSource(capturer);
		capturer.startCapture(videoWidth, videoHeight, videoFps);
		
		localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
		localVideoTrack.setEnabled(renderVideo);
		localVideoTrack.addSink(localRender);
		return localVideoTrack;
	}
	
	private void findVideoSender(@NonNull final PeerConnection peerConnection) {
		if (DEBUG) Log.v(TAG, "findVideoSender:");
		for (RtpSender sender : peerConnection.getSenders()) {
			if (sender.track() != null) {
				String trackType = sender.track().kind();
				if (trackType.equals(VIDEO_TRACK_TYPE)) {
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
		for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
			MediaStreamTrack track = transceiver.getReceiver().track();
			if (track instanceof VideoTrack) {
				return (VideoTrack) track;
			}
		}
		return null;
	}
	
	private void drainCandidates(@NonNull final PeerConnection peerConnection) {
		if (DEBUG) Log.v(TAG, "drainCandidates:");
		if (queuedRemoteCandidates != null) {
			if (DEBUG) Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
			for (IceCandidate candidate : queuedRemoteCandidates) {
				peerConnection.addIceCandidate(candidate);
			}
			queuedRemoteCandidates = null;
		}
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

	private final PeerConnection.Observer
		mPeerConnectionObserver = new PeerConnection.Observer() {
		@Override
		public void onSignalingChange(final PeerConnection.SignalingState newState) {
			if (DEBUG) Log.v(TAG, "onSignalingChange:" + newState);
		}
		
		@Override
		public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
			if (DEBUG) Log.v(TAG, "onIceConnectionChange:" + newState);
		}
		
		@Override
		public void onIceConnectionReceivingChange(final boolean receiving) {
			if (DEBUG) Log.v(TAG, "onIceConnectionReceivingChange:receiving=" + receiving);
		}
		
		@Override
		public void onIceGatheringChange(final PeerConnection.IceGatheringState newState) {
			if (DEBUG) Log.v(TAG, "onIceGatheringChange:" + newState);
		}
		
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			if (DEBUG) Log.v(TAG, "onIceCandidate:");
		}
		
		@Override
		public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
			if (DEBUG) Log.v(TAG, "onIceCandidatesRemoved:");
		}
		
		@Override
		public void onAddStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onAddStream:");
		}
		
		@Override
		public void onRemoveStream(final MediaStream stream) {
			if (DEBUG) Log.v(TAG, "onRemoveStream:");
		}
		
		@Override
		public void onDataChannel(final DataChannel channel) {
			if (DEBUG) Log.v(TAG, "onDataChannel:");
		}
		
		@Override
		public void onRenegotiationNeeded() {
			if (DEBUG) Log.v(TAG, "onRenegotiationNeeded:");
		}
		
		@Override
		public void onAddTrack(final RtpReceiver receiver, final MediaStream[] streams) {
			if (DEBUG) Log.v(TAG, "onAddTrack:");
		}
	};

//--------------------------------------------------------------------------------
	@Nullable
	private Context getContext() {
		return mWeakContext.get();
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
	private void addPlugin(@NonNull final BigInteger key, @NonNull final JanusPlugin plugin) {
		synchronized (mAttachedPlugins) {
			mAttachedPlugins.put(key, plugin);
		}
	}

	private void removePlugin(@NonNull final JanusPlugin plugin) {
		final BigInteger key = plugin instanceof JanusPlugin.Publisher
			? BigInteger.ZERO
			: ((JanusPlugin.Subscriber)plugin).feederId;
		
		executor.execute(() -> {
			synchronized (mAttachedPlugins) {
				mAttachedPlugins.remove(key);
			}
		});
	}

	private void removePlugin(@NonNull final BigInteger key) {
		executor.execute(() -> {
			synchronized (mAttachedPlugins) {
				mAttachedPlugins.remove(key);
			}
		});
	}

	private JanusPlugin getPlugin(@Nullable final BigInteger key) {
		synchronized (mAttachedPlugins) {
			if ((key != null) && mAttachedPlugins.containsKey(key)) {
				return mAttachedPlugins.get(key);
			}
		}
		return null;
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
					mCallback.onChannelError(t.getMessage());
				}
			});
		} catch (final Exception e) {
			// ignore, will be already released.
		}
	}

	/**
	 * Connects to room - function runs on a local looper thread.
	 */
	private void connectToRoomInternal() {
		if (DEBUG) Log.v(TAG, "connectToRoomInternal:");
		// 通常のRESTアクセス用APIインターフェースを生成
		mJanus = setupRetrofit(
			setupHttpClient(HTTP_READ_TIMEOUT_MS, HTTP_WRITE_TIMEOUT_MS),
			baseUrl).create(VideoRoom.class);
		// long poll用APIインターフェースを生成
		mLongPoll = setupRetrofit(
			setupHttpClient(HTTP_READ_TIMEOUT_MS_LONG_POLL, HTTP_WRITE_TIMEOUT_MS),
			baseUrl).create(LongPoll.class);
		executor.execute(() -> {
			requestServerInfo();
		});
	}

	/**
	 * Disconnect from room and send bye messages - runs on a local looper thread.
	 */
	private void disconnectFromRoomInternal() {
		if (DEBUG) Log.v(TAG, "disconnectFromRoomInternal:state=" + mConnectionState);
		cancelCall();
		if ((mConnectionState == ConnectionState.CONNECTED)) {

			if (DEBUG) Log.d(TAG, "Closing room.");
			detach();
		}
		destroy();
	}
	
	private void sendOfferSdpInternal(final SessionDescription sdp) {
		if (DEBUG) Log.v(TAG, "sendOfferSdpInternal:");
		if (mConnectionState != ConnectionState.CONNECTED) {
			reportError(new RuntimeException("Sending offer SDP in non connected state."));
			return;
		}
		final JanusPlugin plugin;
		synchronized (mAttachedPlugins) {
			plugin = mAttachedPlugins.containsKey(BigInteger.ZERO)
				? mAttachedPlugins.get(BigInteger.ZERO) : null;
		}
		if (plugin != null) {
			plugin.sendOfferSdp(sdp, connectionParameters.loopback);
		} else {
			reportError(new IllegalStateException("publisher not found"));
		}
	}
	
	private void sendAnswerSdpInternal(final SessionDescription sdp) {
		if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:");
		if (connectionParameters.loopback) {
			Log.e(TAG, "Sending answer in loopback mode.");
			return;
		}
		final JanusPlugin plugin;
		synchronized (mAttachedPlugins) {
			plugin = mAttachedPlugins.containsKey(BigInteger.ZERO)
				? mAttachedPlugins.get(BigInteger.ZERO) : null;
		}
		if (plugin != null) {
			plugin.sendAnswerSdp(sdp, connectionParameters.loopback);
		} else {
			reportError(new IllegalStateException("publisher not found"));
		}
	}

	/**
	 * sendLocalIceCandidateの実体、ワーカースレッド上で実行
	 * @param candidate
	 */
	private void sendLocalIceCandidateInternal(final IceCandidate candidate) {
		if (DEBUG) Log.v(TAG, "sendLocalIceCandidateInternal:");
		if (mConnectionState != ConnectionState.CONNECTED) {
			if (DEBUG) Log.d(TAG, "already disconnected");
			return;
		}
		final JanusPlugin plugin;
		synchronized (mAttachedPlugins) {
			plugin = mAttachedPlugins.containsKey(BigInteger.ZERO)
				? mAttachedPlugins.get(BigInteger.ZERO) : null;
		}
		if (plugin != null) {
			plugin.sendLocalIceCandidate(candidate,
				connectionParameters.loopback);
		} else {
			reportError(new IllegalStateException("there is no publisher"));
		}
	}
	
//--------------------------------------------------------------------
	private void requestServerInfo() {
		if (DEBUG) Log.v(TAG, "requestServerInfo:");
		// Janus-gatewayサーバー情報を取得
		final Call<ServerInfo> call = mJanus.getInfo();
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
		final Call<Session> call = mJanus.create(new Creator());
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
							mCallback.onConnectServer(JanusRTCClient.this);
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
	 * detach from VideoRoom plugin
	 */
	private void detach() {
		if (DEBUG) Log.v(TAG, "detach:");
		cancelCall();
		mConnectionState = ConnectionState.CLOSED;
		synchronized (mAttachedPlugins) {
			for (final Map.Entry<BigInteger, JanusPlugin> entry:
				mAttachedPlugins.entrySet()) {

				// key === BigInteger.ZEROはPublisherのキーの別名で、
				// 本当のidでも別途登録されているはずなのでここでは呼ばない
				if (!BigInteger.ZERO.equals(entry.getKey())) {
					entry.getValue().detach();
				}
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
		detach();
		if (mSession != null) {
			final Destroy destroy = new Destroy(mSession, null);
			final Call<Void> call = mJanus.destroy(mSession.id(), destroy);
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
		statsTimer.cancel();
		if (DEBUG) Log.d(TAG, "Closing audio source.");
		if (audioSource != null) {
			audioSource.dispose();
			audioSource = null;
		}
		if (DEBUG) Log.d(TAG, "Stopping capture.");
		if (videoCapturer != null) {
			try {
				videoCapturer.stopCapture();
			} catch (InterruptedException e) {
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
		remoteRenders = null;
		if (factory != null && peerConnectionParameters.aecDump) {
			factory.stopAecDump();
		}
		if (videoSource != null) {
			videoSource.dispose();
			videoSource = null;
		}
		if (DEBUG) Log.d(TAG, "Closing peer connection factory.");
		if (factory != null) {
			factory.dispose();
			factory = null;
		}
		rootEglBase.release();
		if (DEBUG) Log.d(TAG, "Closing peer connection done.");
		PeerConnectionFactory.stopInternalTracingCapture();
		PeerConnectionFactory.shutdownInternalTracer();
	}

	/**
	 * JanusPluginからのコールバックリスナーの実装
	 */
	private final JanusPlugin.JanusPluginCallback mJanusPluginCallback
		= new JanusPlugin.JanusPluginCallback() {
		@Override
		public void onAttach(@NonNull final JanusPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onAttach:" + plugin);
			addPlugin(plugin.id(), plugin);
		}
		
		@Override
		public void onJoin(@NonNull final JanusPlugin plugin,
			final EventRoom room) {

			if (DEBUG) Log.v(TAG, "onJoin:" + plugin);
			if (plugin instanceof JanusPlugin.Publisher) {
				mConnectionState = ConnectionState.CONNECTED;
				handleOnJoin(room);	// FIXME publisherの時はhandleJoin呼んじゃダメかも
			} else if (plugin instanceof JanusPlugin.Subscriber) {
//				mConnectionState = ConnectionState.CONNECTED;
//				handleOnJoin(room);
			}
		}
		
		@Override
		public void onDetach(@NonNull final JanusPlugin plugin) {
			if (DEBUG) Log.v(TAG, "onDetach:" + plugin);
			removePlugin(plugin);
		}
		
		@Override
		public void onLeave(@NonNull final JanusPlugin plugin,
			@NonNull final BigInteger pluginId) {
			
			if (DEBUG) Log.v(TAG, "onLeave:" + plugin + ",leave=" + pluginId);
		}
		
		@Override
		public void onRemoteIceCandidate(@NonNull final JanusPlugin plugin,
			final IceCandidate candidate) {

			if (DEBUG) Log.v(TAG, "onRemoteIceCandidate:" + plugin
				+ "\n" + candidate);
			mCallback.onRemoteIceCandidate(candidate);
		}
		
		@Override
		public void onRemoteDescription(@NonNull final JanusPlugin plugin,
			final SessionDescription sdp) {
			
			if (DEBUG) Log.v(TAG, "onRemoteDescription:" + plugin
				+ "\n" + sdp);
			if (plugin instanceof JanusPlugin.Publisher) {
				mCallback.onRemoteDescription(sdp);
			} else if (plugin instanceof JanusPlugin.Subscriber) {
				if (sdp.type == SessionDescription.Type.ANSWER) {
					mCallback.onRemoteDescription(sdp);
				} else {
					final SessionDescription answerSdp
						= new SessionDescription(SessionDescription.Type.ANSWER,
							sdp.description);
					mCallback.onRemoteDescription(sdp);
				}
			}
		}
		
		@Override
		public void onError(@NonNull final JanusPlugin plugin,
			@NonNull final Throwable t) {

			reportError(t);
		}
	};

	/**
	 * TransactionManagerからのコールバック
	 */
	private final TransactionManager.TransactionCallback mTransactionCallback
		= new TransactionManager.TransactionCallback() {
		@Override
		public boolean onReceived(@NonNull final String transaction,
			final JSONObject json) {

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
		final Call<ResponseBody> call = mLongPoll.getEvent(mSession.id());
		addCall(call);
		call.enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(@NonNull final Call<ResponseBody> call,
				@NonNull final Response<ResponseBody> response) {

				if (DEBUG) Log.v(TAG, "longPoll:onResponse");
				removeCall(call);
				if ((mConnectionState != ConnectionState.ERROR)
					&& (mConnectionState != ConnectionState.CLOSED)
					&& (mConnectionState != ConnectionState.UNINITIALIZED)) {

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
				final JSONObject body = new JSONObject(responseBody.string());
				final String transaction = body.optString("transaction");
				if (!TextUtils.isEmpty(transaction)) {
					// トランザクションコールバックでの処理を試みる
					// WebRTCイベントはトランザクションがない
					if (TransactionManager.handleTransaction(transaction, body)) {
						return;	// 処理済みの時はここで終了
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
	private void handlePluginEvent(final JSONObject body) {
		if (DEBUG) Log.v(TAG, "handlePluginEvent:" + body);
		final Gson gson = new Gson();
		final EventRoom event = gson.fromJson(body.toString(), EventRoom.class);

		// Senderフィールドは対象のプラグインのidなので対応するプラグインを探して実行を試みる
		final JanusPlugin plugin = getPlugin(event.sender);
		if (plugin != null) {
			if (DEBUG) Log.v(TAG, "handlePluginEvent: try handle message on plugin specified by sender");
			if (plugin.onReceived("", body)) return;
		}
		
		if (DEBUG) Log.v(TAG, "handlePluginEvent: unhandled event");
	}
	
	private void handleOnJoin(final EventRoom room) {
		if (DEBUG) Log.v(TAG, "handleOnJoin:");
		// roomにjoinできた
		final SignalingParameters params = new SignalingParameters(
			mCallback.getIceServers(this),
				true,						// initiator=trueならこの端末側がofferする
				room.plugindata.data.id.toString(),	// clientId
				null, null,
				null, null);	// この2つはinitiator=falseの時有効
		// Fire connection and signaling parameters events.
		mCallback.onConnectedToRoom(params);
	}

	/**
	 * WebRTC関係のメッセージの処理
	 * @param body
	 */
	private void handleWebRTCEvent(final JSONObject body) {
		if (DEBUG) Log.v(TAG, "handleWebRTCEvent:" + body);
		switch (body.optString("janus")) {
		case "media":
		case "webrtcup":
		case "slowlink":
			break;
		case "hangup":
			mCallback.onChannelClose();
			break;
		default:
			break;
		}
	}

//================================================================================
	/**
	 * keep first OkHttpClient as singleton
	 */
	private static OkHttpClient sOkHttpClient;
	/**
	 * Janus-gatewayサーバーとの通信用のOkHttpClientインスタンスの初期化処理
	 * @return
	 */
	private synchronized OkHttpClient setupHttpClient(
		final long readTimeoutMs, final long writeTimeoutMs) {
	
		if (DEBUG) Log.v(TAG, "setupHttpClient:");

		final OkHttpClient.Builder builder;
		if (sOkHttpClient == null) {
		 	builder = new OkHttpClient.Builder();
		} else {
			builder = sOkHttpClient.newBuilder();
		}
		builder
			.addInterceptor(new Interceptor() {
				@Override
				public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
	
					final Request original = chain.request();
					// header設定
					final Request request = original.newBuilder()
						.header("Accept", "application/json")
						.method(original.method(), original.body())
						.build();
	
					okhttp3.Response response = chain.proceed(request);
					return response;
				}
			})
			.connectTimeout(HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)	// 接続タイムアウト
			.readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)		// 読み込みタイムアウト
			.writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS);	// 書き込みタイムアウト
		
		// ログ出力設定
		if (DEBUG) {
			final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BODY);
			builder.addInterceptor(logging);
		}
		final OkHttpClient result = builder.build();
		if (sOkHttpClient == null) {
			sOkHttpClient = result;
		}
		return result;
	}
	
	/**
	 * Janus-gatewayサーバーとの通信用のRetrofitインスタンスの初期化処理
	 * @param client
	 * @param baseUrl
	 * @return
	 */
	private Retrofit setupRetrofit(@NonNull final OkHttpClient client,
		@NonNull final String baseUrl) {

		if (DEBUG) Log.v(TAG, "setupRetrofit:" + baseUrl);
		// JSONのパーサーとしてGsonを使う
		final Gson gson = new GsonBuilder()
//			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)	// IDENTITY
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();
		return new Retrofit.Builder()
			.baseUrl(baseUrl)
			.addConverterFactory(GsonConverterFactory.create(gson))
			.client(client)
			.build();
	}

}
