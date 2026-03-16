package com.serenegiant.janus.request.videoroom
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2026 saki t_saki@serenegiant.com
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
 * VideoRoomプラグイン用メッセージボディー
 * publishリクエスト用(伝送開始)
 */
internal class Publish() {
	val request: String = "publish" // "publish"
	var audiocodec: String? = null // "<audio codec to prefer among the negotiated ones; optional>",
	var videocodec: String? = null // "<video codec to prefer among the negotiated ones; optional>",
	var bitrate: Int? = null // <bitrate cap to return via REMB; optional, overrides the global room value if present>
	var record: Boolean? = null // <true|false, whether this publisher should be recorded or not; optional>,
	var filename: String? = null // "<if recording, the base path/file to use for the recording files; optional>",
	var display: String? = null // "<new display name to use in the room; optional>",
	var audio_level_average: Int? = null // "<if provided, overrided the room audio_level_average for this user; optional>",
	var audio_active_packets: Int? = null // "<if provided, overrided the room audio_active_packets for this user; optional>",
	var descriptions: Array<StreamDescription>? = null // Other descriptions, if any

	constructor(
		audiocodec: String?,
		videocodec: String?,
		bitrate: Int?,
		record: Boolean?,
		filename: String?,
		display: String?,
		audio_level_average: Int?,
		audio_active_packets: Int?,
		descriptions: Array<StreamDescription>?
	) : this() {
		this.audiocodec = audiocodec
		this.videocodec = videocodec
		this.bitrate = bitrate
		this.record = record
		this.filename = filename
		this.display = display
		this.audio_level_average = audio_level_average
		this.audio_active_packets = audio_active_packets
		this.descriptions = descriptions
	}

	override fun toString(): String {
		return "Publish{" +
			"request='" + request + '\'' +
			", audiocodec='" + audiocodec + '\'' +
			", videocodec='" + videocodec + '\'' +
			", bitrate=" + bitrate +
			", record=" + record +
			", filename='" + filename + '\'' +
			", display='" + display + '\'' +
			", audio_level_average=" + audio_level_average +
			", audio_active_packets=" + audio_active_packets +
			", descriptions=" + descriptions?.contentToString() +
			'}'
	}
}
