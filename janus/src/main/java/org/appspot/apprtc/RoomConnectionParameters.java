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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Struct holding the connection parameters of an AppRTC room.
 */
public class RoomConnectionParameters {
	@NonNull
	public final String roomUrl;
	@NonNull
	public final String apiName;
	/** ルームID */
	public final int roomId;
	public final boolean loopback;
	public final String urlParameters;
	@Nullable
	public final String userName;
	@Nullable
	public final String displayName;
	
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final int roomId, final boolean loopback, final String urlParameters,
		@Nullable final String userName, @Nullable final String displayName) {
		this.roomUrl = roomUrl;
		this.apiName = apiName;
		this.roomId = roomId;
		this.loopback = loopback;
		this.urlParameters = urlParameters;
		this.userName = userName;
		this.displayName = displayName;
	}
	
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final int roomId, boolean loopback) {
		this(roomUrl, apiName, roomId,
			loopback, null,
			null, null);
	}
}
