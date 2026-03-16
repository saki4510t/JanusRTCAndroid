package com.serenegiant.janus
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2026 saki t_saki@serenegiant.com
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

import com.serenegiant.janus.response.videoroom.PublisherInfo
import com.serenegiant.janus.response.videoroom.RoomData
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.IceServer
import org.webrtc.RTCStatsReport
import org.webrtc.SessionDescription
import org.webrtc.VideoSink

interface JanusCallback : BuilderCallback {
	/**
	 * callback when JanusVideoRoomClient connects janus-gateway server
	 * @param client
	 */
	fun onConnectServer(client: JanusVideoRoomClient)

	/**
	 * get list of IceServer
	 * @param client
	 * @return
	 */
	fun getIceServers(client: JanusVideoRoomClient): List<IceServer?>

	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	fun onConnectedToRoom(initiator: Boolean, room: RoomData)

	/**
	 * Callback fired once remote SDP is received.
	 */
	fun onRemoteDescription(sdp: SessionDescription)

	/**
	 * Callback fired once remote Ice candidate is received.
	 */
	fun onRemoteIceCandidate(candidate: IceCandidate)

	/**
	 * Callback fired once remote Ice candidate removals are received.
	 */
	fun onRemoteIceCandidatesRemoved(candidates: Array<IceCandidate>)

	/**
	 * Callback fired once connection is established (IceConnectionState is
	 * CONNECTED).
	 */
	fun onIceConnected()

	/**
	 * Callback fired once connection is closed (IceConnectionState is
	 * DISCONNECTED/FAILED).
	 */
	fun onIceDisconnected(failed: Boolean)

	/**
	 * 新しいパブリッシャーが見つかったときのコールバック
	 * 返り値によってそのパブリッシャーを受け入れる(通話する)かどうかを判断する
	 * このメソッドがfalseを返した場合subscriberが生成されないので
	 * onEnterもonLeaveも呼び出されない
	 * @param info
	 * @return true: 受け入れる, false: 受け入れない
	 */
	fun onNewPublisher(info: PublisherInfo): Boolean

	/**
	 * #onNewPublisherがtrueを返し該当パブリッシャーが映像トラックを持っている時に呼ばれる。
	 * 描画用のVideoSinkのListを返す。
	 * 空なら描画されない(後からは追加できない)
	 * @param info
	 * @return
	 */
	fun getRemoteVideoSink(info: PublisherInfo): List<VideoSink>

	/**
	 * Callback fired when someone enter to the same room
	 * @param info
	 */
	fun onEnter(info: PublisherInfo)

	/**
	 * Callback fired when someone leaved from the same room
	 * @param info
	 * @param numUsers
	 */
	fun onLeave(info: PublisherInfo, numUsers: Int)

	/**
	 * Callback fired once channel is closed (hangup event occurred).
	 */
	fun onChannelClose()

	/**
	 * Callback fired when something webrtc event  other than hangup event occurred.
	 * @param event
	 */
	fun onEvent(event: JSONObject)

	/**
	 * Callback fired once peer connection is closed.
	 */
	fun onDisconnected()

	/**
	 * Callback fired when stat info from peer connection is ready
	 * @param isPublisher
	 * @param report
	 */
	fun onPeerConnectionStatsReady(
		isPublisher: Boolean,
		report: RTCStatsReport
	)

	/**
	 * Callback fired once channel error happened.
	 * @param t
	 */
	fun onChannelError(t: Throwable)
}
