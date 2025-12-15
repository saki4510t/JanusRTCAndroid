package com.serenegiant.janus.response.videoroom
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

import com.serenegiant.janus.request.JsepSdp

internal class RoomEvent(
	val janus: String,
	val sender: Long,
	val transaction: String,
	val plugindata: PluginData?,	// janus-gatewayから受け取ったデータなので念のためにnullableに
	val jsep: JsepSdp?			// janus-gatewayから受け取ったデータなので念のためにnullableに
) {
	override fun toString(): String {
		return "RoomEvent{" +
			"janus='" + janus + '\'' +
			", sender=" + sender +
			", transaction='" + transaction + '\'' +
			", plugindata=" + plugindata +
			", jsep=" + jsep +
			'}'
	}

	class PluginData(val plugin: String, val data: RoomData?) {
		override fun toString(): String {
			return "PluginData{" +
				"plugin='" + plugin + '\'' +
				", data=" + data +
				'}'
		}
	}
}
