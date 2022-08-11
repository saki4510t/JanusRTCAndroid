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

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * VideoRoomプラグイン用メッセージボディー
 * パブリッシャーのconfigureリクエスト用
 * パブリッシャーに対して発行するpublishリクエストとconfigureリクエストはほぼ同じ
 */
public class ConfigPublisher {
	@NonNull
	public final String request;	// "configure",
	@Nullable
	public Integer bitrate;	// <bitrate cap to return via REMB; optional, overrides the global room value if present (unless bitrate_cap is set)>,
	@Nullable
	public Boolean keyframe;// <true|false, whether we should send this publisher a keyframe request>,
	@Nullable
	public Boolean record;	// <true|false, whether this publisher should be recorded or not; optional>,
	@Nullable
	public String filename;	// "<if recording, the base path/file to use for the recording files; optional>",
	@Nullable
	public String display;	// "<new display name to use in the room; optional>",
	@Nullable
	public Integer audio_active_packets;// "<new audio_active_packets to overwrite in the room one; optional>",
	@Nullable
	public Integer audio_level_average;	// "<new audio_level_average to overwrite the room one; optional>",
	@Nullable
	public String mid;			// <mid of the m-line to refer to for this configure request; optional>,
	@Nullable
	public Boolean send;		// <true|false, depending on whether the media addressed by the above mid should be relayed or not; optional>,
	@Nullable
	public Integer min_delay;	// <minimum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	@Nullable
	public Integer max_delay;	// <maximum delay to enforce via the playout-delay RTP extension, in blocks of 10ms; optional>,
	@Nullable
	public StreamDescription[] descriptions;	// Updated descriptions for the published streams; see "publish" for syntax; optional

	public ConfigPublisher() {
		this.request = "configure";
	}

	public ConfigPublisher(@Nullable final Integer bitrate, @Nullable final Boolean keyframe, @Nullable final Boolean record, @Nullable final String filename, @Nullable final String display, @Nullable final Integer audio_active_packets, @Nullable final Integer audio_level_average, @Nullable final String mid, @Nullable final Boolean send, @Nullable final Integer min_delay, @Nullable final Integer max_delay, @Nullable final StreamDescription[] descriptions) {
		this();
		this.bitrate = bitrate;
		this.keyframe = keyframe;
		this.record = record;
		this.filename = filename;
		this.display = display;
		this.audio_active_packets = audio_active_packets;
		this.audio_level_average = audio_level_average;
		this.mid = mid;
		this.send = send;
		this.min_delay = min_delay;
		this.max_delay = max_delay;
		this.descriptions = descriptions;
	}

	@NonNull
	@Override
	public String toString() {
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
			", descriptions=" + Arrays.toString(descriptions) +
			'}';
	}
}
