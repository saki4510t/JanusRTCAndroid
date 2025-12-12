package com.serenegiant.janus
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

import android.os.Build
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.serenegiant.janus.TransactionManager.TransactionCallback
import com.serenegiant.janus.TransactionManager.removeTransaction
import com.serenegiant.janus.request.Attach
import com.serenegiant.janus.request.Detach
import com.serenegiant.janus.request.JsepSdp
import com.serenegiant.janus.request.Message
import com.serenegiant.janus.request.Trickle
import com.serenegiant.janus.request.TrickleCompleted
import com.serenegiant.janus.request.videoroom.ConfigPublisher
import com.serenegiant.janus.request.videoroom.ConfigSubscriber
import com.serenegiant.janus.request.videoroom.Join
import com.serenegiant.janus.request.videoroom.Kick
import com.serenegiant.janus.request.videoroom.Offer
import com.serenegiant.janus.request.videoroom.Start
import com.serenegiant.janus.response.PluginInfo
import com.serenegiant.janus.response.Session
import com.serenegiant.janus.response.videoroom.PublisherInfo
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.serenegiant.nio.CharsetsUtils
import com.serenegiant.webrtc.AppRTCConst
import com.serenegiant.webrtc.PeerConnectionParameters
import com.serenegiant.webrtc.RoomConnectionParameters
import com.serenegiant.webrtc.RtcEventLog
import com.serenegiant.webrtc.util.SdpUtils.preferCodec
import com.serenegiant.webrtc.util.SdpUtils.setStartBitrate
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceGatheringState
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.ExecutorService

/**
 * VideoRoomプラグインへアクセスするためのヘルパークラス
 * 実際に使うのはVideoRoomPluginクラスを継承したPublisherクラス(映像音声送信)と
 * Subscriberクラス(映像音声受信)
 */
