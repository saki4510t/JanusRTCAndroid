package com.serenegiant.janus;/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.support.annotation.Nullable;

import org.appspot.apprtc.RoomConnectionParameters;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;

/**
 * com.serenegiant.janus.JanusClient is the interface representing an AppRTC client.
 */
public interface JanusClient {
	
	public void createPeerConnectionFactory(
		@Nullable final PeerConnectionFactory.Options options);

	/**
	 * Asynchronously connect to an AppRTC room URL using supplied connection
	 * parameters. Once connection is established onConnectedToRoom()
	 * callback with room parameters is invoked.
	 */
	public void connectToRoom(final RoomConnectionParameters connectionParameters);
	
	/**
	 * Send offer SDP to the other participant.
	 */
	@Deprecated
	public void sendOfferSdp(final SessionDescription sdp);
	
	/**
	 * Send answer SDP to the other participant.
	 */
	@Deprecated
	public void sendAnswerSdp(final SessionDescription sdp);
	
	/**
	 * Send Ice candidate to the other participant.
	 */
	@Deprecated
	public void sendLocalIceCandidate(final IceCandidate candidate);
	
	/**
	 * Send removed ICE candidates to the other participant.
	 */
	@Deprecated
	public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);
	
	/**
	 * Disconnect from room.
	 */
	public void disconnectFromRoom();
	
}
