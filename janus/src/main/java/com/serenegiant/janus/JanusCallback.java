package com.serenegiant.janus;
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 saki t_saki@serenegiant.com
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

import android.support.annotation.NonNull;

import com.serenegiant.janus.response.PublisherInfo;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.math.BigInteger;
import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public interface JanusCallback {
	@NonNull
	public OkHttpClient.Builder setupOkHttp(@NonNull final OkHttpClient.Builder builder,
		final boolean isLongPoll,
		final long connectionTimeout, final long readTimeoutMs, final long writeTimeoutMs);

	@NonNull
	public Retrofit.Builder setupRetrofit(@NonNull final Retrofit.Builder builder);

	/**
	 * callback when JanusRTCClient connects janus-gateway server
	 * @param client
	 */
	public void onConnectServer(@NonNull final JanusRTCClient client);
	
	/**
	 * get list of IceServer
	 * @param client
	 * @return
	 */
	public List<PeerConnection.IceServer> getIceServers(@NonNull final JanusRTCClient client);

	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	public void onConnectedToRoom(final boolean initiator);
	
	/**
	 * Callback fired once remote SDP is received.
	 */
	public void onRemoteDescription(final SessionDescription sdp);
	
	/**
	 * Callback fired once remote Ice candidate is received.
	 */
	public void onRemoteIceCandidate(final IceCandidate candidate);
	
	/**
	 * Callback fired once remote Ice candidate removals are received.
	 */
	public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

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
	 * Callback fired when someone enter to the same room
 	 * @param info
	 */
	public void onEnter(final PublisherInfo info);
	
	/**
	 * Callback fired when someone leaved from the same room
	 * @param info
	 * @param numUsers
	 */
	public void onLeave(final PublisherInfo info, final int numUsers);

	/**
	 * Callback fired once channel is closed.
	 */
	public void onChannelClose();
	
	/**
	 * Callback fired once peer connection is closed.
	 */
	public void onDisconnected();

	/**
	 * Callback fired once channel error happened.
	 */
	public void onChannelError(final String description);

}
