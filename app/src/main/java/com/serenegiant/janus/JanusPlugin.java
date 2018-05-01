package com.serenegiant.janus;

import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.serenegiant.janus.request.Attach;
import com.serenegiant.janus.request.Configure;
import com.serenegiant.janus.request.Detach;
import com.serenegiant.janus.request.Join;
import com.serenegiant.janus.request.JsepSdp;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.Start;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.response.EventRoom;
import com.serenegiant.janus.response.Plugin;
import com.serenegiant.janus.response.PublisherInfo;
import com.serenegiant.janus.response.Session;

import org.appspot.apprtc.PeerConnectionParameters;
import org.appspot.apprtc.RtcEventLog;
import org.appspot.apprtc.util.SdpUtils;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.appspot.apprtc.AppRTCConst.AUDIO_CODEC_ISAC;
import static org.appspot.apprtc.AppRTCConst.AUDIO_CODEC_OPUS;

/*package*/ abstract class JanusPlugin implements PeerConnection.Observer {
	private static final boolean DEBUG = true;	// set false on production
	
	/**
	 * callback interface for JanusPlugin
	 */
	interface JanusPluginCallback {
		/**
		 * callback when attached to plugin
		 * @param plugin
		 */
		public void onAttach(@NonNull final JanusPlugin plugin);
		
		/**
		 * callback when jointed to room
		 * @param plugin
		 * @param room
		 */
		public void onJoin(@NonNull final JanusPlugin plugin, final EventRoom room);
		
		/**
		 * callback when detached from plugin
		 * @param plugin
		 */
		public void onDetach(@NonNull final JanusPlugin plugin);
		
		/**
		 * callback when other publisher leaved from room
		 * @param plugin
		 * @param pluginId
		 */
		public void onLeave(@NonNull final JanusPlugin plugin,
			@NonNull final BigInteger pluginId);
		
		/**
		 * callback when MediaStream is added to PeerConnection
		 * @param plugin
		 * @param remoteStream
		 */
		public void onAddRemoteStream(@NonNull final JanusPlugin plugin,
			@NonNull final MediaStream remoteStream);
		
		/**
		 * callback when MediaStream is removed from PeerConnection
		 * @param plugin
		 * @param stream
		 */
		public void onRemoveStream(@NonNull final JanusPlugin plugin,
			@NonNull final MediaStream stream);
		
		/**
		 * callback when IceCandidate is updated
		 * @param plugin
		 * @param remoteCandidate
		 */
		public void onRemoteIceCandidate(@NonNull final JanusPlugin plugin,
			final IceCandidate remoteCandidate);
		/**
		 * Callback fired once connection is established (IceConnectionState is
		 * CONNECTED).
		 */
		public void onIceConnected(@NonNull final JanusPlugin plugin);
		
		/**
		 * Callback fired once connection is closed (IceConnectionState is
		 * DISCONNECTED).
		 */

		public void onIceDisconnected(@NonNull final JanusPlugin plugin);
		/**
		 * Callback fired once local SDP is created and set.
		 */
		public void onLocalDescription(@NonNull final JanusPlugin plugin,
			final SessionDescription sdp);
		
		public void createSubscriber(@NonNull final JanusPlugin plugin,
			@NonNull final BigInteger feederId);

		/**
		 * リモート側のSessionDescriptionを受信した時
		 * これを呼び出すと通話中の状態になる
		 * @param plugin
		 * @param sdp
		 */
		public void onRemoteDescription(@NonNull final JanusPlugin plugin,
			final SessionDescription sdp);

		public void onError(@NonNull final JanusPlugin plugin,
			@NonNull final Throwable t);
	}
	
	private static enum RoomState {
		UNINITIALIZED,
		ATTACHED,
		CONNECTED,
		CLOSED,
		ERROR }

	protected final String TAG = "JanusPlugin:" + getClass().getSimpleName();

	@NonNull
	private final MediaConstraints sdpMediaConstraints;
	@NonNull
	private final PeerConnectionParameters peerConnectionParameters;
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
	protected final VideoRoom mVideoRoom;
	@NonNull
	protected final Session mSession;
	@NonNull
	protected final JanusPluginCallback mCallback;
	protected final ExecutorService executor = JanusRTCClient.executor;
	protected final List<Call<?>> mCurrentCalls = new ArrayList<>();
	private final boolean isLoopback;
	private final boolean isVideoCallEnabled;
	protected RoomState mRoomState = RoomState.UNINITIALIZED;
	protected Plugin mPlugin;
	protected Room mRoom;
	protected SessionDescription mLocalSdp;
	protected SessionDescription mRemoteSdp;
	protected boolean isInitiator;
	protected boolean isError;
	private final boolean preferIsac;
	
	/**
	 * constructor
	 * @param session
	 * @param callback
	 */
	public JanusPlugin(@NonNull VideoRoom videoRoom,
		@NonNull final Session session,
		@NonNull final JanusPluginCallback callback,
		@NonNull final PeerConnectionParameters peerConnectionParameters,
		@NonNull final MediaConstraints sdpMediaConstraints,
		final boolean isVideoCallEnabled) {
		
		this.mVideoRoom = videoRoom;
		this.mSession = session;
		this.mCallback = callback;
		this.peerConnectionParameters = peerConnectionParameters;
		this.sdpMediaConstraints = sdpMediaConstraints;
		this.isVideoCallEnabled = isVideoCallEnabled;
		this.isLoopback = peerConnectionParameters.loopback;
		
		// Check if ISAC is used by default.
		preferIsac = peerConnectionParameters.audioCodec != null
			&& peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC);
	}
	
	@Override
	protected void finalize() throws Throwable {
		try {
			detach();
		} finally {
			super.finalize();
		}
	}
	
	BigInteger id() {
		return mPlugin != null ? mPlugin.id() : null;
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
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
			}
			if (isVideoCallEnabled) {
				sdpDescription =
					SdpUtils.preferCodec(sdpDescription,
						peerConnectionParameters.getSdpVideoCodecName(), false);
			}
			if (peerConnectionParameters.audioStartBitrate > 0) {
				sdpDescription = SdpUtils.setStartBitrate(
					AUDIO_CODEC_OPUS, false, sdpDescription, peerConnectionParameters.audioStartBitrate);
			}
			if (DEBUG) Log.d(TAG, "Set remote SDP.");
			final SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
			peerConnection.setRemoteDescription(mSdpObserver, sdpRemote);
		});
	}

	@NonNull
	protected abstract String getPType();

	protected abstract BigInteger getFeedId();

	/**
	 * attach to VideoRoom plugin
	 */
	public void attach() {
		if (DEBUG) Log.v(TAG, "attach:");
		final Attach attach = new Attach(mSession,
			"janus.plugin.videoroom",
			null);
		final Call<Plugin> call = mVideoRoom.attach(mSession.id(), attach);
		addCall(call);
		call.enqueue(new Callback<Plugin>() {
			@Override
			public void onResponse(@NonNull final Call<Plugin> call,
				@NonNull final Response<Plugin> response) {

				if (response.isSuccessful() && (response.body() != null)) {
					removeCall(call);
					final Plugin plugin = response.body();
					if ("success".equals(plugin.janus)) {
						mPlugin = plugin;
						mRoom = new Room(mSession, mPlugin);
						mRoomState = RoomState.ATTACHED;
						// プラグインにアタッチできた＼(^o^)／
						if (DEBUG) Log.v(TAG, "attach:success");
						mCallback.onAttach(JanusPlugin.this);
						// ルームへjoin
						executor.execute(() -> {
							join();
						});
					} else {
						reportError(new RuntimeException("unexpected response:" + response));
					}
				} else {
					reportError(new RuntimeException("unexpected response:" + response));
				}
			}
			
			@Override
			public void onFailure(@NonNull final Call<Plugin> call,
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
		final Message message = new Message(mRoom,
			new Join(1234/*FIXME*/, getPType(), Build.MODEL, getFeedId()),
			mTransactionCallback);
		if (DEBUG) Log.v(TAG, "join:" + message);
		final Call<EventRoom> call = mVideoRoom.join(mSession.id(), mPlugin.id(), message);
		addCall(call);
		try {
			final Response<EventRoom> response = call.execute();
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final EventRoom join = response.body();
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
	public void detach() {
		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)
			|| (mPlugin != null)
			|| (peerConnection != null)) {

			mRoomState = RoomState.CLOSED;
			if (DEBUG) Log.v(TAG, "detach:");
			cancelCall();
			final Call<Void> call = mVideoRoom.detach(mSession.id(), mPlugin.id(),
				new Detach(mSession, mTransactionCallback));
			addCall(call);
			try {
				call.execute();
			} catch (final IOException e) {
				if (DEBUG) Log.w(TAG, e);
			}
			removeCall(call);
			if (DEBUG) Log.d(TAG, "Closing peer connection.");
			mRoom = null;
			mPlugin = null;
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
		final Call<EventRoom> call = mVideoRoom.offer(
			mSession.id(),
			mPlugin.id(),
			new Message(mRoom,
				new Configure(true, true),
				new JsepSdp("offer", sdp.description),
				mTransactionCallback)
		);
		addCall(call);
		try {
			final Response<EventRoom> response = call.execute();
			if (DEBUG) Log.v(TAG, "sendOfferSdp:response=" + response
				+ "\n" + response.body());
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final EventRoom offer = response.body();
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
		final Call<ResponseBody> call = mVideoRoom.send(
			mSession.id(),
			mPlugin.id(),
			new Message(mRoom,
				new Start(1234),
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
		if ((mSession == null) || (mPlugin == null)) return;

		final Call<EventRoom> call;
		if (candidate != null) {
			call = mVideoRoom.trickle(
				mSession.id(),
				mPlugin.id(),
				new Trickle(mRoom, candidate, mTransactionCallback)
			);
		} else {
			call = mVideoRoom.trickleCompleted(
				mSession.id(),
				mPlugin.id(),
				new TrickleCompleted(mRoom, mTransactionCallback)
			);
		}
		addCall(call);
		try {
			final Response<EventRoom> response = call.execute();
//				if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:response=" + response
//					+ "\n" + response.body());
			if (response.isSuccessful() && (response.body() != null)) {
				removeCall(call);
				final EventRoom join = response.body();
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
	
	@Override
	public void onSignalingChange(final PeerConnection.SignalingState newState) {
		if (DEBUG) Log.v(TAG, "onSignalingChange:" + newState);
		// 今は何もしない
	}
	
	@Override
	public void onIceConnectionChange(final PeerConnection.IceConnectionState newState) {
		executor.execute(() -> {
			if (DEBUG) Log.d(TAG, "IceConnectionState: " + newState);
			switch (newState) {
			case CONNECTED:
				mCallback.onIceConnected(JanusPlugin.this);
				break;
			case DISCONNECTED:
				mCallback.onIceDisconnected(JanusPlugin.this);
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
	
	@Override
	public void onIceConnectionReceivingChange(final boolean receiving) {
		if (DEBUG) Log.v(TAG, "onIceConnectionReceivingChange:receiving=" + receiving);
		// 今は何もしない
	}
	
	@Override
	public void onIceGatheringChange(final PeerConnection.IceGatheringState newState) {
		if (DEBUG) Log.v(TAG, "onIceGatheringChange:" + newState);
		switch (newState) {
		case NEW:
			break;
		case GATHERING:
			break;
		case COMPLETE:
			executor.execute(() -> sendLocalIceCandidate(null, isLoopback));
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onIceCandidate(final IceCandidate candidate) {
		if (DEBUG) Log.v(TAG, "onIceCandidate:");
		
		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)) {
			executor.execute(() -> sendLocalIceCandidate(candidate, isLoopback));
		}
	}
	
	@Override
	public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
		if (DEBUG) Log.v(TAG, "onIceCandidatesRemoved:");

//		executor.execute(() -> sendLocalIceCandidateRemovals(candidates));
	}
	
	@Override
	public void onAddStream(final MediaStream stream) {
		if (DEBUG) Log.v(TAG, "onAddStream:" + stream);

		executor.execute(() -> mCallback.onAddRemoteStream(JanusPlugin.this, stream));
	}
	
	@Override
	public void onRemoveStream(final MediaStream stream) {
		if (DEBUG) Log.v(TAG, "onRemoveStream:" + stream);
	
		executor.execute(() -> mCallback.onRemoveStream(JanusPlugin.this, stream));
	}
	
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
				String strData = new String(bytes, Charset.forName("UTF-8"));
				Log.d(TAG, "Got msg: " + strData + " over " + channel);
			}
		});
	}
	
	@Override
	public void onRenegotiationNeeded() {
		if (DEBUG) Log.v(TAG, "onRenegotiationNeeded:");
		// 今は何もしない
	}
	
	@Override
	public void onAddTrack(final RtpReceiver receiver, final MediaStream[] streams) {
		if (DEBUG) Log.v(TAG, "onAddTrack:");
		// 今は何もしない
	}

//--------------------------------------------------------------------------------
// Long pollによるメッセージ受信時の処理関係
	/**
	 * TransactionManagerからのコールバックインターフェースの実装
	 */
	protected final TransactionManager.TransactionCallback
		mTransactionCallback = new TransactionManager.TransactionCallback() {
	
		/**
		 * usually this is called from from long poll
		 * 実際の処理は上位クラスの#onReceivedへ移譲
		 * @param body
		 * @return
		 */
		@Override
		public boolean onReceived(@NonNull final String transaction,
			 final JSONObject body) {

			return JanusPlugin.this.onReceived(transaction, body);
		}
	};
	
	/**
	 * TransactionManagerからのコールバックの実際の処理
	 * @param body
	 * @return
	 */
	protected boolean onReceived(@NonNull final String transaction,
		final JSONObject body) {

		if (DEBUG) Log.v(TAG, "onReceived:");
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
			{
				// プラグインイベント
				final Gson gson = new Gson();
				final EventRoom event = gson.fromJson(body.toString(), EventRoom.class);
				handled = handlePluginEvent(transaction, event);
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
		@NonNull final EventRoom room) {

		if (DEBUG) Log.v(TAG, "handlePluginEvent:");
		// XXX このsenderはPublisherとして接続したときのVideoRoomプラグインのidらしい
		final BigInteger sender = room.sender;
		final String eventType = (room.plugindata != null) && (room.plugindata.data != null)
			? room.plugindata.data.videoroom : null;
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
		@NonNull final EventRoom room) {

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
		return true;	// true: 処理済み
	}
	
	/**
	 * eventTypeが"joined"のときの処理
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEventJoined(@NonNull final String transaction,
		@NonNull final EventRoom room) {

		if (DEBUG) Log.v(TAG, "handlePluginEventJoined:");
		mRoomState = RoomState.CONNECTED;
		mRoom.publisherId = room.plugindata.data.id;
		mCallback.onJoin(this, room);
		return true;	// true: 処理済み
	}
	
	/**
	 * eventTypeが"event"のときの処理
	 * @param room
	 * @return
	 */
	protected boolean handlePluginEventEvent(@NonNull final String transaction,
		@NonNull final EventRoom room) {

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
				sdpDescription = SdpUtils.preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
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
	public static class Publisher extends JanusPlugin {

		/**
		 * コンストラクタ
		 * @param session
		 */
		public Publisher(@NonNull VideoRoom videoRoom,
			@NonNull final Session session,
			@NonNull final JanusPluginCallback callback,
			@NonNull final PeerConnectionParameters peerConnectionParameters,
			@NonNull final MediaConstraints sdpMediaConstraints,
			final boolean isVideoCallEnabled) {

			super(videoRoom, session, callback,
				peerConnectionParameters,
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
		protected BigInteger getFeedId() {
			return null;
		}

		protected boolean handlePluginEvent(@NonNull final String transaction,
			@NonNull final EventRoom room) {
			
			final boolean result = super.handlePluginEvent(transaction, room);
			checkPublishers(room);
			return result;
		}
	
		/**
		 * リモート側のPublisherをチェックして増減があれば接続/切断する
		 * @param room
		 */
		private void checkPublishers(final EventRoom room) {
			if (DEBUG) Log.v(TAG, "checkPublishers:");
			if ((room.plugindata != null)
				&& (room.plugindata.data != null)) {
	
				@NonNull
				final List<PublisherInfo> changed = mRoom.updatePublisher(room.plugindata.data.publishers);
				if (room.plugindata.data.leaving != null) {
					for (final PublisherInfo info: changed) {
						if (room.plugindata.data.leaving.equals(info.id)) {
							// XXX ここで削除できたっけ?
							changed.remove(info);
						}
					}
					// FIXME 存在しなくなったPublisherの処理, leaveメッセージで処理すべき?
				}
				if (!changed.isEmpty()) {
					if (DEBUG) Log.v(TAG, "checkPublishers:number of publishers changed");
					for (final PublisherInfo info: changed) {
						executor.execute(() -> {
							if (DEBUG) Log.v(TAG, "checkPublishers:attach new Subscriber");
							mCallback.createSubscriber(
								Publisher.this, info.id);
						});
					}
				}
			}
		}
	}
	
	public static class Subscriber extends JanusPlugin {
		public final BigInteger feederId;

		/**
		 * コンストラクタ
		 * @param session
		 */
		public Subscriber(@NonNull VideoRoom videoRoom,
			@NonNull final Session session,
			@NonNull final JanusPluginCallback callback,
			@NonNull final PeerConnectionParameters peerConnectionParameters,
						  @NonNull final MediaConstraints sdpMediaConstraints,
			@NonNull final BigInteger feederId,
			final boolean isVideoCallEnabled) {

			super(videoRoom, session, callback,
				peerConnectionParameters,
				sdpMediaConstraints,
				isVideoCallEnabled);

			if (DEBUG) Log.v(TAG, "Subscriber:");
			this.feederId = feederId;
		}
		
		@NonNull
		@Override
		protected String getPType() {
			return "subscriber";
		}

		protected BigInteger getFeedId() {
			return feederId;
		}

		@Override
		protected void onRemoteDescription(@NonNull final SessionDescription sdp) {
			if (DEBUG) Log.v(TAG, "onRemoteDescription:\n" + sdp.description);
			
			super.onRemoteDescription(sdp);
			if (sdp.type == SessionDescription.Type.OFFER) {
				createAnswer();
			}
		}

	}
}
