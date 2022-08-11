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
 * VideoRoomプラグイン用メッセージボディー
 * サブスクライバーのconfigureリクエスト用
 */
public class ConfigSubscriber {

	public final String request;	// "configure",
	@Nullable
	public final Long mid;	// <mid of the m-line to refer to for this configure request; optional>,
	@Nullable
	public final Boolean send;		// <true|false, depending on whether the mindex media should be relayed or not; optional>,
	@Nullable
	public final Integer substream;		// <substream to receive (0-2), in case simulcasting is enabled; optional>,
	@Nullable
	public final Integer temporal;		// <temporal layers to receive (0-2), in case simulcasting is enabled; optional>,
	@Nullable
	public final Integer fallback;		// <How much time (in us, default 250000) without receiving packets will make us drop to the substream below>,
	@Nullable
	public final Integer spatial_layer;	// <spatial layer to receive (0-2), in case VP9-SVC is enabled; optional>,
	@Nullable
	public final Integer temporal_layer;// <temporal layers to receive (0-2), in case VP9-SVC is enabled; optional>,
	@Nullable
	public final Integer audio_level_average;	// "<if provided, overrides the room audio_level_average for this user; optional>",
	@Nullable
	public final Integer audio_active_packets;	// "<if provided, overrides the room audio_active_packets for this user; optional>",
	@Nullable
	public final Integer min_delay;		// <minimum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	@Nullable
	public final Integer max_delay;		// <maximum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	@Nullable
	public final Integer restart;		// <trigger an ICE restart; optional>

	public ConfigSubscriber(final String request, @Nullable final Long mid, @Nullable final Boolean send, @Nullable final Integer substream, @Nullable final Integer temporal, @Nullable final Integer fallback, @Nullable final Integer spatial_layer, @Nullable final Integer temporal_layer, @Nullable final Integer audio_level_average, @Nullable final Integer audio_active_packets, @Nullable final Integer min_delay, @Nullable final Integer max_delay, @Nullable final Integer restart) {
		this.request = "configure";
		this.mid = mid;
		this.send = send;
		this.substream = substream;
		this.temporal = temporal;
		this.fallback = fallback;
		this.spatial_layer = spatial_layer;
		this.temporal_layer = temporal_layer;
		this.audio_level_average = audio_level_average;
		this.audio_active_packets = audio_active_packets;
		this.min_delay = min_delay;
		this.max_delay = max_delay;
		this.restart = restart;
	}

	@NonNull
	@Override
	public String toString() {
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
			'}';
	}
}
