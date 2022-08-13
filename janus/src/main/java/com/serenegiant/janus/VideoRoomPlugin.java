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

import android.os.Build;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.serenegiant.janus.request.Attach;
import com.serenegiant.janus.request.videoroom.ConfigPublisher;
import com.serenegiant.janus.request.videoroom.ConfigSubscriber;
import com.serenegiant.janus.response.videoroom.Configured;
import com.serenegiant.janus.request.videoroom.Offer;
import com.serenegiant.janus.request.Detach;
import com.serenegiant.janus.request.videoroom.Join;
import com.serenegiant.janus.request.JsepSdp;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.videoroom.Start;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.response.videoroom.RoomEvent;
import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.videoroom.PublisherInfo;
import com.serenegiant.janus.response.Session;
import com.serenegiant.nio.CharsetsUtils;

import org.appspot.apprtc.AppRTCConst;
import org.appspot.apprtc.PeerConnectionParameters;
import org.appspot.apprtc.RoomConnectionParameters;
import org.appspot.apprtc.RtcEventLog;
import org.appspot.apprtc.util.SdpUtils;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import androidx.annotation.Nullable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*package*/ abstract class VideoRoomPlugin extends JanusPlugin
	implements PeerConnection.Observer {

	private static final boolean DEBUG = true;	// set false on production
	
	/**
	 * callback interface for JanusPlugin
	 */
	interface VideoRoomCallback extends PluginCallback {
		/**
		 * callback when jointed to room
		 * @param plugin
		 * @param room
		 */
		public void onJoin(@NonNull final VideoRoomPlugin plugin,
			@NonNull final RoomEvent room);
		
		/**
		 * callback when other publisher enter to the same room
 		 * @param plugin
		 */
		public void onEnter(@NonNull final VideoRoomPlugin plugin);
	
		/**
		 * callback when other publisher leaved from room
		 * @param plugin
		 * @param pluginId
		 */
		public void onLeave(@NonNull final VideoRoomPlugin plugin,
			final long pluginId, final int numUsers);
		
		/**
		 * callback when MediaStream is added to PeerConnection
		 * @param plugin
		 * @param remoteStream
		 */
		public void onAddRemoteStream(@NonNull final VideoRoomPlugin plugin,
			@NonNull final MediaStream remoteStream);
		
		/**
		 * callback when MediaStream is removed from PeerConnection
		 * @param plugin
		 * @param stream
		 */
		public void onRemoveStream(@NonNull final VideoRoomPlugin plugin,
			@NonNull final MediaStream stream);
		
		/**
		 * callback when IceCandidate is updated
		 * @param plugin
		 * @param remoteCandidate
		 */
		public void onRemoteIceCandidate(@NonNull final VideoRoomPlugin plugin,
			@NonNull final IceCandidate remoteCandidate);
		/**
		 * Callback fired once connection is established (IceConnectionState is
		 * CONNECTED).
		 */
		public void onIceConnected(@NonNull final VideoRoomPlugin plugin);
		
		/**
		 * Callback fired once connection is closed (IceConnectionState is
		 * DISCONNECTED).
		 */

		public void onIceDisconnected(@NonNull final VideoRoomPlugin plugin);
		/**
		 * Callback fired once local SDP is created and set.
		 */
		public void onLocalDescription(@NonNull final VideoRoomPlugin plugin,
			@NonNull final SessionDescription sdp);
		
		public void createSubscriber(@NonNull final VideoRoomPlugin plugin,
			@NonNull final PublisherInfo info);

		/**
		 * リモート側のSessionDescriptionを受信した時
		 * これを呼び出すと通話中の状態になる
		 * @param plugin
		 * @param sdp
		 */
		public void onRemoteDescription(@NonNull final VideoRoomPlugin plugin,
			@NonNull final SessionDescription sdp);

		/**
		 * PeerConnectionの統計情報を取得できたときのコールバック
		 * @param plugin
		 * @param report
		 */
		public void onPeerConnectionStatsReady(@NonNull final VideoRoomPlugin plugin,
			@NonNull final RTCStatsReport report);

		public void onError(@NonNull final VideoRoomPlugin plugin,
			@NonNull final Throwable t);
	}
	
	private static enum RoomState {
		UNINITIALIZED,
		ATTACHED,
		CONNECTED,
		CLOSED,
		ERROR }

	protected final String TAG = "VideoRoomPlugin:" + getClass().getSimpleName();

	@NonNull
	protected final Object mSync = new Object();
	@NonNull
	private final MediaConstraints sdpMediaConstraints;
	@NonNull
	private final PeerConnectionParameters peerConnectionParameters;
	@NonNull
	protected final RoomConnectionParameters roomConnectionParameters;
	private PeerConnection peerConnection;
	/** Enable org.appspot.apprtc.RtcEventLog. */
	@Nullable
	RtcEventLog rtcEventLog;
	@Nullable
	private DataChannel dataChannel;
	/**
	 * Queued remote ICE candidates are consumed only after both local and
	 * remote descriptions are set. Similarly local ICE candidates are sent to
	 * remote peer after both local and remote description are set.
	 */
	@NonNull
	private final List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();

	@NonNull
	protected final VideoRoomAPI mVideoRoomAPI;
	@NonNull
	protected final VideoRoomCallback mCallback;
	protected final ExecutorService executor = Utils.executor;
	protected final List<Call<?>> mCurrentCalls = new ArrayList<>();
	private final boolean isLoopback;
	private final boolean isVideoCallEnabled;
	protected RoomState mRoomState = RoomState.UNINITIALIZED;
	@Nullable
	protected Room mRoom;
	protected SessionDescription mLocalSdp;
	protected SessionDescription mRemoteSdp;
	protected boolean isInitiator;
	protected boolean isError;
	private final boolean preferIsac;
	@NonNull
	private final Gson mGson = new Gson();

	/**
	 * constructor
	 * @param session
	 * @param callback
	 */
	public VideoRoomPlugin(
		@NonNull VideoRoomAPI videoRoomAPI,
		@NonNull final Session session,
		@NonNull final VideoRoomCallback callback,
		@NonNull final PeerConnectionParameters peerConnectionParameters,
		@NonNull final RoomConnectionParameters roomConnectionParameters,
		@NonNull final MediaConstraints sdpMediaConstraints,
		final boolean isVideoCallEnabled) {

		super(session);
		this.mVideoRoomAPI = videoRoomAPI;
		this.mCallback = callback;
		this.peerConnectionParameters = peerConnectionParameters;
		this.roomConnectionParameters = roomConnectionParameters;
		this.sdpMediaConstraints = sdpMediaConstraints;
		this.isVideoCallEnabled = isVideoCallEnabled;
		this.isLoopback = peerConnectionParameters.loopback;
		
		// Check if ISAC is used by default.
		preferIsac = peerConnectionParameters.audioCodec != null
			&& peerConnectionParameters.audioCodec.equals(AppRTCConst.AUDIO_CODEC_ISAC);
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			detach();
		} finally {
			super.finalize();
		}
	}
	
	@Nullable
	PeerConnection getPeerConnection() {
		return peerConnection;
	}
	
	/**
	 * PeerConnection関係をセット
	 * @param peerConnection
	 * @param dataChannel
	 * @param rtcEventLog
	 */
	public void setPeerConnection(
		@NonNull final PeerConnection peerConnection,
		@Nullable final DataChannel dataChannel,
		@Nullable final RtcEventLog rtcEventLog) {

		this.peerConnection = peerConnection;
		this.dataChannel = dataChannel;
		this.rtcEventLog = rtcEventLog;
	}

	public void createOffer() {
		if (DEBUG) Log.v(TAG, "createOffer:");
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC Create OFFER");
				isInitiator = true;
				peerConnection.createOffer(mSdpObserver, sdpMediaConstraints);
			}
		});
	}
	
	public void createAnswer() {
		if (DEBUG) Log.v(TAG, "createAnswer:");
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC create ANSWER");
				isInitiator = false;
				peerConnection.createAnswer(mSdpObserver, sdpMediaConstraints);
			}
		});
	}
	
	private void drainCandidates() {
		if (DEBUG) Log.v(TAG, "drainCandidates:");
		if (!queuedRemoteCandidates.isEmpty()) {
			if (DEBUG) Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
			for (IceCandidate candidate : queuedRemoteCandidates) {
				peerConnection.addIceCandidate(candidate);
			}
			queuedRemoteCandidates.clear();
		}
	}

	public void addRemoteIceCandidate(final IceCandidate candidate) {
		if (DEBUG) Log.v(TAG, "addRemoteIceCandidate:");
		executor.execute(() -> {
			if (peerConnection != null && !isError) {
				queuedRemoteCandidates.add(candidate);
			}
		});
	}
	
	public void removeRemoteIceCandidates(final IceCandidate[] candidates) {
		if (DEBUG) Log.v(TAG, "removeRemoteIceCandidates:");
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
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AppRTCConst.AUDIO_CODEC_ISAC, true);
			}
			if (isVideoCallEnabled) {
				sdpDescription =
					SdpUtils.preferCodec(sdpDescription,
						peerConnectionParameters.getSdpVideoCodecName(), false);
			}
			if (peerConnectionParameters.audioStartBitrate > 0) {
				sdpDescription = SdpUtils.setStartBitrate(
					AppRTCConst.AUDIO_CODEC_OPUS, false, sdpDescription, peerConnectionParameters.audioStartBitrate);
			}
			if (DEBUG) Log.d(TAG, "Set remote SDP.");
			final SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
			peerConnection.setRemoteDescription(mSdpObserver, sdpRemote);
		});
	}

	/**
	 * ルーム参加者の種類文字列を取得する
	 * パブリッシャー場合は"publisher", サブスクライバーの場合は"subscriber"を返す
	 * @return
	 */
	@NonNull
	protected abstract String getPType();

	/**
	 * feed IDを取得する
	 * パブリッシャーの時はnull, サブスクライバーの時はデータを取得するパブリッサシャーのIDを返す
	 * @return
	 */
	protected abstract long getFeedId();

	/**
	 * attach to VideoRoom plugin
	 */
	@Override
	public void attach() {
		if (DEBUG) Log.v(TAG, "attach:");
		final Attach attach = new Attach(getSession(),
			"janus.plugin.videoroom",
			null);
		final Call<PluginInfo> call = mVideoRoomAPI.attachPlugin(
			roomConnectionParameters.apiName, sessionId(), attach);
		addCall(call);
		call.enqueue(new Callback<PluginInfo>() {
			@Override
			public void onResponse(@NonNull final Call<PluginInfo> call,
				@NonNull final Response<PluginInfo> response) {

				if (response.isSuccessful() && (response.body() != null)) {
					removeCall(call);
					final PluginInfo info = response.body();
					if ("success".equals(info.janus)) {
						synchronized (mSync) {
							setInfo(info);
							mRoom = new Room(getSession(), info);
							mRoomState = RoomState.ATTACHED;
						}
						// プラグインにアタッチできた＼(^o^)／
						if (DEBUG) Log.v(TAG, "attach:success");
						mCallback.onAttach(VideoRoomPlugin.this);
						// ルームへjoin
						executor.execute(() -> {
							try {
								join();
							} catch (final Exception e) {
								reportError(e);
							}
						});
					} else {
						reportError(new RuntimeException("unexpected response:" + response));
					}
				} else {
					reportError(new RuntimeException("unexpected response:" + response));
				}
			}
			
			@Override
			public void onFailure(@NonNull final Call<PluginInfo> call,
				@NonNull final Throwable t) {

				reportError(t);
			}
		});
	}
	
	/**
	 * join to Room
	 * @throws IOException
	 */
	public void join() {
		if (DEBUG) Log.v(TAG, "join:");
		final String userName = TextUtils.isEmpty(roomConnectionParameters.userName)
			? Build.MODEL : roomConnectionParameters.userName;
		final String displayName = TextUtils.isEmpty(roomConnectionParameters.displayName)
			? Build.MODEL : roomConnectionParameters.displayName;
		final Room roomCopy;
		synchronized (mSync) {
			roomCopy = mRoom;
		}
		if (roomCopy == null) {
			reportError(new IllegalStateException("Unexpectedly room is null"));
			return;
		}
		final Message message = new Message(roomCopy,
			new Join(roomConnectionParameters.roomId, getPType(), userName, displayName, getFeedId()),
			mTransactionCallback);
		if (DEBUG) Log.v(TAG, "join:" + message);
		final Call<RoomEvent> call = mVideoRoomAPI.join(roomConnectionParameters.apiName,
			sessionId(), pluginId(), message);
		addCall(call);
		try {
			final Response<RoomEvent> response = call.execute();
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final RoomEvent join = response.body();
				if ("event".equals(join.janus)) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず");
					handlePluginEvent(message.transaction, join);
				} else if (!"ack".equals(join.janus)
					&& !"keepalive".equals(join.janus)) {

					throw new RuntimeException("unexpected response:" + response);
				}
				// 実際の応答はlong pollで待機
			} else {
				throw new RuntimeException("unexpected response:" + response);
			}
		} catch (final Exception e) {
			TransactionManager.removeTransaction(message.transaction);
			cancelCall();
			detach();
			reportError(e);
		}
	}

	/**
	 * detach from VideoRoom plugin
	 */
	@Override
	public void detach() {
		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)
			|| attached()
			|| (peerConnection != null)) {

			mRoomState = RoomState.CLOSED;
			if (DEBUG) Log.v(TAG, "detach:");
			cancelCall();
			final Call<Void> call = mVideoRoomAPI.detachPlugin(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(),
				new Detach(getSession(), mTransactionCallback));
			addCall(call);
			try {
				call.execute();
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
			}
			removeCall(call);
			if (DEBUG) Log.d(TAG, "Closing peer connection.");
			synchronized (mSync) {
				mRoom = null;
				setInfo(null);
			}
			if (dataChannel != null) {
				dataChannel.dispose();
				dataChannel = null;
			}
			if (rtcEventLog != null) {
				// RtcEventLog should stop before the peer connection is disposed.
				rtcEventLog.stop();
				rtcEventLog = null;
			}
			if (peerConnection != null) {
				peerConnection.dispose();
				peerConnection = null;
			}
		}
	}

	private void sendOfferSdp(final SessionDescription sdp, final boolean isLoopback) {
		if (DEBUG) Log.v(TAG, "sendOfferSdp:");
		if (mRoomState != RoomState.CONNECTED) {
			reportError(new RuntimeException("Sending offer SDP in non connected state."));
			return;
		}
		final Room roomCopy;
		synchronized (mSync) {
			roomCopy = mRoom;
		}
		if (roomCopy == null) {
			reportError(new IllegalStateException("Unexpectedly room is null"));
			return;
		}
		final Call<RoomEvent> call = mVideoRoomAPI.offer(
			roomConnectionParameters.apiName,
			sessionId(),
			pluginId(),
			new Message(roomCopy,
				new Offer(true, true),
				new JsepSdp("offer", sdp.description),
				mTransactionCallback)
		);
		addCall(call);
		try {
			final Response<RoomEvent> response = call.execute();
			if (DEBUG) Log.v(TAG, "sendOfferSdp:response=" + response
				+ "\n" + response.body());
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final RoomEvent offer = response.body();
				if ("event".equals(offer.janus)) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず");
					final SessionDescription answerSdp
						= new SessionDescription(
						SessionDescription.Type.fromCanonicalForm("answer"),
						offer.jsep.sdp);
					mCallback.onRemoteDescription(this, answerSdp);
				} else if (!"ack".equals(offer.janus)
					&& !"keepalive".equals(offer.janus)) {
					throw new RuntimeException("unexpected response " + response);
				}
				// 実際の待機はlong pollで行う
			} else {
				throw new RuntimeException("failed to send offer sdp");
			}
			if (isLoopback) {
				// In loopback mode rename this offer to answer and route it back.
				mCallback.onRemoteDescription(this, new SessionDescription(
					SessionDescription.Type.fromCanonicalForm("answer"),
					sdp.description));
			}
		} catch (final Exception e) {
			cancelCall();
			reportError(e);
		}
	}
	
	private void sendAnswerSdp(final SessionDescription sdp, final boolean isLoopback) {
		if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:");
		if (isLoopback) {
			Log.e(TAG, "Sending answer in loopback mode.");
			return;
		}
		final Room roomCopy;
		synchronized (mSync) {
			roomCopy = mRoom;
		}
		if (roomCopy == null) {
			reportError(new IllegalStateException("Unexpectedly room is null"));
			return;
		}
		final Call<ResponseBody> call = mVideoRoomAPI.send(
			roomConnectionParameters.apiName,
			sessionId(),
			pluginId(),
			new Message(roomCopy,
				new Start(roomConnectionParameters.roomId),
				new JsepSdp("answer", sdp.description),
				mTransactionCallback)
		);
		addCall(call);
		try {
			final Response<ResponseBody> response = call.execute();
			if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:response=" + response
				+ "\n" + response.body());
			removeCall(call);
		} catch (final IOException e) {
			cancelCall();
			reportError(e);
		}
	}

	public void sendLocalIceCandidate(final IceCandidate candidate, final boolean isLoopback) {
		if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:");
		if (!attached()) return;

		final Room roomCopy;
		synchronized (mSync) {
			roomCopy = mRoom;
		}
		if (roomCopy == null) {
			reportError(new IllegalStateException("Unexpectedly room is null"));
			return;
		}
		final Call<RoomEvent> call;
		if (candidate != null) {
			call = mVideoRoomAPI.trickle(
				roomConnectionParameters.apiName,
				sessionId(),
				pluginId(),
				new Trickle(roomCopy, candidate, mTransactionCallback)
			);
		} else {
			call = mVideoRoomAPI.trickleCompleted(
				roomConnectionParameters.apiName,
				sessionId(),
				pluginId(),
				new TrickleCompleted(roomCopy, mTransactionCallback)
			);
		}
		addCall(call);
		try {
			final Response<RoomEvent> response = call.execute();
//				if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:response=" + response
//					+ "\n" + response.body());
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final RoomEvent join = response.body();
				if ("event".equals(join.janus)) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず");
//					// FIXME 正常に処理できた…Roomの情報を更新する
//					IceCandidate remoteCandidate = null;
//					// FIXME removeCandidateを生成する
//					if (remoteCandidate != null) {
//						mCallback.onRemoteIceCandidate(this, remoteCandidate);
//					} else {
//						// FIXME remoteCandidateがなかった時
//					}
				} else if (!"ack".equals(join.janus)
					&& !"keepalive".equals(join.janus)) {

					throw new RuntimeException("unexpected response " + response);
				}
				// 実際の待機はlong pollで行う
			} else {
				throw new RuntimeException("unexpected response " + response);
			}
			if ((candidate != null) && isLoopback) {
				mCallback.onRemoteIceCandidate(this, candidate);
			}
		} catch (final IOException e) {
			cancelCall();
			detach();
			reportError(e);
		}
	}

