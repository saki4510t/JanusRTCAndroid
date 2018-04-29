package org.appspot.apprtc;/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.content.Context;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import org.appspot.apprtc.util.SdpUtils;
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
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule.AudioRecordErrorCallback;
import org.webrtc.audio.JavaAudioDeviceModule.AudioTrackErrorCallback;
import org.webrtc.audio.LegacyAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioRecord;
import org.webrtc.voiceengine.WebRtcAudioRecord.AudioRecordStartErrorCode;
import org.webrtc.voiceengine.WebRtcAudioRecord.WebRtcAudioRecordErrorCallback;
import org.webrtc.voiceengine.WebRtcAudioTrack;
import org.webrtc.voiceengine.WebRtcAudioTrack.AudioTrackStartErrorCode;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import static org.appspot.apprtc.AppRTCConst.*;

/**
 * Peer connection client implementation.
 * <p>
 * <p>All public methods are routed to local looper thread.
 * All PeerConnectionEvents callbacks are invoked from the same looper thread.
 * This class is a singleton.
 */
public class PeerConnectionClient {
	private static final boolean DEBUG = true;    // set false on production
	
	/**
	 * Executor thread is started once in private ctor and is used for all
	 * peer connection API calls to ensure new peer connection factory is
	 * created on the same thread as previously destroyed factory.
	 */
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private final PCObserver pcObserver = new PCObserver();
	private final SDPObserver sdpObserver = new SDPObserver();
	private final Timer statsTimer = new Timer();
	private final EglBase rootEglBase;
	private final Context appContext;
	private final PeerConnectionParameters peerConnectionParameters;
	private final PeerConnectionEvents events;
	
	@Nullable
	private PeerConnectionFactory factory;
	@Nullable
	private PeerConnection peerConnection;
	@Nullable
	private AudioSource audioSource;
	@Nullable
	private VideoSource videoSource;
	private boolean preferIsac;
	private boolean videoCapturerStopped;
	private boolean isError;
	@Nullable
	private VideoSink localRender;
	@Nullable
	private List<VideoRenderer.Callbacks> remoteRenders;
	private SignalingParameters signalingParameters;
	private int videoWidth;
	private int videoHeight;
	private int videoFps;
	private MediaConstraints audioConstraints;
	private MediaConstraints sdpMediaConstraints;
	/**
	 * Queued remote ICE candidates are consumed only after both local and
	 * remote descriptions are set. Similarly local ICE candidates are sent to
	 * remote peer after both local and remote description are set.
	 */
	@Nullable
	private List<IceCandidate> queuedRemoteCandidates;
	private boolean isInitiator;
	@Nullable
	private SessionDescription localSdp; // either offer or answer SDP
	@Nullable
	private VideoCapturer videoCapturer;
	/**
	 * enableVideo is set to true if video should be rendered and sent.
	 */
	private boolean renderVideo = true;
	@Nullable
	private VideoTrack localVideoTrack;
	@Nullable
	private VideoTrack remoteVideoTrack;
	@Nullable
	private RtpSender localVideoSender;
	/** enableAudio is set to true if audio should be sent. */
	private boolean enableAudio = true;
	@Nullable
	private AudioTrack localAudioTrack;
	@Nullable
	private DataChannel dataChannel;
	private final boolean dataChannelEnabled;
	/** Enable org.appspot.apprtc.RtcEventLog. */
	@Nullable
	private RtcEventLog rtcEventLog;
	/**
	 * Implements the WebRtcAudioRecordSamplesReadyCallback interface and writes
	 * recorded audio samples to an output file.
	 */
	@Nullable
	private RecordedAudioToFileController saveRecordedAudioToFile = null;
	
