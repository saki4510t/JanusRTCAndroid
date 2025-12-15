package com.serenegiant.janus.request.videoroom
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

/**
 * offer SDP送信用メッセージボディー
 * XXX https://janus.conf.meetecho.com/docs/rest.htmlだとrequestは無いんだけど
 * 実際にはrequest="configure"を含めないとエラーが出て接続できない
 */
internal class Offer(val audio: Boolean, val video: Boolean) {
	val request: String = "configure"

	override fun toString(): String {
		return "Offer{" +
			"request='" + request + '\'' +
			", audio=" + audio +
			", video=" + video +
			'}'
	}
}