internal abstract class VideoRoomPlugin(
	protected val mVideoRoomAPI: VideoRoomAPI,
	session: Session,
	protected val mCallback: VideoRoomCallback,
	private val peerConnectionParameters: PeerConnectionParameters,
	protected val roomConnectionParameters: RoomConnectionParameters,
	private val sdpMediaConstraints: MediaConstraints,
	private val isVideoCallEnabled: Boolean
) : JanusPlugin(session), PeerConnection.Observer {
	/**
	 * callback interface for JanusPlugin
	 */
	internal interface VideoRoomCallback : PluginCallback {
		/**
		 * callback when jointed to room
		 * @param plugin
		 * @param room
		 */
		fun onJoin(
			plugin: VideoRoomPlugin,
			room: RoomEvent
		)

		/**
		 * callback when other publisher enter to the same room
		 * @param plugin
		 */
		fun onEnter(plugin: VideoRoomPlugin)

		/**
		 * callback when other publisher leaved from room
		 * @param plugin
		 * @param pluginId
		 */
		fun onLeave(
			plugin: VideoRoomPlugin,
			pluginId: Long, numUsers: Int
		)

		/**
		 * callback when MediaStream is added to PeerConnection
		 * @param plugin
		 * @param remoteStream
		 */
		fun onAddRemoteStream(
			plugin: VideoRoomPlugin,
			remoteStream: MediaStream
		)

		/**
		 * callback when MediaStream is removed from PeerConnection
		 * @param plugin
		 * @param stream
		 */
		fun onRemoveStream(
			plugin: VideoRoomPlugin,
			stream: MediaStream
		)

		/**
		 * callback when IceCandidate is updated
		 * @param plugin
		 * @param remoteCandidate
		 */
		fun onRemoteIceCandidate(
			plugin: VideoRoomPlugin,
			remoteCandidate: IceCandidate
		)

		/**
		 * Callback fired once connection is established (IceConnectionState is
		 * CONNECTED).
		 */
		fun onIceConnected(plugin: VideoRoomPlugin)

		/**
		 * Callback fired once connection is closed (IceConnectionState is
		 * DISCONNECTED).
		 */
		fun onIceDisconnected(plugin: VideoRoomPlugin)

		/**
		 * Callback fired once local SDP is created and set.
		 */
		fun onLocalDescription(
			plugin: VideoRoomPlugin,
			sdp: SessionDescription
		)

		fun createSubscriber(
			plugin: VideoRoomPlugin,
			info: PublisherInfo
		)

		/**
		 * リモート側のSessionDescriptionを受信した時
		 * これを呼び出すと通話中の状態になる
		 * @param plugin
		 * @param sdp
		 */
		fun onRemoteDescription(
			plugin: VideoRoomPlugin,
			sdp: SessionDescription
		)

		/**
		 * PeerConnectionの統計情報を取得できたときのコールバック
		 * @param plugin
		 * @param report
		 */
		fun onPeerConnectionStatsReady(
			plugin: VideoRoomPlugin,
			report: RTCStatsReport
		)

		fun onError(
			plugin: VideoRoomPlugin,
			t: Throwable
		)
	}

	enum class RoomState {
		UNINITIALIZED,
		ATTACHED,
		CONNECTED,
		CLOSED,
		ERROR
	}

	protected val TAG: String = "VideoRoomPlugin:" + javaClass.simpleName

	protected val mSync: Any = Any()
	var peerConnection: PeerConnection? = null
		private set

	/** Enable com.serenegiant.webrtc.RtcEventLog.  */
	var rtcEventLog: RtcEventLog? = null
	private var dataChannel: DataChannel? = null

	/**
	 * Queued remote ICE candidates are consumed only after both local and
	 * remote descriptions are set. Similarly local ICE candidates are sent to
	 * remote peer after both local and remote description are set.
	 */
	private val queuedRemoteCandidates: MutableList<IceCandidate> = ArrayList()

	protected val executor: ExecutorService = Utils.executor
	protected val mCurrentCalls: MutableList<Call<*>> = ArrayList()

	private val isLoopback = peerConnectionParameters.loopback
	protected var mRoomState: RoomState = RoomState.UNINITIALIZED
	protected var mRoom: Room? = null
	protected var mLocalSdp: SessionDescription? = null
	protected var mRemoteSdp: SessionDescription? = null
	protected var isInitiator: Boolean = false
	protected var isError: Boolean = false


	// Check if ISAC is used by default.
	private val preferIsac = peerConnectionParameters.audioCodec != null
		&& peerConnectionParameters.audioCodec == AppRTCConst.AUDIO_CODEC_ISAC
	private val mGson = Gson()

	/**
	 * PeerConnection関係をセット
	 * @param peerConnection
	 * @param dataChannel
	 * @param rtcEventLog
	 */
	fun setPeerConnection(
		peerConnection: PeerConnection,
		dataChannel: DataChannel?,
		rtcEventLog: RtcEventLog?
	) {
		this.peerConnection = peerConnection
		this.dataChannel = dataChannel
		this.rtcEventLog = rtcEventLog
	}

	fun createOffer() {
		if (DEBUG) Log.v(TAG, "createOffer:")
		executor.execute {
			val connection = peerConnection
			if (connection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC Create OFFER")
				isInitiator = true
				connection.createOffer(mSdpObserver, sdpMediaConstraints)
			}
		}
	}

	fun createAnswer() {
		if (DEBUG) Log.v(TAG, "createAnswer:")
		executor.execute {
			val connection = peerConnection
			if (connection != null && !isError) {
				if (DEBUG) Log.d(TAG, "PC create ANSWER")
				isInitiator = false
				connection.createAnswer(mSdpObserver, sdpMediaConstraints)
			}
		}
	}

	private fun drainCandidates() {
		if (DEBUG) Log.v(TAG, "drainCandidates:")
		if (queuedRemoteCandidates.isNotEmpty()) {
			if (DEBUG) Log.d(TAG, "Add " + queuedRemoteCandidates.size + " remote candidates")
			for (candidate in queuedRemoteCandidates) {
				peerConnection?.addIceCandidate(candidate)
			}
			queuedRemoteCandidates.clear()
		}
	}

	fun addRemoteIceCandidate(candidate: IceCandidate) {
		if (DEBUG) Log.v(TAG, "addRemoteIceCandidate:")
		executor.execute {
			if (peerConnection != null && !isError) {
				queuedRemoteCandidates.add(candidate)
			}
		}
	}

	fun removeRemoteIceCandidates(candidates: Array<IceCandidate?>?) {
		if (DEBUG) Log.v(TAG, "removeRemoteIceCandidates:")
		executor.execute {
			val connection = peerConnection
			if (connection == null || isError) {
				return@execute
			}
			// Drain the queued remote candidates if there is any so that
			// they are processed in the proper order.
			drainCandidates()
			connection.removeIceCandidates(candidates)
		}
	}

	fun setRemoteDescription(sdp: SessionDescription) {
		executor.execute {
			val connection = peerConnection
			if (connection == null || isError) {
				return@execute
			}
			var sdpDescription = sdp.description
			if (preferIsac) {
				sdpDescription = preferCodec(sdpDescription, AppRTCConst.AUDIO_CODEC_ISAC, true)
			}
			if (isVideoCallEnabled) {
				sdpDescription = preferCodec(sdpDescription, peerConnectionParameters.sdpVideoCodecName, false)
			}
			if (peerConnectionParameters.audioStartBitrate > 0) {
				sdpDescription = setStartBitrate(
					AppRTCConst.AUDIO_CODEC_OPUS,
					false,
					sdpDescription,
					peerConnectionParameters.audioStartBitrate
				)
			}
			if (DEBUG) Log.d(TAG, "Set remote SDP.")
			val sdpRemote = SessionDescription(sdp.type, sdpDescription)
			connection.setRemoteDescription(mSdpObserver, sdpRemote)
		}
	}

	/**
	 * ルーム参加者の種類文字列を取得する
	 * パブリッシャー場合は"publisher", サブスクライバーの場合は"subscriber"を返す
	 * @return
	 */
	protected abstract val pType: String

	/**
	 * feed IDを取得する
	 * パブリッシャーの時は0, サブスクライバーの時はデータを取得するパブリッサシャーのIDを返す
	 * @return
	 */
	abstract val feedId: Long

	/**
	 * attach to VideoRoom plugin
	 */
	override fun attach() {
		if (DEBUG) Log.v(TAG, "attach:")
		val attach = Attach(session, "janus.plugin.videoroom", null)
		val call = mVideoRoomAPI.attachPlugin(
			roomConnectionParameters.apiName, sessionId(), attach)
		addCall(call)
		call.enqueue(object : Callback<PluginInfo?> {
			override fun onResponse(
				call: Call<PluginInfo?>,
				response: Response<PluginInfo?>
			) {
				if (response.isSuccessful && (response.body() != null)) {
					removeCall(call)
					val info = response.body()
					if ((info != null) && ("success" == info.janus)) {
						synchronized(mSync) {
							this@VideoRoomPlugin.info = info
							mRoom = Room(session, info)
							mRoomState = RoomState.ATTACHED
						}
						// プラグインにアタッチできた＼(^o^)／
						if (DEBUG) Log.v(TAG, "attach:success")
						mCallback.onAttach(this@VideoRoomPlugin)
						// ルームへjoin
						executor.execute {
							try {
								join()
							} catch (e: Exception) {
								reportError(e)
							}
						}
					} else {
						reportError(RuntimeException("unexpected response:$response"))
					}
				} else {
					reportError(RuntimeException("unexpected response:$response"))
				}
			}

			override fun onFailure(
				call: Call<PluginInfo?>,
				t: Throwable
			) {
				reportError(t)
			}
		})
	}

	/**
	 * join to Room
	 * @throws IOException
	 */
	fun join() {
		if (DEBUG) Log.v(TAG, "join:")
		val userName = if (TextUtils.isEmpty(roomConnectionParameters.userName)) {
			Build.MODEL
		} else {
			roomConnectionParameters.userName!!
		}
		val displayName = if (TextUtils.isEmpty(roomConnectionParameters.displayName)) {
			Build.MODEL
		} else {
			roomConnectionParameters.displayName!!
		}
		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return
		}
		val message = Message(
			roomCopy,
			Join(roomConnectionParameters.roomId, pType, userName, displayName,	feedId),
			mTransactionCallback
		)
		if (DEBUG) Log.v(TAG, "join:$message")
		val call = mVideoRoomAPI.join(
			roomConnectionParameters.apiName,
			sessionId(), pluginId(), message
		)
		addCall(call)
		try {
			val response = call.execute()
			if (response.isSuccessful && (response.body() != null)) {
				removeCall(call)
				val join = response.body()
				if ("event" == join!!.janus) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず")
					handlePluginEvent(message.transaction, join)
				} else if ("ack" != join.janus && "keepalive" != join.janus) {
					throw RuntimeException("unexpected response:$response,$join")
				}
				// 実際の応答はlong pollで待機
			} else {
				throw RuntimeException("unexpected response:$response")
			}
		} catch (e: Exception) {
			removeTransaction(message.transaction)
			cancelCall()
			detach()
			reportError(e)
		}
	}

	/**
	 * detach from VideoRoom plugin
	 */
	override fun detach() {
		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)
			|| attached()
			|| (peerConnection != null)
		) {
			mRoomState = RoomState.CLOSED
			if (DEBUG) Log.v(TAG, "detach:")
			cancelCall()
			val call = mVideoRoomAPI.detachPlugin(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(),
				Detach(session, mTransactionCallback)
			)
			addCall(call)
			try {
				call.execute()
			} catch (e: IOException) {
				if (DEBUG) Log.w(TAG, e)
			}
			removeCall(call)
			if (DEBUG) Log.d(TAG, "Closing peer connection.")
			synchronized(mSync) {
				mRoom = null
				info = null
			}
			if (dataChannel != null) {
				dataChannel!!.dispose()
				dataChannel = null
			}
			if (rtcEventLog != null) {
				// RtcEventLog should stop before the peer connection is disposed.
				rtcEventLog!!.stop()
				rtcEventLog = null
			}
			peerConnection?.dispose()
			peerConnection = null
		}
	}

	private fun sendOfferSdp(sdp: SessionDescription, isLoopback: Boolean) {
		if (DEBUG) Log.v(TAG, "sendOfferSdp:")
		if (mRoomState != RoomState.CONNECTED) {
			reportError(RuntimeException("Sending offer SDP in non connected state."))
			return
		}
		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return
		}
		val call = mVideoRoomAPI.offer(
			roomConnectionParameters.apiName,
			sessionId(),
			pluginId(),
			Message(
				roomCopy,
				Offer(audio = true, video = true),
				JsepSdp("offer", sdp.description),
				mTransactionCallback
			)
		)
		addCall(call)
		try {
			val response = call.execute()
			if (DEBUG) Log.v(TAG, "sendOfferSdp:response=$response,${response.body()}".trimIndent())
			if (response.isSuccessful && (response.body() != null)) {
				removeCall(call)
				val offer = response.body()
				if ("event" == offer!!.janus) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず")
					val answerSdp = SessionDescription(
						SessionDescription.Type.fromCanonicalForm("answer"),
						offer.jsep?.sdp
					)
					mCallback.onRemoteDescription(this, answerSdp)
				} else if ("ack" != offer.janus && "keepalive" != offer.janus) {
					throw RuntimeException("unexpected response $response")
				}
				// 実際の待機はlong pollで行う
			} else {
				throw RuntimeException("failed to send offer sdp")
			}
			if (isLoopback) {
				// In loopback mode rename this offer to answer and route it back.
				mCallback.onRemoteDescription(
					this, SessionDescription(
						SessionDescription.Type.fromCanonicalForm("answer"),
						sdp.description
					)
				)
			}
		} catch (e: Exception) {
			cancelCall()
			reportError(e)
		}
	}

	private fun sendAnswerSdp(sdp: SessionDescription, isLoopback: Boolean) {
		if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:")
		if (isLoopback) {
			Log.e(TAG, "Sending answer in loopback mode.")
			return
		}
		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return
		}
		val call = mVideoRoomAPI.send(
			roomConnectionParameters.apiName,
			sessionId(),
			pluginId(),
			Message(
				roomCopy,
				Start(roomConnectionParameters.roomId),
				JsepSdp("answer", sdp.description),
				mTransactionCallback
			)
		)
		addCall(call)
		try {
			val response = call.execute()
			if (DEBUG) Log.v(TAG, "sendAnswerSdpInternal:response=$response,${response.body()}".trimIndent())
			removeCall(call)
		} catch (e: IOException) {
			cancelCall()
			reportError(e)
		}
	}

	fun sendLocalIceCandidate(candidate: IceCandidate?, isLoopback: Boolean) {
		if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:")
		if (!attached()) return

		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return
		}
		val call = if (candidate != null) {
			mVideoRoomAPI.trickle(
				roomConnectionParameters.apiName,
				sessionId(),
				pluginId(),
				Trickle(roomCopy, candidate, mTransactionCallback)
			)
		} else {
			mVideoRoomAPI.trickleCompleted(
				roomConnectionParameters.apiName,
				sessionId(),
				pluginId(),
				TrickleCompleted(roomCopy, mTransactionCallback)
			)
		}
		addCall(call)
		try {
			val response = call.execute()
//			if (DEBUG) Log.v(TAG, "sendLocalIceCandidate:response=" + response
//					+ "\n" + response.body());
			if (response.isSuccessful && (response.body() != null)) {
				removeCall(call)
				val join = response.body()
				if ("event" == join!!.janus) {
					if (DEBUG) Log.v(TAG, "多分ここにはこない, ackが返ってくるはず")
//					// FIXME 正常に処理できた…Roomの情報を更新する
//					IceCandidate remoteCandidate = null;
//					// FIXME removeCandidateを生成する
//					if (remoteCandidate != null) {
//						mCallback.onRemoteIceCandidate(this, remoteCandidate);
//					} else {
//						// FIXME remoteCandidateがなかった時
//					}
				} else if ("ack" != join.janus && "keepalive" != join.janus) {
					throw RuntimeException("unexpected response $response")
				}
				// 実際の待機はlong pollで行う
			} else {
				throw RuntimeException("unexpected response $response")
			}
			if ((candidate != null) && isLoopback) {
				mCallback.onRemoteIceCandidate(this, candidate)
			}
		} catch (e: IOException) {
			cancelCall()
			detach()
			reportError(e)
		}
	}

	fun kick(kick: Kick): Boolean {
		if (DEBUG) Log.v(TAG, "kick:")
		cancelCall()
		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return false
		}
		val message = Message(
			roomCopy,
			kick, mTransactionCallback /* FIXME 無名オブジェクトにする */
		)
		if (DEBUG) Log.v(TAG, "kick:$message")
		val call = mVideoRoomAPI.kick(
			roomConnectionParameters.apiName,
			sessionId(), pluginId(), message
		)
		addCall(call)
		var result = false
		try {
			val response = call.execute()
			if (DEBUG) Log.v(TAG, "configure:response=$response,body=${response.body()}".trimIndent())
			val body = response.body()
			result = (response.code() == 200)
				&& "ack".equals(body!!.janus, ignoreCase = true)
			// FIXME　実際の結果はTransactionManagerのコールバックで返ってくるみたい
			removeCall(call)
		} catch (e: IOException) {
			if (DEBUG) Log.w(TAG, e)
			cancelCall()
			reportError(e)
		}
		if (DEBUG) Log.d(TAG, "kick:finished.")
		return result
	}

	//--------------------------------------------------------------------------------
	/**
	 * PeerConnection.Observerの実装
	 * @param newState
	 */
	override fun onSignalingChange(newState: SignalingState) {
		if (DEBUG) Log.v(TAG, "onSignalingChange:$newState")
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param newState
	 */
	override fun onIceConnectionChange(newState: IceConnectionState) {
		executor.execute {
			if (DEBUG) Log.d(TAG, "IceConnectionState: $newState")
			when (newState) {
				IceConnectionState.CONNECTED -> mCallback.onIceConnected(this@VideoRoomPlugin)
				IceConnectionState.DISCONNECTED -> mCallback.onIceDisconnected(this@VideoRoomPlugin)
				IceConnectionState.FAILED -> Log.w(TAG, "ICE connection failed.")
				else -> {}
			}
		}
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param receiving
	 */
	override fun onIceConnectionReceivingChange(receiving: Boolean) {
		if (DEBUG) Log.v(TAG, "onIceConnectionReceivingChange:receiving=$receiving")
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param newState
	 */
	override fun onIceGatheringChange(newState: IceGatheringState) {
		if (DEBUG) Log.v(TAG, "onIceGatheringChange:$newState")
		when (newState) {
			IceGatheringState.COMPLETE -> executor.execute {
				sendLocalIceCandidate(
					null,
					isLoopback
				)
			}

			IceGatheringState.NEW, IceGatheringState.GATHERING -> {}
			else -> {}
		}
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param candidate
	 */
	override fun onIceCandidate(candidate: IceCandidate) {
		if (DEBUG) Log.v(TAG, "onIceCandidate:")

		if ((mRoomState == RoomState.CONNECTED)
			|| (mRoomState == RoomState.ATTACHED)
		) {
			executor.execute { sendLocalIceCandidate(candidate, isLoopback) }
		}
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param candidates
	 */
	override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
		if (DEBUG) Log.v(TAG, "onIceCandidatesRemoved:")

//		executor.execute(() -> sendLocalIceCandidateRemovals(candidates))
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param stream
	 */
	override fun onAddStream(stream: MediaStream) {
		if (DEBUG) Log.v(TAG, "onAddStream:$stream")

		executor.execute {
			mCallback.onAddRemoteStream(
				this@VideoRoomPlugin,
				stream
			)
		}
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param stream
	 */
	override fun onRemoveStream(stream: MediaStream) {
		if (DEBUG) Log.v(TAG, "onRemoveStream:$stream")

		executor.execute { mCallback.onRemoveStream(this@VideoRoomPlugin, stream) }
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param channel
	 */
	override fun onDataChannel(channel: DataChannel) {
		if (DEBUG) Log.v(TAG, "onDataChannel:")
		if (dataChannel == null) {
			return
		}

		// FIXME これはAppRTCMobileのままで単にログを出力するだけ
		channel.registerObserver(object : DataChannel.Observer {
			override fun onBufferedAmountChange(previousAmount: Long) {
				if (DEBUG) Log.d(TAG, "Data channel buffered amount changed: ${channel.label()}: ${channel.state()}")
			}

			override fun onStateChange() {
				if (DEBUG) Log.d(TAG, "Data channel state changed: ${channel.label()}: ${channel.state()}")
			}

			override fun onMessage(buffer: DataChannel.Buffer) {
				if (buffer.binary) {
					if (DEBUG) Log.d(TAG, "Received binary msg over $channel")
					return
				}
				val data = buffer.data
				val bytes = ByteArray(data.capacity())
				data[bytes]
				val strData = String(bytes, CharsetsUtils.UTF8)
				Log.d(TAG, "Got msg: $strData over $channel")
			}
		})
	}

	/**
	 * PeerConnection.Observerの実装
	 */
	override fun onRenegotiationNeeded() {
		if (DEBUG) Log.v(TAG, "onRenegotiationNeeded:")
		// 今は何もしない
	}

	/**
	 * PeerConnection.Observerの実装
	 * @param receiver
	 * @param streams
	 */
	override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {
		if (DEBUG) Log.v(TAG, "onAddTrack:")
		// 今は何もしない
	}

	//--------------------------------------------------------------------------------
	/**
	 * PeerConnectionの統計情報を取得要求
	 */
	fun requestStats() {
		peerConnection?.getStats(mRTCStatsCollectorCallback)
	}

	private val mRTCStatsCollectorCallback = RTCStatsCollectorCallback { rtcStatsReport ->
		if (rtcStatsReport != null) {
			mCallback.onPeerConnectionStatsReady(this@VideoRoomPlugin, rtcStatsReport)
		}
	}

	//--------------------------------------------------------------------------------
	// Long pollによるメッセージ受信時の処理関係
	/**
	 * TransactionManagerからのコールバックインターフェースの実装
	 */
	protected val mTransactionCallback: TransactionCallback = object : TransactionCallback {
		/**
		 * usually this is called from from long poll
		 * 実際の処理は上位クラスの#onReceivedへ移譲
		 * @param transaction
		 * @param body
		 * @return
		 */
		override fun onReceived(
			transaction: String,
			body: JSONObject
		): Boolean {
			return this@VideoRoomPlugin.onReceived(transaction, body)
		}
	}

	/**
	 * TransactionManagerからのコールバックの実際の処理
	 * @param transaction
	 * @param body
	 * @return
	 */
	fun onReceived(
		transaction: String,
		body: JSONObject
	): Boolean {
		if (DEBUG) Log.v(TAG, "onReceived:$body")
		val janus = body.optString("janus")
		var handled = false
		if (!TextUtils.isEmpty(janus)) {
			when (janus) {
				"ack" -> {
					// do nothing
					return true
				}

				"keepalive" -> {
					// サーバー側がタイムアウト(30秒？)した時は{"janus": "keepalive"}が来る
					// do nothing
					return true
				}

				"event" -> {
					// プラグインイベント
					try {
						val event = mGson.fromJson(body.toString(), RoomEvent::class.java)
						handled = handlePluginEvent(transaction, event)
					} catch (e: JsonSyntaxException) {
						reportError(RuntimeException("wrong plugin event\n$body"))
					}
				}

				"detached" -> {
					detach()
					mCallback.onDetach(this)
				}

				"media", "webrtcup", "slowlink", "hangup" -> {
					// event for WebRTC
					handled = handleWebRTCEvent(transaction, body)
				}

				"error" -> {
					reportError(RuntimeException("error response\n$body"))
					return true
				}

				else -> Log.d(TAG, "handleLongPoll:unknown event\n$body")
			}
		} else {
			Log.d(TAG, "handleLongPoll:unexpected response\n$body")
		}
		return handled // true: handled
	}

	/**
	 * プラグイン向けのイベントメッセージの処理
	 * @param room
	 * @return
	 */
	protected open fun handlePluginEvent(
		transaction: String,
		room: RoomEvent
	): Boolean {
		if (DEBUG) Log.v(TAG, "handlePluginEvent:")
		// XXX このsenderはPublisherとして接続したときのVideoRoomプラグインのidらしい
		val sender = room.sender
		val eventType = if ((room.plugindata != null) && (room.plugindata.data != null))
			room.plugindata.data.videoroom
		else
			null
		// FIXME plugindata.pluginが"janus.plugin.videoroom"かどうかのチェックをしたほうが良いかも
		if (DEBUG) Log.v(TAG, "handlePluginEvent:$room")
		if (!TextUtils.isEmpty(eventType)) {
			when (eventType) {
				"attached" -> return handlePluginEventAttached(transaction, room)
				"joined" -> return handlePluginEventJoined(transaction, room)
				"event" -> return handlePluginEventEvent(transaction, room)
			}
		}
		return false // true: handled
	}

	/**
	 * eventTypeが"attached"のときの処理
	 * Subscriberがリモート側へjoinした時のレスポンス
	 * @param room
	 * @return
	 */
	protected fun handlePluginEventAttached(
		transaction: String,
		room: RoomEvent
	): Boolean {
		if (DEBUG) Log.v(TAG, "handlePluginEventAttached:")
		if (room.jsep != null) {
			if ("answer" == room.jsep.type) {
				if (DEBUG) Log.v(TAG, "handlePluginEventAttached:answer")
				// Janus-gatewayの相手している時にたぶんこれは来ない
				val answerSdp = SessionDescription(
					SessionDescription.Type.fromCanonicalForm("answer"),
					room.jsep.sdp
				)
				onRemoteDescription(answerSdp)
			} else if ("offer" == room.jsep.type) {
				if (DEBUG) Log.v(TAG, "handlePluginEventAttached:offer")
				// Janus-gatewayの相手している時はたぶんいつもこっち
				val sdp = SessionDescription(
					SessionDescription.Type.fromCanonicalForm("offer"),
					room.jsep.sdp
				)
				onRemoteDescription(sdp)
			}
		}
		if (this is Subscriber) {
			mCallback.onEnter(this)
		}
		return true // true: 処理済み
	}

	/**
	 * eventTypeが"joined"のときの処理
	 * @param room
	 * @return
	 */
	protected fun handlePluginEventJoined(
		transaction: String,
		room: RoomEvent
	): Boolean {
		if (DEBUG) Log.v(TAG, "handlePluginEventJoined:")
		val roomCopy: Room?
		synchronized(mSync) {
			roomCopy = mRoom
		}
		if (roomCopy == null) {
			reportError(IllegalStateException("Unexpectedly room is null"))
			return true
		}
		mRoomState = RoomState.CONNECTED
		roomCopy.publisherId = room.plugindata?.data?.id ?: 0
		mCallback.onJoin(this, room)
		return true // true: 処理済み
	}

	/**
	 * eventTypeが"event"のときの処理
	 * @param room
	 * @return
	 */
	protected fun handlePluginEventEvent(
		transaction: String,
		room: RoomEvent
	): Boolean {
		if (DEBUG) Log.v(TAG, "handlePluginEventEvent:")
		if (room.jsep != null) {
			if ("answer" == room.jsep.type) {
				val answerSdp = SessionDescription(
					SessionDescription.Type.fromCanonicalForm("answer"),
					room.jsep.sdp
				)
				onRemoteDescription(answerSdp)
			} else if ("offer" == room.jsep.type) {
				val offerSdp = SessionDescription(
					SessionDescription.Type.fromCanonicalForm("offer"),
					room.jsep.sdp
				)
				onRemoteDescription(offerSdp)
			}
		}
		if ((room.plugindata != null)
			&& (room.plugindata.data != null)
		) {
//			if (room.plugindata.data.unpublished != null) {
//				// XXX なにか処理必要？
//			}

			if (room.plugindata.data.leaving != null) {
				// FIXME ここは即プラグインマップから削除してその上でonLeaveを呼ぶほうがよい？
				executor.execute {
					val roomCopy: Room?
					synchronized(mSync) {
						roomCopy = mRoom
					}
					mCallback.onLeave(
						this@VideoRoomPlugin,
						room.plugindata.data.leaving,
						roomCopy?.getNumPublishers() ?: 0
					)
				}
			}
		}
		return true // true: 処理済み
	}

	private fun onLocalDescription(sdp: SessionDescription) {
		if (DEBUG) Log.v(TAG, "onLocalDescription:")
		mCallback.onLocalDescription(this, sdp)
		executor.execute {
			if (sdp.type == SessionDescription.Type.OFFER) {
				sendOfferSdp(sdp, isLoopback)
			} else {
				sendAnswerSdp(sdp, isLoopback)
			}
		}
	}

	/**
	 * リモート側のSessionDescriptionの準備ができたときの処理
	 * @param sdp
	 * @return
	 */
	protected open fun onRemoteDescription(sdp: SessionDescription) {
		mRemoteSdp = sdp
		setRemoteDescription(sdp)
		//		// 通話準備完了
		mCallback.onRemoteDescription(this, sdp)
	}

	/**
	 * WebRTC関係のイベント受信時の処理
	 * @param body
	 * @return
	 */
	protected fun handleWebRTCEvent(
		transaction: String,
		body: JSONObject
	): Boolean {
		if (DEBUG) Log.v(TAG, "handleWebRTCEvent:$body")
		return false // true: handled
	}

	//================================================================================
	/**
	 * set call that is currently in progress
	 * @param call
	 */
	protected fun addCall(call: Call<*>) {
		synchronized(mCurrentCalls) {
			mCurrentCalls.add(call)
		}
	}

	protected fun removeCall(call: Call<*>) {
		synchronized(mCurrentCalls) {
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
	protected fun cancelCall() {
		synchronized(mCurrentCalls) {
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

	protected fun reportError(t: Throwable) {
		try {
			mCallback.onError(this, t)
		} catch (e: Exception) {
			Log.w(TAG, e)
		}
	}

	/**
	 * PeerConnectionからのコールバック
	 */
	private val mSdpObserver: SdpObserver = object : SdpObserver {
		override fun onCreateSuccess(origSdp: SessionDescription) {
			if (DEBUG) Log.v(TAG, "SdpObserver#onCreateSuccess:")
			if (mLocalSdp != null) {
				reportError(RuntimeException("Multiple SDP create."))
				return
			}
			var sdpDescription = origSdp.description
			if (preferIsac) {
				sdpDescription = preferCodec(sdpDescription!!, AppRTCConst.AUDIO_CODEC_ISAC, true)
			}
			if (isVideoCallEnabled) {
				sdpDescription = preferCodec(
					sdpDescription!!,
					peerConnectionParameters.sdpVideoCodecName, false
				)
			}
			val sdp = SessionDescription(origSdp.type, sdpDescription)
			mLocalSdp = sdp
			executor.execute {
				val connection = peerConnection
				if (connection != null && !isError) {
					Log.d(TAG, "SdpObserver: Set local SDP from " + sdp.type)
					connection.setLocalDescription(this, sdp)
				}
			}
		}

		override fun onSetSuccess() {
			if (DEBUG) Log.v(TAG, "SdpObserver#onSetSuccess:")
			executor.execute {
				val connection = peerConnection
				if (connection == null || isError) {
					return@execute
				}
				if (isInitiator) {
					// For offering peer connection we first create offer and set
					// local SDP, then after receiving answer set remote SDP.
					if (connection.remoteDescription == null) {
						// We've just set our local SDP so time to send it.
						if (DEBUG) Log.d(TAG, "SdpObserver: Local SDP set successfully")
						onLocalDescription(mLocalSdp!!)
					} else {
						// We've just set remote description, so drain remote
						// and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "SdpObserver: Remote SDP set successfully")
						drainCandidates()
					}
				} else {
					// For answering peer connection we set remote SDP and then
					// create answer and set local SDP.
					if (connection.localDescription != null) {
						// We've just set our local SDP so time to send it, drain
						// remote and send local ICE candidates.
						if (DEBUG) Log.d(TAG, "SdpObserver: Local SDP set successfully")
						onLocalDescription(mLocalSdp!!)
						drainCandidates()
					} else {
						// We've just set remote SDP - do nothing for now -
						// answer will be created soon.
						if (DEBUG) Log.d(TAG, "SdpObserver: Remote SDP set successfully")
					}
				}
			}
		}

		override fun onCreateFailure(error: String) {
			reportError(RuntimeException("createSDP error: $error"))
		}

		override fun onSetFailure(error: String) {
			reportError(RuntimeException("setSDP error: $error"))
		}
	}

	//================================================================================
	/**
	 * janus-gatewayのVideoRoomプラグインへリモート映像・音声を送信するための
	 * ピアコネクションを保持するVideoRoomPlugin
	 * XXX パブリッシャー側は当面マルチストリーム対応しない予定
	 */
	class Publisher(
		videoRoomAPI: VideoRoomAPI,
		session: Session,
		callback: VideoRoomCallback,
		peerConnectionParameters: PeerConnectionParameters,
		roomConnectionParameters: RoomConnectionParameters,
		sdpMediaConstraints: MediaConstraints,
		isVideoCallEnabled: Boolean
	) :
		VideoRoomPlugin(
			videoRoomAPI, session, callback,
			peerConnectionParameters,
			roomConnectionParameters,
			sdpMediaConstraints,
			isVideoCallEnabled
		) {

		init {
			if (DEBUG) Log.v(TAG, "Publisher:")
		}

		override val pType: String
			get() = "publisher"

		override val feedId: Long
			get() = 0

		override fun handlePluginEvent(
			transaction: String,
			room: RoomEvent
		): Boolean {
			val result = super.handlePluginEvent(transaction, room)
			checkPublishers(room)
			return result
		}

		/**
		 * publisher用のconfigure API呼び出しを実効
		 * @param config
		 */
		fun configure(config: ConfigPublisher): Boolean {
			if (DEBUG) Log.v(TAG, "configure:$config")
			cancelCall()
			val roomCopy: Room?
			synchronized(mSync) {
				roomCopy = mRoom
			}
			if (roomCopy == null) {
				reportError(IllegalStateException("Unexpectedly room is null"))
				return false
			}
			val message = Message(
				roomCopy,
				config, mTransactionCallback /* FIXME 無名オブジェクトにする */
			)
			if (DEBUG) Log.v(TAG, "configure:$message")
			val call = mVideoRoomAPI.configure(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(), message
			)
			addCall(call)
			var result = false
			try {
				val response = call.execute()
				if (DEBUG) Log.v(TAG, "configure:response=$response,body=${response.body()}".trimIndent())
				val body = response.body()
				result = (response.code() == 200)
					&& "ack".equals(body!!.janus, ignoreCase = true)
				// FIXME　実際の結果はTransactionManagerのコールバックで返ってくるみたい
				removeCall(call)
			} catch (e: Exception) {
				if (DEBUG) Log.w(TAG, e)
				cancelCall()
				reportError(e)
			}
			if (DEBUG) Log.d(TAG, "configure:finished.$result")
			return result
		}

		/**
		 * リモート側のPublisherをチェックして増減があれば接続/切断する
		 * @param room
		 */
		private fun checkPublishers(room: RoomEvent) {
			if (DEBUG) Log.v(TAG, "checkPublishers:")
			val roomCopy: Room?
			synchronized(mSync) {
				roomCopy = mRoom
			}
			if ((roomCopy != null)
				&& (room.plugindata != null)
				&& (room.plugindata.data != null)
			) {
				// ローカルキャッシュ

				val data = room.plugindata.data
				if (data.unpublished != null) {
					roomCopy.updatePublisher(data.unpublished, false)
				}
				if (data.leaving != null) {
					roomCopy.removePublisher(data.leaving)
				}
				val changed = roomCopy.updatePublishers(data.publishers)
				if (changed.isNotEmpty()) {
					if (DEBUG) Log.v(TAG, "checkPublishers:number of publishers changed")
					for (info in changed) {
						executor.execute {
							if (DEBUG) Log.v(TAG, "checkPublishers:attach new Subscriber")
							mCallback.createSubscriber(
								this@Publisher, info
							)
						}
					}
				}
			}
		}
	}

	/**
	 * janus-gatewayのVideoRoomプラグインからのリモート映像・音声の受信を行うための
	 * ピアコネクションを保持するVideoRoomPlugin
	 *
	 * FIXME マルチストリーム対応を追加する
	 */
	class Subscriber(
		videoRoomAPI: VideoRoomAPI,
		session: Session,
		callback: VideoRoomCallback,
		peerConnectionParameters: PeerConnectionParameters,
		roomConnectionParameters: RoomConnectionParameters,
		sdpMediaConstraints: MediaConstraints,
		info: PublisherInfo,
		isVideoCallEnabled: Boolean
	) :
		VideoRoomPlugin(
			videoRoomAPI, session, callback,
			peerConnectionParameters,
			roomConnectionParameters,
			sdpMediaConstraints,
			isVideoCallEnabled
		) {

		val publisherInfo: PublisherInfo

		init {
			if (DEBUG) Log.v(TAG, "Subscriber:")
			this.publisherInfo = info
		}

		override val pType: String
			get() = "subscriber"

		override val feedId: Long
			get() = publisherInfo.id!!

		override fun onRemoteDescription(sdp: SessionDescription) {
			if (DEBUG) Log.v(TAG, "onRemoteDescription:${sdp.description}".trimIndent())
			super.onRemoteDescription(sdp)
			if (sdp.type == SessionDescription.Type.OFFER) {
				createAnswer()
			}
		}

		/**
		 * Subscriber用のconfigure API呼び出し
		 * @param config
		 * @return true: 呼び出し成功
		 */
		fun configure(config: ConfigSubscriber): Boolean {
			if (DEBUG) Log.v(TAG, "configure:$config")
			cancelCall()
			val roomCopy: Room?
			synchronized(mSync) {
				roomCopy = mRoom
			}
			if (roomCopy == null) {
				reportError(IllegalStateException("Unexpectedly room is null"))
				return false
			}
			val message = Message(
				roomCopy,
				config, mTransactionCallback /* FIXME 無名オブジェクトにする */
			)
			if (DEBUG) Log.v(TAG, "configure:$message")
			val call = mVideoRoomAPI.configure(
				roomConnectionParameters.apiName,
				sessionId(), pluginId(), message
			)
			addCall(call)
			var result = false
			try {
				val response = call.execute()
				if (DEBUG) Log.v(TAG, "configure:response=$response,body=${response.body()}".trimIndent())
				val body = response.body()
				result = (response.code() == 200)
					&& (body != null)
					&& "ack".equals(body.janus, ignoreCase = true)
				// FIXME　実際の結果はTransactionManagerのコールバックで返ってくるみたい
				removeCall(call)
			} catch (e: IOException) {
				if (DEBUG) Log.w(TAG, e)
				cancelCall()
				reportError(e)
			}
			if (DEBUG) Log.d(TAG, "configure:finished.$result")
			return result
		}
	}

	companion object {
		private const val DEBUG = false // set false on production
	}
}
