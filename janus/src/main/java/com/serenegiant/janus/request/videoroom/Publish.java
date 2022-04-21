package com.serenegiant.janus.request.videoroom;
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

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * VideoRoomプラグイン用メッセージボディー
 * publishリクエスト用(伝送開始)
 */
public class Publish {
	@NonNull
	public final String request;// "publish"
	@Nullable
	public String audiocodec;	// "<audio codec to prefer among the negotiated ones; optional>",
	@Nullable
	public String videocodec;	// "<video codec to prefer among the negotiated ones; optional>",
	@Nullable
	public Integer bitrate;		// <bitrate cap to return via REMB; optional, overrides the global room value if present>,
	@Nullable
	public Boolean record;		// <true|false, whether this publisher should be recorded or not; optional>,
	@Nullable
	public String filename;		// "<if recording, the base path/file to use for the recording files; optional>",
	@Nullable
	public String display;		// "<new display name to use in the room; optional>",
	@Nullable
	public Integer audio_level_average;	// "<if provided, overrided the room audio_level_average for this user; optional>",
	@Nullable
	public Integer audio_active_packets;	// "<if provided, overrided the room audio_active_packets for this user; optional>",
	@Nullable
	public StreamDescription[] descriptions;	// Other descriptions, if any

	public Publish() {
		this.request = "publish";
	}

	public Publish(@Nullable final String audiocodec, @Nullable final String videocodec, @Nullable final Integer bitrate, @Nullable final Boolean record, @Nullable final String filename, @Nullable final String display, @Nullable final Integer audio_level_average, @Nullable final Integer audio_active_packets, @Nullable final StreamDescription[] descriptions) {
		this();
		this.audiocodec = audiocodec;
		this.videocodec = videocodec;
		this.bitrate = bitrate;
		this.record = record;
		this.filename = filename;
		this.display = display;
		this.audio_level_average = audio_level_average;
		this.audio_active_packets = audio_active_packets;
		this.descriptions = descriptions;
	}

	@NonNull
	@Override
	public String toString() {
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
			", descriptions=" + Arrays.toString(descriptions) +
			'}';
	}
}
