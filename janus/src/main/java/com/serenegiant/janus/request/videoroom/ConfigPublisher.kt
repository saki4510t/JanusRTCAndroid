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
 * パブリッシャーのconfigureリクエスト用
 * パブリッシャーに対して発行するpublishリクエストとconfigureリクエストはほぼ同じ
 */
class ConfigPublisher() {
	val request: String = "configure" // "configure",
	var bitrate: Int? = null // <bitrate cap to return via REMB; optional, overrides the global room value if present (unless bitrate_cap is set)>,
	var keyframe: Boolean? = null // <true|false, whether we should send this publisher a keyframe request>,
	var record: Boolean? = null // <true|false, whether this publisher should be recorded or not; optional>,
	var filename: String? = null // "<if recording, the base path/file to use for the recording files; optional>",
	var display: String? = null // "<new display name to use in the room; optional>",
	var audio_active_packets: Int? = null // "<new audio_active_packets to overwrite in the room one; optional>",
	var audio_level_average: Int? = null // "<new audio_level_average to overwrite the room one; optional>",
	var mid: String? = null // <mid of the m-line to refer to for this configure request; optional>,
	var send: Boolean? = null // <true|false, depending on whether the media addressed by the above mid should be relayed or not; optional>,
	var min_delay: Int? = null // <minimum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	var max_delay: Int? = null // <maximum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	var descriptions: Array<StreamDescription>? = null // Updated descriptions for the published streams; see "publish" for syntax; optional

	/**
	 * constructor to adjust bitrate
	 * @param bitrate
	 */
	constructor(bitrate: Int) : this() {
		this.bitrate = bitrate
	}

	/**
	 * constructor to change display name
	 * @param display
	 */
	constructor(display: String) : this() {
		this.display = display
	}

	/**
	 * constructor
	 * @param bitrate
	 * @param keyframe
	 * @param record
	 * @param filename
	 * @param display
	 * @param audio_active_packets
	 * @param audio_level_average
	 * @param mid
	 * @param send
	 * @param min_delay
	 * @param max_delay
	 * @param descriptions
	 */
	constructor(
		bitrate: Int?,
		keyframe: Boolean?,
		record: Boolean?,
		filename: String?,
		display: String?,
		audio_active_packets: Int?,
		audio_level_average: Int?,
		mid: String?,
		send: Boolean?,
		min_delay: Int?,
		max_delay: Int?,
		descriptions: Array<StreamDescription>?
	) : this() {
		this.bitrate = bitrate
		this.keyframe = keyframe
		this.record = record
		this.filename = filename
		this.display = display
		this.audio_active_packets = audio_active_packets
		this.audio_level_average = audio_level_average
		this.mid = mid
		this.send = send
		this.min_delay = min_delay
		this.max_delay = max_delay
		this.descriptions = descriptions
	}

	override fun toString(): String {
		return "ConfigPublisher{" +
			"request='" + request + '\'' +
			", bitrate=" + bitrate +
			", keyframe=" + keyframe +
			", record=" + record +
			", filename='" + filename + '\'' +
			", display='" + display + '\'' +
			", audio_active_packets=" + audio_active_packets +
			", audio_level_average=" + audio_level_average +
			", mid='" + mid + '\'' +
			", send=" + send +
			", min_delay=" + min_delay +
			", max_delay=" + max_delay +
			", descriptions=" + descriptions.contentToString() +
			'}'
	}
}
