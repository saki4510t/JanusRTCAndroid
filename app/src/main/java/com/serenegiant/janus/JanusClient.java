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
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSink;

import java.util.List;

/**
 * com.serenegiant.janus.JanusClient is the interface representing an AppRTC client.
 */
public interface JanusClient {
	
	/**
	 * request to create PeerConnectionFactory
	 * @param options
	 */
	public void createPeerConnectionFactory(
		@Nullable final PeerConnectionFactory.Options options);
	
	/**
	 * create PeerConnection
	 * @param localRender
	 * @param remoteRenders
	 * @param videoCapturer
	 */
	public void createPeerConnection(final VideoSink localRender,
		final List<VideoRenderer.Callbacks> remoteRenders,
		final VideoCapturer videoCapturer);
	
	/**
	 * temporary disable video transmitting/receiving
	 */
	public void stopVideoSource();
	
	/**
	 * temporary enable video transmitting/receiving
	 */
	public void startVideoSource();
	
	/**
	 * switch camera to transmit camera images
	 */
	public void switchCamera();
	
	/**
	 * request change video size and frame rate
	 * @param width
	 * @param height
	 * @param framerate
	 */
	public void changeCaptureFormat(final int width, final int height, final int framerate);
	
	/**
	 * temporary enable/disable voice transmitting/receiving
	 * @param enable
	 */
	public void setAudioEnabled(final boolean enable);
	
	/**
	 * temporary enable/disable video transmitting/receiving
	 */
	public void setVideoEnabled(final boolean enable);
	
	/**
	 * request update stats
	 * @param enable
	 * @param periodMs
	 */
	public void enableStatsEvents(final boolean enable, final int periodMs);

	/**
	 * Asynchronously connect to an Janus-gateway room URL using supplied connection
	 * parameters. Once connection is established onConnectedToRoom()
	 * callback with room parameters is invoked.
	 */
	public void connectToRoom(final RoomConnectionParameters connectionParameters);
	
	/**
	 * Disconnect from room.
	 */
	public void disconnectFromRoom();
	
}
