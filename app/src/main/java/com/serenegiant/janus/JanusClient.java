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
import org.webrtc.PeerConnectionFactory;

/**
 * com.serenegiant.janus.JanusClient is the interface representing an AppRTC client.
 */
public interface JanusClient {
	
	public void createPeerConnectionFactory(
		@Nullable final PeerConnectionFactory.Options options);

	public void stopVideoSource();
	public void startVideoSource();
	public void switchCamera();
	public void changeCaptureFormat(final int width, final int height, final int framerate);
	public void setAudioEnabled(final boolean enable);
	public void setVideoEnabled(final boolean enable);
	public void enableStatsEvents(boolean enable, int periodMs);
	public void setVideoMaxBitrate(final int maxBitrateKbps);

	/**
	 * Asynchronously connect to an AppRTC room URL using supplied connection
	 * parameters. Once connection is established onConnectedToRoom()
	 * callback with room parameters is invoked.
	 */
	public void connectToRoom(final RoomConnectionParameters connectionParameters);
	
	/**
	 * Disconnect from room.
	 */
	public void disconnectFromRoom();
	
}
