package com.serenegiant.janus.request.videoroom
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

import com.serenegiant.janus.response.StreamInfo

/**
 * VideoRoomプラグイン用メッセージボディー
 * ルームへ入室するとき
 */
class Join @JvmOverloads constructor(
	/** ルームID  */
	val room: Long, // <unique ID of the room to join>...だけどroom設定だと<unique numeric ID>なので数字じゃないとだめ
	/** 参加者の種類文字列  */
	val ptype: String,
	/** サブスクライバーのみ  */
	val username: String? = null,
	/** パブリッシャーのみ?  */
	var display: String? = null, // <display name for the publisher; optional>
	/** サブスクライバーのみ  */
	var feed: Long? = null, // <unique ID of the publisher to subscribe to; mandatory>
	/** サブスクライバーのみ  */
	var private_id: Long? = null, // もしかするとStringかも // <unique ID of the publisher that originated this request; optional, unless mandated by the room configuration>,
	/** サブスクライバーのみ  */
	var streams: Array<StreamInfo>? = null,
	/** パブリッシャーのみ  */
	var id: String? = null, // <unique ID to register for the publisher; optional, will be chosen by the plugin if missing>
	/** パブリッシャーのみ  */
	val token: String? = null,
) {
	val request: String = "join"

	override fun toString(): String {
		return "Join{" +
			"request='" + request + '\'' +
			", room=" + room +
			", ptype='" + ptype + '\'' +
			", username='" + username + '\'' +
			", display='" + display + '\'' +
			", feed=" + feed +
			", private_id=" + private_id +
			", token='" + token + '\'' +
			", streams=" + streams.contentToString() + '\'' +
			'}'
	}
}