//--------------------------------------------------------------------------------
	/**
	 * PeerConnection.Observerの実装
	 * @param newState
	 */
	@Override
	public void onSignalingChange(final PeerConnection.SignalingState newState) {
		if (DEBUG) Log.v(TAG, "onSignalingChange:" + newState);
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param newState
	 */
	@Override
	public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
		executor.execute(() -> {
			if (DEBUG) Log.d(TAG, "IceConnectionState: " + newState);
			switch (newState) {
			case CONNECTED:
				mCallback.onIceConnected(VideoRoomPlugin.this);
				break;
			case DISCONNECTED:
				mCallback.onIceDisconnected(VideoRoomPlugin.this);
				break;
			case FAILED:
				Log.w(TAG, "ICE connection failed.");
				// FIXME なぜだかこれが起こる、映像は少なくとも片方向はながれているので接続はできてそうなんだけど
//				reportError(new RuntimeException("ICE connection failed."));
				break;
			default:
				break;
			}
		});
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param receiving
	 */
	@Override
	public void onIceConnectionReceivingChange(final boolean receiving) {
		if (DEBUG) Log.v(TAG, "onIceConnectionReceivingChange:receiving=" + receiving);
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
 	 * @param newState
	 */
	@Override
	public void onIceGatheringChange(final PeerConnection.IceGatheringState newState) {
		if (DEBUG) Log.v(TAG, "onIceGatheringChange:" + newState);
		switch (newState) {
		case COMPLETE:
			executor.execute(() -> sendLocalIceCandidate(null, isLoopback));
			break;
		case NEW:
		case GATHERING:
		default:
			break;
		}
	}

	/**
	 * PeerConnection.Observerの実装
 	 * @param candidate
	 */
	@Override
	public void onIceCandidate(final IceCandidate candidate) {
		if (DEBUG) Log.v(TAG, "onIceCandidate:");
		
		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)) {
			executor.execute(() -> sendLocalIceCandidate(candidate, isLoopback));
		}
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param candidates
	 */
	@Override
	public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
		if (DEBUG) Log.v(TAG, "onIceCandidatesRemoved:");

//		executor.execute(() -> sendLocalIceCandidateRemovals(candidates));
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param stream
	 */
	@Override
	public void onAddStream(final MediaStream stream) {
		if (DEBUG) Log.v(TAG, "onAddStream:" + stream);

		executor.execute(() -> mCallback.onAddRemoteStream(VideoRoomPlugin.this, stream));
	}

	/**
	 * PeerConnection.Observerの実装
 	 * @param stream
	 */
	@Override
	public void onRemoveStream(final MediaStream stream) {
		if (DEBUG) Log.v(TAG, "onRemoveStream:" + stream);
	
		executor.execute(() -> mCallback.onRemoveStream(VideoRoomPlugin.this, stream));
	}

	/**
	 * PeerConnection.Observerの実装
 	 * @param channel
	 */
	@Override
	public void onDataChannel(final DataChannel channel) {
		if (DEBUG) Log.v(TAG, "onDataChannel:");
		if (dataChannel == null) {
			return;
		}
		
		// FIXME これはAppRTCMobileのままで単にログを出力するだけ
		channel.registerObserver(new DataChannel.Observer() {
			@Override
			public void onBufferedAmountChange(long previousAmount) {
				if (DEBUG) Log.d(TAG,
					 "Data channel buffered amount changed: "
					 + channel.label() + ": " + channel.state());
			}
			
			@Override
			public void onStateChange() {
				if (DEBUG) Log.d(TAG,
					"Data channel state changed: "
					+ channel.label() + ": " + channel.state());
			}
			
			@Override
			public void onMessage(final DataChannel.Buffer buffer) {
				if (buffer.binary) {
					if (DEBUG) Log.d(TAG, "Received binary msg over " + channel);
					return;
				}
				final ByteBuffer data = buffer.data;
				final byte[] bytes = new byte[data.capacity()];
				data.get(bytes);
				String strData = new String(bytes, CharsetsUtils.UTF8);
				Log.d(TAG, "Got msg: " + strData + " over " + channel);
			}
		});
	}

	/**
	 * PeerConnection.Observerの実装
	 */
	@Override
	public void onRenegotiationNeeded() {
		if (DEBUG) Log.v(TAG, "onRenegotiationNeeded:");
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
 	 * @param receiver
	 * @param streams
	 */
	@Override
	public void onAddTrack(final RtpReceiver receiver, final MediaStream[] streams) {
		if (DEBUG) Log.v(TAG, "onAddTrack:");
		// 今は何もしない
	}

//--------------------------------------------------------------------------------

	/**
	 * PeerConnectionの統計情報を取得要求
	 */
	public void requestStats() {
		if (peerConnection != null) {
			peerConnection.getStats(mRTCStatsCollectorCallback);
		}
	}

	@NonNull
	private final RTCStatsCollectorCallback mRTCStatsCollectorCallback
		= new RTCStatsCollectorCallback() {
		@Override
		public void onStatsDelivered(final RTCStatsReport rtcStatsReport) {
			if (rtcStatsReport != null) {
				mCallback.onPeerConnectionStatsReady(VideoRoomPlugin.this, rtcStatsReport);
			}
		}
	};

//--------------------------------------------------------------------------------
// Long pollによるメッセージ受信時の処理関係
	/**
	 * TransactionManagerからのコールバックインターフェースの実装
	 */
	@NonNull
	protected final TransactionManager.TransactionCallback
		mTransactionCallback = new TransactionManager.TransactionCallback() {
	
		/**
		 * usually this is called from from long poll
		 * 実際の処理は上位クラスの#onReceivedへ移譲
		 * @param transaction
		 * @param body
		 * @return
		 */
		@Override
		public boolean onReceived(
			@NonNull final String transaction,
			@NonNull final JSONObject body) {

			return VideoRoomPlugin.this.onReceived(transaction, body);
		}
	};
	
	/**
	 * TransactionManagerからのコールバックの実際の処理
	 * @param transaction
	 * @param body
	 * @return
	 */
	protected boolean onReceived(
		@NonNull final String transaction,
		@NonNull final JSONObject body) {

		if (DEBUG) Log.v(TAG, "onReceived:");
		@Nullable
		final String janus = body.optString("janus");
		boolean handled = false;
		if (!TextUtils.isEmpty(janus)) {
			switch (janus) {
			case "ack":
				// do nothing
				return true;
			case "keepalive":
				// サーバー側がタイムアウト(30秒？)した時は{"janus": "keepalive"}が来る
				// do nothing
				return true;
			case "event":
			{	// プラグインイベント
				try {
					final RoomEvent event = mGson.fromJson(body.toString(), RoomEvent.class);
					handled = handlePluginEvent(transaction, event);
				} catch (final JsonSyntaxException e) {
					reportError(new RuntimeException("wrong plugin event\n" + body));
				}
				break;
			}
			case "media":
			case "webrtcup":
			case "slowlink":
			case "hangup":
				// event for WebRTC
				handled = handleWebRTCEvent(transaction, body);
				break;
			case "error":
				reportError(new RuntimeException("error response\n" + body));
				return true;
			default:
				Log.d(TAG, "handleLongPoll:unknown event\n" + body);
				break;
			}
		} else {
			Log.d(TAG, "handleLongPoll:unexpected response\n" + body);
		}
		return handled;	// true: handled
	}

	/**
	 * プラグイン向けのイベントメッセージの処理
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEvent(@NonNull final String transaction,
		@NonNull final RoomEvent room) {

		if (DEBUG) Log.v(TAG, "handlePluginEvent:");
		// XXX このsenderはPublisherとして接続したときのVideoRoomプラグインのidらしい
		final long sender = room.sender;
		final String eventType = (room.plugindata != null) && (room.plugindata.data != null)
			? room.plugindata.data.videoroom : null;
		// FIXME plugindata.pluginが"janus.plugin.videoroom"かどうかのチェックをしたほうが良いかも
		if (DEBUG) Log.v(TAG, "handlePluginEvent:" + room);
		if (!TextUtils.isEmpty(eventType)) {
			switch (eventType) {
			case "attached":
				return handlePluginEventAttached(transaction, room);
			case "joined":
				return handlePluginEventJoined(transaction, room);
			case "event":
				return handlePluginEventEvent(transaction, room);
			}
		}
		return false;	// true: handled
	}
	
	/**
	 * eventTypeが"attached"のときの処理
	 * Subscriberがリモート側へjoinした時のレスポンス
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEventAttached(@NonNull final String transaction,
		@NonNull final RoomEvent room) {

		if (DEBUG) Log.v(TAG, "handlePluginEventAttached:");
		if (room.jsep != null) {
			if ("answer".equals(room.jsep.type)) {
				if (DEBUG) Log.v(TAG, "handlePluginEventAttached:answer");
				// Janus-gatewayの相手している時にたぶんこれは来ない
				final SessionDescription answerSdp
					= new SessionDescription(
						SessionDescription.Type.fromCanonicalForm("answer"),
					room.jsep.sdp);
				onRemoteDescription(answerSdp);
			} else if ("offer".equals(room.jsep.type)) {
				if (DEBUG) Log.v(TAG, "handlePluginEventAttached:offer");
				// Janus-gatewayの相手している時はたぶんいつもこっち
				final SessionDescription sdp
					= new SessionDescription(
						SessionDescription.Type.fromCanonicalForm("offer"),
					room.jsep.sdp);
				onRemoteDescription(sdp);
			}
		}
		if (this instanceof Subscriber) {
			mCallback.onEnter(this);
		}
		return true;	// true: 処理済み
	}
	
	/**
	 * eventTypeが"joined"のときの処理
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEventJoined(@NonNull final String transaction,
		@NonNull final RoomEvent room) {

		if (DEBUG) Log.v(TAG, "handlePluginEventJoined:");
		final Room roomCopy;
		synchronized (mSync) {
			roomCopy = mRoom;
		}
		if (roomCopy == null) {
			reportError(new IllegalStateException("Unexpectedly room is null"));
			return true;
		}
		mRoomState = RoomState.CONNECTED;
		roomCopy.publisherId = room.plugindata.data.id;
		mCallback.onJoin(this, room);
		return true;	// true: 処理済み
	}
	
	/**
	 * eventTypeが"event"のときの処理
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEventEvent(@NonNull final String transaction,
		@NonNull final RoomEvent room) {

		if (DEBUG) Log.v(TAG, "handlePluginEventEvent:");
		if (room.jsep != null) {
			if ("answer".equals(room.jsep.type)) {
				final SessionDescription answerSdp
					= new SessionDescription(
						SessionDescription.Type.fromCanonicalForm("answer"),
					room.jsep.sdp);
				onRemoteDescription(answerSdp);
			} else if ("offer".equals(room.jsep.type)) {
				final SessionDescription offerSdp
					= new SessionDescription(
						SessionDescription.Type.fromCanonicalForm("offer"),
					room.jsep.sdp);
				onRemoteDescription(offerSdp);
			}
		}
		if ((room.plugindata != null)
			&& (room.plugindata.data != null)) {

//			if (room.plugindata.data.unpublished != null) {
//				// XXX なにか処理必要？
//			}
			if (room.plugindata.data.leaving != null) {
				// FIXME ここは即プラグインマップから削除してその上でonLeaveを呼ぶほうがよい？
				executor.execute( () -> {
					final Room roomCopy;
					synchronized (mSync) {
						roomCopy = mRoom;
					}
					mCallback.onLeave(VideoRoomPlugin.this,
						room.plugindata.data.leaving,
						roomCopy != null ? roomCopy.getNumPublishers() : 0);
				});
			}
		}
		return true;	// true: 処理済み
	}
	
	private void onLocalDescription(@NonNull final SessionDescription sdp) {
		if (DEBUG) Log.v(TAG, "onLocalDescription:");
		mCallback.onLocalDescription(this, sdp);
		executor.execute(() -> {

			if (sdp.type == SessionDescription.Type.OFFER) {
				sendOfferSdp(sdp, isLoopback);
			} else {
				sendAnswerSdp(sdp, isLoopback);
			}
		});
	}
	
	/**
	 * リモート側のSessionDescriptionの準備ができたときの処理
	 * @param sdp
	 * @return
	 */
	protected void onRemoteDescription(@NonNull final SessionDescription sdp) {
		mRemoteSdp = sdp;
		setRemoteDescription(sdp);
//		// 通話準備完了
		mCallback.onRemoteDescription(this, sdp);
	}

	/**
	 * WebRTC関係のイベント受信時の処理
	 * @param body
	 * @return
	 */
	protected boolean handleWebRTCEvent(@NonNull final String transaction,
		final JSONObject body) {

		if (DEBUG) Log.v(TAG, "handleWebRTCEvent:" + body);
		return false;	// true: handled
	}

//================================================================================
	/**
	 * set call that is currently in progress
	 * @param call
	 */
	protected void addCall(@NonNull final Call<?> call) {
		synchronized (mCurrentCalls) {
			mCurrentCalls.add(call);
		}
	}
	
	protected void removeCall(@NonNull final Call<?> call) {
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
	protected void cancelCall() {
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

	protected void reportError(@NonNull final Throwable t) {
		try {
			mCallback.onError(this, t);
		} catch (final Exception e) {
			Log.w(TAG, e);
		}
	}

	/**
	 * PeerConnectionからのコールバック
	 */
	private final SdpObserver mSdpObserver = new SdpObserver() {
		@Override
		public void onCreateSuccess(final SessionDescription origSdp) {
			if (DEBUG) Log.v(TAG, "SdpObserver#onCreateSuccess:");
			if (mLocalSdp != null) {
				reportError(new RuntimeException("Multiple SDP create."));
				return;
			}
			String sdpDescription = origSdp.description;
			if (preferIsac) {
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AppRTCConst.AUDIO_CODEC_ISAC, true);
			}
			if (isVideoCallEnabled) {
				sdpDescription =
					SdpUtils.preferCodec(sdpDescription,
						peerConnectionParameters.getSdpVideoCodecName(), false);
			}
			final SessionDescription sdp = new SessionDescription(origSdp.type, sdpDescription);
			mLocalSdp = sdp;
			executor.execute(() -> {
				if (peerConnection != null && !isError) {
					Log.d(TAG, "SdpObserver: Set local SDP from " + sdp.type);
					peerConnection.setLocalDescription(mSdpObserver, sdp);
				}
			});
		}
		
		@Override
		public void onSetSuccess() {
			if (DEBUG) Log.v(TAG, "SdpObserver#onSetSuccess:");
			executor.execute(() -> {
				if (peerConnection == null || isError) {
					return;
				}
				if (isInitiator) {
					// For offering peer connection we first create offer and set
					// local SDP, then after receiving answer set remote SDP.
					if (peerConnection.getRemoteDescription() == null) {
						// We've just set our local SDP so time to send it.
						if (DEBUG) Log.d(TAG, "SdpObserver: Local SDP set successfully");
						onLocalDescription(mLocalSdp);
					} else {
						// We've just set remote description, so drain remote
						// and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "SdpObserver: Remote SDP set successfully");
						drainCandidates();
					}
				} else {
					// For answering peer connection we set remote SDP and then
					// create answer and set local SDP.
					if (peerConnection.getLocalDescription() != null) {
						// We've just set our local SDP so time to send it, drain
						// remote and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "SdpObserver: Local SDP set successfully");
						onLocalDescription(mLocalSdp);
						drainCandidates();
					} else {
						// We've just set remote SDP - do nothing for now -
						// answer will be created soon.
						if (DEBUG) Log.d(TAG, "SdpObserver: Remote SDP set successfully");
					}
				}
			});
		}
		
		@Override
		public void onCreateFailure(final String error) {
			reportError(new RuntimeException("createSDP error: " + error));
		}
		
		@Override
		public void onSetFailure(final String error) {
			reportError(new RuntimeException("setSDP error: " + error));
		}
	};
//================================================================================
	public static class Publisher extends VideoRoomPlugin {

		/**
		 * コンストラクタ
		 * @param session
		 */
		public Publisher(@NonNull VideoRoomAPI videoRoomAPI,
			@NonNull final Session session,
			@NonNull final VideoRoomCallback callback,
			@NonNull final PeerConnectionParameters peerConnectionParameters,
			@NonNull final RoomConnectionParameters roomConnectionParameters,
			@NonNull final MediaConstraints sdpMediaConstraints,
			final boolean isVideoCallEnabled) {

			super(videoRoomAPI, session, callback,
				peerConnectionParameters,
				roomConnectionParameters,
				sdpMediaConstraints,
				isVideoCallEnabled);
			if (DEBUG) Log.v(TAG, "Publisher:");
		}
		
		@NonNull
		@Override
		protected String getPType() {
			return "publisher";
		}

		@Override
		protected long getFeedId() {
			return 0;
		}

		@Override
		protected boolean handlePluginEvent(@NonNull final String transaction,
			@NonNull final RoomEvent room) {
			
			final boolean result = super.handlePluginEvent(transaction, room);
			checkPublishers(room);
			return result;
		}

		/**
		 * publisher用のconfigure API呼び出しを実効
		 * @param config
		 */
		public boolean configure(@NonNull final ConfigPublisher config) {
			if (DEBUG) Log.v(TAG, "configure:");
			cancelCall();
			final Call<Configured> call = mVideoRoomAPI.configure(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(),
				config);
			addCall(call);
			boolean result = false;
			try {
				final Response<Configured> response = call.execute();
				if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:response=" + response
					+ "\n" + response.body());
				result = "ok".equals(response.body().configured);
				removeCall(call);
			} catch (final Exception e) {
				if (DEBUG) Log.w(TAG, e);
				cancelCall();
				reportError(e);
			}
			if (DEBUG) Log.d(TAG, "configure:finished.");
			return result;
		}

		/**
		 * リモート側のPublisherをチェックして増減があれば接続/切断する
		 * @param room
		 */
		private void checkPublishers(@NonNull final RoomEvent room) {
			if (DEBUG) Log.v(TAG, "checkPublishers:");
			final Room roomCopy;
			synchronized (mSync) {
				roomCopy = mRoom;
			}
			if ((roomCopy != null)
				&& (room.plugindata != null)
				&& (room.plugindata.data != null)) {

				// ローカルキャッシュ
				final RoomEvent.Data data = room.plugindata.data;
				if (data.unpublished != null) {
					roomCopy.updatePublisher(data.unpublished, false);
				}
				if (data.leaving != null) {
					roomCopy.removePublisher(data.leaving);
				}
				@NonNull
				final List<PublisherInfo> changed = roomCopy.updatePublishers(data.publishers);
				if (!changed.isEmpty()) {
					if (DEBUG) Log.v(TAG, "checkPublishers:number of publishers changed");
					for (final PublisherInfo info: changed) {
						executor.execute(() -> {
							if (DEBUG) Log.v(TAG, "checkPublishers:attach new Subscriber");
							mCallback.createSubscriber(
								Publisher.this, info);
						});
					}
				}
			}
		}
	}
	
	public static class Subscriber extends VideoRoomPlugin {
		@NonNull
		public final PublisherInfo info;

		/**
		 * コンストラクタ
		 * @param session
		 */
		public Subscriber(@NonNull VideoRoomAPI videoRoomAPI,
			@NonNull final Session session,
			@NonNull final VideoRoomCallback callback,
			@NonNull final PeerConnectionParameters peerConnectionParameters,
			@NonNull final RoomConnectionParameters roomConnectionParameters,
			@NonNull final MediaConstraints sdpMediaConstraints,
			@NonNull final PublisherInfo info,
			final boolean isVideoCallEnabled) {

			super(videoRoomAPI, session, callback,
				peerConnectionParameters,
				roomConnectionParameters,
				sdpMediaConstraints,
				isVideoCallEnabled);

			if (DEBUG) Log.v(TAG, "Subscriber:");
			this.info = info;
		}
		
		@NonNull
		@Override
		protected String getPType() {
			return "subscriber";
		}

		protected long getFeedId() {
			return info.id;
		}

		@Override
		protected void onRemoteDescription(@NonNull final SessionDescription sdp) {
			if (DEBUG) Log.v(TAG, "onRemoteDescription:\n" + sdp.description);
			
			super.onRemoteDescription(sdp);
			if (sdp.type == SessionDescription.Type.OFFER) {
				createAnswer();
			}
		}

		/**
		 * Subscriber用のconfigure API呼び出し
		 * @param config
		 * @return true: 呼び出し成功
		 */
		public boolean configure(@NonNull final ConfigSubscriber config) {
			if (DEBUG) Log.v(TAG, "configure:");
			cancelCall();
			final Call<Configured> call = mVideoRoomAPI.configure(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(),
				config);
			addCall(call);
			boolean result = false;
			try {
				final Response<Configured> response = call.execute();
				result = "ok".equals(response.body().configured);
				removeCall(call);
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
				cancelCall();
				reportError(e);
			}
			if (DEBUG) Log.d(TAG, "configure:finished.");
			return result;
		}

	}
}
