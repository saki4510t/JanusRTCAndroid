package com.serenegiant.janus;
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

import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;

import androidx.annotation.NonNull;

public class Plugin {
	/**
	 * セッション
	 */
	@NonNull
	private final Session session;

	/**
	 * プラグイン
	 */
	@NonNull
	private final PluginInfo info;

	/**
	 * 接続状態
 	 */
	public String state;

	/**
	 * コンストラクタ
	 * @param session
	 * @param info
	 */
	public Plugin(@NonNull final Session session, @NonNull final PluginInfo info) {
		this.session = session;
		this.info = info;
	}

	/**
	 * セッションIDを取得
	 * @return
	 */
	public BigInteger sessionId() {
		return session.id();
	}

	/**
	 * プラグインIDを取得
	 * @return
	 */
	public BigInteger pluginId() {
		return info.id();
	}
}
