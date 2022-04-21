package com.serenegiant.janus.request.videoroom;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigInteger;

/**
 * VideoRoomプラグイン用メッセージボディー
 */
public class Join {
	public final String request;
	public final int room;
	public final String ptype;
	public final String username;
	public final String display;
	public final BigInteger feed;
	@Nullable
	public final String token;
	
	public Join(final int room, @NonNull final String pType,
		@Nullable final String username,
		@Nullable final String display,
		@Nullable final BigInteger feed) {

		this(room, pType, username, display, feed, null);
	}

	public Join(final int room, @NonNull final String pType,
		@Nullable final String username,
		@Nullable final String display,
		@Nullable final BigInteger feed,
		@Nullable final String token) {

		this.request = "join";
		this.room = room;
		this.ptype = pType;
		this.username = username;
		this.display = display;
		this.feed = feed;
		this.token = token;
	}

	@NonNull
	@Override
	public String toString() {
		return "Join{" +
			"request='" + request + '\'' +
			", room=" + room +
			", ptype='" + ptype + '\'' +
			", username='" + username + '\'' +
			", display='" + display + '\'' +
			", feed='" + feed + '\'' +
			", token='" + token + '\'' +
			'}';
	}
}
