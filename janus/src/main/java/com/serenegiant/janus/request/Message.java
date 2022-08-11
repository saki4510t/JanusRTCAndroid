package com.serenegiant.janus.request;
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

import com.serenegiant.janus.Plugin;
import com.serenegiant.janus.Room;
import com.serenegiant.janus.TransactionManager;

/**
 * プラグインメッセージ送信用のヘルパークラス
 */
public class Message {
	@NonNull
	public final String janus;
	@NonNull
	public final String transaction;
	public final long session_id;
	public final long handle_id;
	
	public final Object body;
	public final Object jsep;
	
	public Message(@NonNull final Plugin plugin,
		final Object body, final Object jsep,
		@Nullable final TransactionManager.TransactionCallback callback) {

		this.janus = "message";
		this.transaction = TransactionManager.get(12, callback);
		this.session_id = plugin.sessionId();
		this.handle_id = plugin.pluginId();
		this.body = body;
		this.jsep = jsep;
	}

	public Message(@NonNull final Room room,
		final Object body,
		@Nullable final TransactionManager.TransactionCallback callback) {

		this(room, body, null, callback);
	}

	@NonNull
	@Override
	public String toString() {
		return "Message{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", session_id=" + session_id +
			", handle_id=" + handle_id +
			", body=" + body +
			", jsep=" + jsep +
			'}';
	}
}
