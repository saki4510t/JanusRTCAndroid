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

import androidx.annotation.NonNull;

import com.serenegiant.janus.response.videoroom.PublisherInfo;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsReport;
import org.webrtc.SessionDescription;
import org.webrtc.VideoSink;

import java.util.List;


public interface JanusCallback extends Utils.BuilderCallback {
	/**
	 * callback when JanusVideoRoomClient connects janus-gateway server
	 * @param client
	 */
	public void onConnectServer(@NonNull final JanusVideoRoomClient client);
	
	/**
	 * get list of IceServer
	 * @param client
	 * @return
	 */
	@NonNull
	public List<PeerConnection.IceServer> getIceServers(@NonNull final JanusVideoRoomClient client);

	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	public void onConnectedToRoom(final boolean initiator);
	
	/**
	 * Callback fired once remote SDP is received.
	 */
	public void onRemoteDescription(@NonNull final SessionDescription sdp);
	
	/**
	 * Callback fired once remote Ice candidate is received.
	 */
	public void onRemoteIceCandidate(@NonNull final IceCandidate candidate);
	
	/**
	 * Callback fired once remote Ice candidate removals are received.
	 */
	public void onRemoteIceCandidatesRemoved(@NonNull final IceCandidate[] candidates);

	/**
	 * Callback fired once connection is established (IceConnectionState is
	 * CONNECTED).
	 */
	public void onIceConnected();
	
	/**
	 * Callback fired once connection is closed (IceConnectionState is
	 * DISCONNECTED).
	 */
	public void onIceDisconnected();
	
	/**
	 * 新しいパブリッシャーが見つかったときのコールバック
	 * 返り値によってそのパブリッシャーを受け入れる(通話する)かどうかを判断する
	 * このメソッドがfalseを返した場合subscriberが生成されないので
	 * onEnterもonLeaveも呼び出されない
	 * @param info
	 * @return true: 受け入れる, false: 受け入れない
	 */
	public boolean onNewPublisher(@NonNull final PublisherInfo info);

	/**
	 * #onNewPublisherがtrueを返し該当パブリッシャーが映像トラックを持っている時に呼ばれる。
	 * 描画用のVideoSinkのListを返す。
	 * 空なら描画されない(後からは追加できない)
	 * @param info
	 * @return
	 */
	@NonNull
	public List<VideoSink> getRemoteVideoSink(@NonNull final PublisherInfo info);

	/**
	 * Callback fired when someone enter to the same room
 	 * @param info
	 */
	public void onEnter(@NonNull final PublisherInfo info);
	
	/**
	 * Callback fired when someone leaved from the same room
	 * @param info
	 * @param numUsers
	 */
	public void onLeave(@NonNull final PublisherInfo info, final int numUsers);

	/**
	 * Callback fired once channel is closed (hangup event occurred).
	 */
	public void onChannelClose();
	
	/**
	 * Callback fired when something webrtc event  other than hangup event occurred.
	 * @param event
	 */
	public void onEvent(@NonNull final JSONObject event);
	
	/**
	 * Callback fired once peer connection is closed.
	 */
	public void onDisconnected();

	/**
	 * Callback fired when stat info from peer connection is ready
	 * @param isPublisher
	 * @param report
	 */
	public void onPeerConnectionStatsReady(final boolean isPublisher,
		final RTCStatsReport report);

	/**
	 * Callback fired once channel error happened.
	 * @param t
	 */
	public void onChannelError(@NonNull final Throwable t);

}