	/**
	 * Create a org.appspot.apprtc.PeerConnectionClient with the specified parameters.
	 * org.appspot.apprtc.PeerConnectionClient takes ownership of |eglBase|.
	 */
	public PeerConnectionClient(@NonNull final Context appContext,
		final EglBase eglBase,
		final PeerConnectionParameters peerConnectionParameters,
		final PeerConnectionEvents events) {

		this.rootEglBase = eglBase;
		this.appContext = appContext;
		this.events = events;
		this.peerConnectionParameters = peerConnectionParameters;
		this.dataChannelEnabled = peerConnectionParameters.dataChannelParameters != null;
		
		if (DEBUG) Log.d(TAG, "Preferred video codec: "
			+ peerConnectionParameters.getSdpVideoCodecName());
		
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
	
	/**
	 * This function should only be called once.
	 */
	public void createPeerConnectionFactory(PeerConnectionFactory.Options options) {
		if (factory != null) {
			throw new IllegalStateException("PeerConnectionFactory has already been constructed");
		}
		executor.execute(() -> createPeerConnectionFactoryInternal(options));
	}
	
	public void createPeerConnection(final VideoSink localRender,
									 final VideoRenderer.Callbacks remoteRender, final VideoCapturer videoCapturer,
									 final SignalingParameters signalingParameters) {
		if (peerConnectionParameters.videoCallEnabled && videoCapturer == null) {
			Log.w(TAG, "Video call enabled but no video capturer provided.");
		}
		createPeerConnection(
			localRender, Collections.singletonList(remoteRender), videoCapturer, signalingParameters);
	}
	
	public void createPeerConnection(final VideoSink localRender,
									 final List<VideoRenderer.Callbacks> remoteRenders, final VideoCapturer videoCapturer,
									 final SignalingParameters signalingParameters) {
		if (peerConnectionParameters == null) {
			Log.e(TAG, "Creating peer connection without initializing factory.");
			return;
		}
		this.localRender = localRender;
		this.remoteRenders = remoteRenders;
		this.videoCapturer = videoCapturer;
		this.signalingParameters = signalingParameters;
		executor.execute(() -> {
			try {
				createMediaConstraintsInternal();
				createPeerConnectionInternal();
				maybeCreateAndStartRtcEventLog();
			} catch (Exception e) {
				reportError("Failed to create peer connection: " + e.getMessage());
				throw e;
			}
		});
	}
	
	public void close() {
		if (DEBUG) Log.v(TAG, "close:");
		executor.execute(this::closeInternal);
	}
	
	private boolean isVideoCallEnabled() {
		return peerConnectionParameters.videoCallEnabled && videoCapturer != null;
	}
	
	private void createPeerConnectionFactoryInternal(PeerConnectionFactory.Options options) {
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
	
	private AudioDeviceModule createLegacyAudioDevice() {
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
		WebRtcAudioRecord.setErrorCallback(new WebRtcAudioRecordErrorCallback() {
			@Override
			public void onWebRtcAudioRecordInitError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioRecordStartError(
				AudioRecordStartErrorCode errorCode, String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioRecordError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
				reportError(errorMessage);
			}
		});
		
		WebRtcAudioTrack.setErrorCallback(new WebRtcAudioTrack.ErrorCallback() {
			@Override
			public void onWebRtcAudioTrackInitError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioTrackStartError(
				AudioTrackStartErrorCode errorCode, String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioTrackError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
				reportError(errorMessage);
			}
		});
		
		return new LegacyAudioDeviceModule();
	}
	
	private AudioDeviceModule createJavaAudioDevice() {
		// Enable/disable OpenSL ES playback.
		if (!peerConnectionParameters.useOpenSLES) {
			Log.w(TAG, "External OpenSLES ADM not implemented yet.");
			// TODO(magjed): Add support for external OpenSLES ADM.
		}
		
		// Set audio record error callbacks.
		AudioRecordErrorCallback audioRecordErrorCallback = new AudioRecordErrorCallback() {
			@Override
			public void onWebRtcAudioRecordInitError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioRecordStartError(
				JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioRecordError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
				reportError(errorMessage);
			}
		};
		
		AudioTrackErrorCallback audioTrackErrorCallback = new AudioTrackErrorCallback() {
			@Override
			public void onWebRtcAudioTrackInitError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioTrackStartError(
				JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
				reportError(errorMessage);
			}
			
			@Override
			public void onWebRtcAudioTrackError(String errorMessage) {
				Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
				reportError(errorMessage);
			}
		};
		
		return JavaAudioDeviceModule.builder(appContext)
			.setSamplesReadyCallback(saveRecordedAudioToFile)
			.setUseHardwareAcousticEchoCanceler(!peerConnectionParameters.disableBuiltInAEC)
			.setUseHardwareNoiseSuppressor(!peerConnectionParameters.disableBuiltInNS)
			.setAudioRecordErrorCallback(audioRecordErrorCallback)
			.setAudioTrackErrorCallback(audioTrackErrorCallback)
			.createAudioDeviceModule();
	}
	
	private void createMediaConstraintsInternal() {
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
		if (factory == null || isError) {
			Log.e(TAG, "Peerconnection factory is not created");
			return;
		}
		if (DEBUG) Log.d(TAG, "Create peer connection.");
		
		queuedRemoteCandidates = new ArrayList<>();
		
		if (isVideoCallEnabled()) {
			factory.setVideoHwAccelerationOptions(
				rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
		}
		
		PeerConnection.RTCConfiguration rtcConfig =
			new PeerConnection.RTCConfiguration(signalingParameters.iceServers);
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
		
		peerConnection = factory.createPeerConnection(rtcConfig, pcObserver);
		
		if (dataChannelEnabled) {
			DataChannel.Init init = new DataChannel.Init();
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
		
		List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
		if (isVideoCallEnabled()) {
			peerConnection.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels);
			// We can add the renderers right away because we don't need to wait for an
			// answer to get the remote track.
			remoteVideoTrack = getRemoteVideoTrack();
			remoteVideoTrack.setEnabled(renderVideo);
			for (VideoRenderer.Callbacks remoteRender : remoteRenders) {
				remoteVideoTrack.addRenderer(new VideoRenderer(remoteRender));
			}
		}
		peerConnection.addTrack(createAudioTrack(), mediaStreamLabels);
		if (isVideoCallEnabled()) {
			findVideoSender();
		}
		
		if (peerConnectionParameters.aecDump) {
			try {
				ParcelFileDescriptor aecDumpFileDescriptor =
					ParcelFileDescriptor.open(new File(Environment.getExternalStorageDirectory().getPath()
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
		if (DEBUG) Log.d(TAG, "Peer connection created.");
	}
	
	private File createRtcEventLogOutputFile() {
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmm_ss", Locale.getDefault());
		Date date = new Date();
		final String outputFileName = "event_log_" + dateFormat.format(date) + ".log";
		return new File(
			appContext.getDir(RTCEVENTLOG_OUTPUT_DIR_NAME, Context.MODE_PRIVATE), outputFileName);
	}
	
	private void maybeCreateAndStartRtcEventLog() {
		if (appContext == null || peerConnection == null) {
			return;
		}
		if (!peerConnectionParameters.enableRtcEventLog) {
			if (DEBUG) Log.d(TAG, "org.appspot.apprtc.RtcEventLog is disabled.");
			return;
		}
		rtcEventLog = new RtcEventLog(peerConnection);
		rtcEventLog.start(createRtcEventLogOutputFile());
	}
	
	private void closeInternal() {
		if (factory != null && peerConnectionParameters.aecDump) {
			factory.stopAecDump();
		}
		if (DEBUG) Log.d(TAG, "Closing peer connection.");
		statsTimer.cancel();
		if (dataChannel != null) {
			dataChannel.dispose();
			dataChannel = null;
		}
		if (rtcEventLog != null) {
			// org.appspot.apprtc.RtcEventLog should stop before the peer connection is disposed.
			rtcEventLog.stop();
			rtcEventLog = null;
		}
		if (peerConnection != null) {
			peerConnection.dispose();
			peerConnection = null;
		}
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
		if (DEBUG) Log.d(TAG, "Closing peer connection factory.");
		if (factory != null) {
			factory.dispose();
			factory = null;
		}
		rootEglBase.release();
		if (DEBUG) Log.d(TAG, "Closing peer connection done.");
		events.onPeerConnectionClosed();
		PeerConnectionFactory.stopInternalTracingCapture();
		PeerConnectionFactory.shutdownInternalTracer();
	}
	
	public boolean isHDVideo() {
		return isVideoCallEnabled() && videoWidth * videoHeight >= 1280 * 720;
	}
	
	@SuppressWarnings("deprecation") // TODO(sakal): getStats is deprecated.
	private void getStats() {
		if (peerConnection == null || isError) {
			return;
		}
		boolean success = peerConnection.getStats(new StatsObserver() {
			@Override
			public void onComplete(final StatsReport[] reports) {
				events.onPeerConnectionStatsReady(reports);
			}
		}, null);
		if (!success) {
			Log.e(TAG, "getStats() returns false!");
		}
	}
	
	public void enableStatsEvents(boolean enable, int periodMs) {
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
	
	public void setAudioEnabled(final boolean enable) {
		executor.execute(() -> {
			enableAudio = enable;
			if (localAudioTrack != null) {
				localAudioTrack.setEnabled(enableAudio);
			}
		});
	}
	
	public void setVideoEnabled(final boolean enable) {
		executor.execute(() -> {
			renderVideo = enable;
			if (localVideoTrack != null) {
				localVideoTrack.setEnabled(renderVideo);
			}
			if (remoteVideoTrack != null) {
				remoteVideoTrack.setEnabled(renderVideo);
			}
		});
	}
	
	public void createOffer() {
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC Create OFFER");
				isInitiator = true;
				peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
			}
		});
	}
	
	public void createAnswer() {
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC create ANSWER");
				isInitiator = false;
				peerConnection.createAnswer(sdpObserver, sdpMediaConstraints);
			}
		});
	}
	
	public void addRemoteIceCandidate(final IceCandidate candidate) {
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				if (queuedRemoteCandidates != null) {
					queuedRemoteCandidates.add(candidate);
				} else {
					peerConnection.addIceCandidate(candidate);
				}
			}
		});
	}
	
	public void removeRemoteIceCandidates(final IceCandidate[] candidates) {
		executor.execute(() -> {
			if (peerConnection == null || isError) {
				return;
			}
			// Drain the queued remote candidates if there is any so that
			// they are processed in the proper order.
			drainCandidates();
			peerConnection.removeIceCandidates(candidates);
		});
	}
	
	public void setRemoteDescription(final SessionDescription sdp) {
		executor.execute(() -> {
			if (peerConnection == null || isError) {
				return;
			}
			String sdpDescription = sdp.description;
			if (preferIsac) {
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
			}
			if (isVideoCallEnabled()) {
				sdpDescription =
					SdpUtils.preferCodec(sdpDescription, peerConnectionParameters.getSdpVideoCodecName(), false);
			}
			if (peerConnectionParameters.audioStartBitrate > 0) {
				sdpDescription = SdpUtils.setStartBitrate(
					AUDIO_CODEC_OPUS, false, sdpDescription, peerConnectionParameters.audioStartBitrate);
			}
			if (DEBUG) Log.d(TAG, "Set remote SDP.");
			SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
			peerConnection.setRemoteDescription(sdpObserver, sdpRemote);
		});
	}
	
	public void stopVideoSource() {
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
	
	public void startVideoSource() {
		executor.execute(() -> {
			if (videoCapturer != null && videoCapturerStopped) {
				if (DEBUG) Log.d(TAG, "Restart video source.");
				videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
				videoCapturerStopped = false;
			}
		});
	}
	
	public void setVideoMaxBitrate(@Nullable final Integer maxBitrateKbps) {
		executor.execute(() -> {
			if (peerConnection == null || localVideoSender == null || isError) {
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
				encoding.maxBitrateBps = maxBitrateKbps == null ? null : maxBitrateKbps * BPS_IN_KBPS;
			}
			if (!localVideoSender.setParameters(parameters)) {
				Log.e(TAG, "RtpSender.setParameters failed.");
			}
			if (DEBUG) Log.d(TAG, "Configured max video bitrate to: " + maxBitrateKbps);
		});
	}
	
	private void reportError(final String errorMessage) {
		Log.e(TAG, "Peerconnection error: " + errorMessage);
		executor.execute(() -> {
			if (!isError) {
				events.onPeerConnectionError(errorMessage);
				isError = true;
			}
		});
	}
	
	@Nullable
	private AudioTrack createAudioTrack() {
		audioSource = factory.createAudioSource(audioConstraints);
		localAudioTrack = factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
		localAudioTrack.setEnabled(enableAudio);
		return localAudioTrack;
	}
	
	@Nullable
	private VideoTrack createVideoTrack(VideoCapturer capturer) {
		videoSource = factory.createVideoSource(capturer);
		capturer.startCapture(videoWidth, videoHeight, videoFps);
		
		localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
		localVideoTrack.setEnabled(renderVideo);
		localVideoTrack.addSink(localRender);
		return localVideoTrack;
	}
	
	private void findVideoSender() {
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
	
	// Returns the remote VideoTrack, assuming there is only one.
	private @Nullable
	VideoTrack getRemoteVideoTrack() {
		for (RtpTransceiver transceiver : peerConnection.getTransceivers()) {
			MediaStreamTrack track = transceiver.getReceiver().track();
			if (track instanceof VideoTrack) {
				return (VideoTrack) track;
			}
		}
		return null;
	}
	
	private void drainCandidates() {
		if (queuedRemoteCandidates != null) {
			if (DEBUG) Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
			for (IceCandidate candidate : queuedRemoteCandidates) {
				peerConnection.addIceCandidate(candidate);
			}
			queuedRemoteCandidates = null;
		}
	}
	
	private void switchCameraInternal() {
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
	
	public void switchCamera() {
		executor.execute(this::switchCameraInternal);
	}
	
	public void changeCaptureFormat(final int width, final int height, final int framerate) {
		executor.execute(() -> changeCaptureFormatInternal(width, height, framerate));
	}
	
	private void changeCaptureFormatInternal(int width, int height, int framerate) {
		if (!isVideoCallEnabled() || isError || videoCapturer == null) {
			Log.e(TAG,
				"Failed to change capture format. Video: " + isVideoCallEnabled()
					+ ". Error : " + isError);
			return;
		}
		if (DEBUG) Log.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
		videoSource.adaptOutputFormat(width, height, framerate);
	}
	
	/**
	 * Implementation detail: observe ICE & stream changes and react accordingly.
 	 */
	private class PCObserver implements PeerConnection.Observer {
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			if (DEBUG) Log.v(TAG, "onIceCandidate:");
			executor.execute(() -> events.onIceCandidate(candidate));
		}
		
		@Override
		public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
			if (DEBUG) Log.v(TAG, "onIceCandidatesRemoved:");
			executor.execute(() -> events.onIceCandidatesRemoved(candidates));
		}
		
		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
			if (DEBUG) Log.v(TAG, "onSignalingChange:" + newState);
			executor.execute(() -> events.onSignalingChange(newState));
		}
		
		@Override
		public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
			if (DEBUG) Log.v(TAG, "onIceConnectionChange:" + newState);
			executor.execute(() -> {
				if (DEBUG) Log.d(TAG, "IceConnectionState: " + newState);
				if (newState == IceConnectionState.CONNECTED) {
					events.onIceConnected();
				} else if (newState == IceConnectionState.DISCONNECTED) {
					events.onIceDisconnected();
				} else if (newState == IceConnectionState.FAILED) {
					reportError("ICE connection failed.");
				}
			});
		}
		
		@Override
		public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
			if (DEBUG) Log.d(TAG, "IceGatheringState: " + newState);
			switch (newState) {
			case NEW:
				break;
			case GATHERING:
				break;
			case COMPLETE:
				executor.execute(() -> events.onIceCandidate(null));
				break;
			default:
				break;
			}
		}
		
		@Override
		public void onIceConnectionReceivingChange(boolean receiving) {
			if (DEBUG) Log.d(TAG, "IceConnectionReceiving changed to " + receiving);
		}
		
		@Override
		public void onAddStream(final MediaStream stream) {
			if (DEBUG) Log.d(TAG, "onAddStream:" + stream);
		}
		
		@Override
		public void onRemoveStream(final MediaStream stream) {
			if (DEBUG) Log.d(TAG, "onRemoveStream:" + stream);
		}
		
		@Override
		public void onDataChannel(final DataChannel dc) {
			if (DEBUG) Log.d(TAG, "New Data channel " + dc.label());
			
			if (!dataChannelEnabled)
				return;
			
			dc.registerObserver(new DataChannel.Observer() {
				@Override
				public void onBufferedAmountChange(long previousAmount) {
					if (DEBUG)
						Log.d(TAG, "Data channel buffered amount changed: " + dc.label() + ": " + dc.state());
				}
				
				@Override
				public void onStateChange() {
					if (DEBUG)
						Log.d(TAG, "Data channel state changed: " + dc.label() + ": " + dc.state());
				}
				
				@Override
				public void onMessage(final DataChannel.Buffer buffer) {
					if (buffer.binary) {
						if (DEBUG) Log.d(TAG, "Received binary msg over " + dc);
						return;
					}
					ByteBuffer data = buffer.data;
					final byte[] bytes = new byte[data.capacity()];
					data.get(bytes);
					String strData = new String(bytes, Charset.forName("UTF-8"));
					Log.d(TAG, "Got msg: " + strData + " over " + dc);
				}
			});
		}
		
		@Override
		public void onRenegotiationNeeded() {
			if (DEBUG) Log.v(TAG, "onRenegotiationNeeded:");
			// No need to do anything; AppRTC follows a pre-agreed-upon
			// signaling/negotiation protocol.
		}
		
		@Override
		public void onAddTrack(final RtpReceiver receiver, final MediaStream[] mediaStreams) {
			if (DEBUG) Log.v(TAG, "onAddTrack:");
		}
	}
	
	/**
	 * Implementation detail: handle offer creation/signaling and answer setting,
	 * as well as adding remote ICE candidates once the answer SDP is set.
	 */
	private class SDPObserver implements SdpObserver {
		@Override
		public void onCreateSuccess(final SessionDescription origSdp) {
			if (DEBUG) Log.v(TAG, "onCreateSuccess:");
			if (localSdp != null) {
				reportError("Multiple SDP create.");
				return;
			}
			String sdpDescription = origSdp.description;
			if (preferIsac) {
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
			}
			if (isVideoCallEnabled()) {
				sdpDescription =
					SdpUtils.preferCodec(sdpDescription, peerConnectionParameters.getSdpVideoCodecName(), false);
			}
			final SessionDescription sdp = new SessionDescription(origSdp.type, sdpDescription);
			localSdp = sdp;
			executor.execute(() -> {
				if (peerConnection != null && !isError) {
					Log.d(TAG, "Set local SDP from " + sdp.type);
					peerConnection.setLocalDescription(sdpObserver, sdp);
				}
			});
		}
		
		@Override
		public void onSetSuccess() {
			if (DEBUG) Log.v(TAG, "onSetSuccess:");
			executor.execute(() -> {
				if (peerConnection == null || isError) {
					return;
				}
				if (isInitiator) {
					// For offering peer connection we first create offer and set
					// local SDP, then after receiving answer set remote SDP.
					if (peerConnection.getRemoteDescription() == null) {
						// We've just set our local SDP so time to send it.
						if (DEBUG) Log.d(TAG, "Local SDP set successfully");
						events.onLocalDescription(localSdp);
					} else {
						// We've just set remote description, so drain remote
						// and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "Remote SDP set successfully");
						drainCandidates();
					}
				} else {
					// For answering peer connection we set remote SDP and then
					// create answer and set local SDP.
					if (peerConnection.getLocalDescription() != null) {
						// We've just set our local SDP so time to send it, drain
						// remote and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "Local SDP set successfully");
						events.onLocalDescription(localSdp);
						drainCandidates();
					} else {
						// We've just set remote SDP - do nothing for now -
						// answer will be created soon.
						if (DEBUG) Log.d(TAG, "Remote SDP set successfully");
					}
				}
			});
		}
		
		@Override
		public void onCreateFailure(final String error) {
			reportError("createSDP error: " + error);
		}
		
		@Override
		public void onSetFailure(final String error) {
			reportError("setSDP error: " + error);
		}
	}
}
