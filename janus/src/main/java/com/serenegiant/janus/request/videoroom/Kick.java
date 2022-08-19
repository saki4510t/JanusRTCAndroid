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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * kickリクエスト用POJO
 */
public class Kick {
	@NonNull
	public final String request = "kick";
	/**
	 * unique numeric ID of the room
	 */
	public final long room;
	/**
	 * unique numeric ID of the participant to kick
	 */
	public final long id;
	/**
	 * room secret, mandatory if configured
	 */
	@Nullable
	public final String secret;

	/**
	 * コンストラクタ
	 * @param room
	 * @param id
	 * @param secret
	 */
	public Kick(final long room, final long id, @Nullable final String secret) {
		this.secret = secret;
		this.room = room;
		this.id = id;
	}

	@NonNull
	@Override
	public String toString() {
		return "Kick{" +
			"request='" + request + '\'' +
			", room=" + room +
			", id=" + id +
			", secret='" + secret + '\'' +
			'}';
	}
}
