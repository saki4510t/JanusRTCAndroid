package com.serenegiant.janus.request.videoroom;
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

import com.serenegiant.janus.response.StreamInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * VideoRoomプラグイン用メッセージボディー
 * ルームへ入室するとき
 */
public class Join {
	@NonNull
	public final String request;
	/** ルームID */
	public final long room;	// <unique ID of the room to join>...だけどroom設定だと<unique numeric ID>なので数字じゃないとだめ
	/** 参加者の種類文字列 */
	@NonNull
	public final String ptype;
	/** サブスクライバーのみ */
	@Nullable
	public String username;
	/** パブリッシャーのみ */
	@Nullable
	public String id;		// <unique ID to register for the publisher; optional, will be chosen by the plugin if missing>,
	/** パブリッシャーのみ */
	@Nullable
	public String display;	// <display name for the publisher; optional>
	/** サブスクライバーのみ */
	@Nullable
	public Long feed;	// <unique ID of the publisher to subscribe to; mandatory>
	/** サブスクライバーのみ */
	@Nullable
	public Long private_id;	// もしかするとStringかも // <unique ID of the publisher that originated this request; optional, unless mandated by the room configuration>,
	/** サブスクライバーのみ */
	public StreamInfo[] streams;
	/** パブリッシャーのみ */
	@Nullable
	public String token;

	/**
	 * 最低限の引数を指定するコンストラクタ
	 * @param room
	 * @param pType
	 */
	public Join(final long room, @NonNull final String pType) {
		this(room, pType, null, null, null, null, null);
	}

	public Join(final long room, @NonNull final String pType,
		@Nullable final String username,
		@Nullable final String display,
		@Nullable final Long feed) {

		this(room, pType, username, display, feed, null, null);
	}

	public Join(final long room, @NonNull final String pType,
		@Nullable final String username,
		@Nullable final String display,
		@Nullable final Long feed,
		@Nullable final Long private_id,
		@Nullable final String token) {

		this.request = "join";
		this.room = room;
		this.ptype = pType;
		this.username = username;
		this.display = display;
		this.feed = feed;
		this.private_id = private_id;
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
			", feed=" + feed +
			", private_id=" + private_id +
			", token='" + token + '\'' +
			", streams=" + Arrays.toString(streams) + '\'' +
			'}';
	}
}
