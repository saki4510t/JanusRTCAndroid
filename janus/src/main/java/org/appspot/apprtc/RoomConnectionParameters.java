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
	public final long roomId;
	public final boolean loopback;
	@Nullable
	public final String urlParameters;
	@Nullable
	public final String userName;
	@Nullable
	public final String displayName;

	/**
	 * コンストラクタ
	 * @param roomUrl
	 * @param apiName
	 * @param roomId
	 * @param loopback
	 * @param urlParameters
	 * @param userName nullまたは空文字列ならBuild.MODELを使う
	 * @param displayName  nullまたは空文字列ならBuild.MODELを使う
	 */
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final long roomId, final boolean loopback, @Nullable final String urlParameters,
		@Nullable final String userName, @Nullable final String displayName) {
		this.roomUrl = roomUrl;
		this.apiName = apiName;
		this.roomId = roomId;
		this.loopback = loopback;
		this.urlParameters = urlParameters;
		this.userName = userName;
		this.displayName = displayName;
	}

	/**
	 * コンストラクタ
	 * urlParameters, userName, displayNameはnullなので
	 * userNameとdisplayNameはBuild.MODELを使う
	 * @param roomUrl
	 * @param apiName
	 * @param roomId
	 * @param loopback
	 */
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final long roomId, boolean loopback) {
		this(roomUrl, apiName, roomId,
			loopback, null,
			null, null);
	}
}
