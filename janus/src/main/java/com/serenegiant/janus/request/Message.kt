package com.serenegiant.janus.request
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2025 saki t_saki@serenegiant.com
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

import com.serenegiant.janus.Plugin
import com.serenegiant.janus.Room
import com.serenegiant.janus.TransactionCallback
import com.serenegiant.janus.TransactionManager

/**
 * プラグインメッセージ送信用のヘルパークラス
 */
class Message(
	@JvmField
	val plugin: Plugin,
	@JvmField
	val body: Any,
	@JvmField
	val jsep: Any?,
	@JvmField
	val callback: TransactionCallback
) {
	@JvmField
	val janus = "message"
	@JvmField
	val transaction = TransactionManager.get(12, callback)
	@JvmField
	val session_id = plugin.sessionId()
	@JvmField
	val handle_id = plugin.pluginId()

	constructor(
		room: Room,
		body: Any,
		callback: TransactionCallback
	) : this(room, body, null, callback)

	override fun toString(): String {
		return "Message{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", session_id=" + session_id +
			", handle_id=" + handle_id +
			", body=" + body +
			", jsep=" + jsep +
			'}'
	}
}
