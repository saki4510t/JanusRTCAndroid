package org.appspot.apprtc;
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/**
 * Struct holding the connection parameters of an AppRTC room.
 */
public class RoomConnectionParameters {
	public final String roomUrl;
	public final String roomId;
	public final boolean loopback;
	public final String urlParameters;
	
	public RoomConnectionParameters(
		String roomUrl, String roomId, boolean loopback, String urlParameters) {
		this.roomUrl = roomUrl;
		this.roomId = roomId;
		this.loopback = loopback;
		this.urlParameters = urlParameters;
	}
	
	public RoomConnectionParameters(String roomUrl, String roomId, boolean loopback) {
		this(roomUrl, roomId, loopback, null /* urlParameters */);
	}
}
