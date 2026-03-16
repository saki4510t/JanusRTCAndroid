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
 * サブスクライバーのconfigureリクエスト用
 */
class ConfigSubscriber(
	val mid: Long? = null,	// <mid of the m-line to refer to for this configure request; optional>,
	val send: Boolean? = null, // <true|false, depending on whether the mindex media should be relayed or not; optional>,
	val substream: Int? = null, // <substream to receive (0-2), in case simulcasting is enabled; optional>,
	val temporal: Int? = null, // <temporal layers to receive (0-2), in case simulcasting is enabled; optional>,
	val fallback: Int? = null, // <How much time (in us, default 250000) without receiving packets will make us drop to the substream below>,
	val spatial_layer: Int? = null, // <spatial layer to receive (0-2), in case VP9-SVC is enabled; optional>,
	val temporal_layer: Int? = null, // <temporal layers to receive (0-2), in case VP9-SVC is enabled; optional>,
	val audio_level_average: Int? = null, // "<if provided, overrides the room audio_level_average for this user; optional>",
	val audio_active_packets: Int? = null, // "<if provided, overrides the room audio_active_packets for this user; optional>",
	val min_delay: Int? = null, // <minimum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	val max_delay: Int? = null, // <maximum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	val restart: Int? = null, // <trigger an ICE restart; optional>
) {
	val request: String = "configure" // "configure",

	override fun toString(): String {
		return "ConfigSubscription{" +
			"request='" + request + '\'' +
			", mid=" + mid +
			", send=" + send +
			", substream=" + substream +
			", temporal=" + temporal +
			", fallback=" + fallback +
			", spatial_layer=" + spatial_layer +
			", temporal_layer=" + temporal_layer +
			", audio_level_average=" + audio_level_average +
			", audio_active_packets=" + audio_active_packets +
			", min_delay=" + min_delay +
			", max_delay=" + max_delay +
			", restart=" + restart +
			'}'
	}
}
