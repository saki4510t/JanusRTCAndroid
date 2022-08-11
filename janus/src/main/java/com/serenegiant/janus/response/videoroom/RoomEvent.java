package com.serenegiant.janus.response.videoroom;
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

import com.serenegiant.janus.request.JsepSdp;
import com.serenegiant.janus.response.StreamInfo;

import java.math.BigInteger;
import java.util.Arrays;

import androidx.annotation.NonNull;

public class RoomEvent {
	public final String janus;
	public final BigInteger sender;
	public final String transaction;
	public final PluginData plugindata;
	public final JsepSdp jsep;
	
	public RoomEvent(final String janus, final BigInteger sender,
					 final String transaction,
					 final PluginData plugindata, final JsepSdp jsep) {
		
		this.janus = janus;
		this.sender = sender;
		this.transaction = transaction;
		this.plugindata = plugindata;
		this.jsep = jsep;
	}
	
	public static class PluginData {
		public final String plugin;
		public final Data data;
		
		public PluginData(final String plugin, final Data data) {
			this.plugin = plugin;
			this.data = data;
		}

		@NonNull
		@Override
		public String toString() {
			return "PluginData{" +
				"plugin='" + plugin + '\'' +
				", data=" + data +
				'}';
		}
	}
	
	public static class Data {
		public final String videoroom;
		/** ルームID */
		public final int room;
		public final String description;
		public final boolean configured;
		public final Object started;
		public final String audio_codec;
		public final String video_codec;
		public final BigInteger unpublished;
		public final BigInteger leaving;
		/** これは参加者のID, XXX これはStringの方がいいかも */
		public final BigInteger id;
		/** これはルームconfigで指定したID, XXX これはStringの方がいいかも */
		public final BigInteger private_id;
		public PublisherInfo[] publishers;
		public final String paused;
		public final String switched;
		public final String changes;
		public final StreamInfo[] streams;
		
		public Data(final String videoroom, final int room,
			final String description,
			final boolean configured, final Object started,
			final String audio_codec, final String video_codec,
			final BigInteger unpublished,
			final BigInteger leaving,
			final BigInteger id, final BigInteger private_id,
			final PublisherInfo[] publishers,
			final String paused,
			final String switched,
			final String changes,
			final StreamInfo[] streams) {

			this.videoroom = videoroom;
			this.room = room;
			this.description = description;
			this.configured = configured;
			this.started = started;
			this.audio_codec = audio_codec;
			this.video_codec = video_codec;
			this.unpublished = unpublished;
			this.leaving = leaving;
			this.id = id;
			this.private_id = private_id;
			this.publishers = publishers;
			this.paused = paused;
			this.switched = switched;
			this.changes = changes;
			this.streams = streams;
		}

		@NonNull
		@Override
		public String toString() {
			return "Data{" +
				"videoroom='" + videoroom + '\'' +
				", room=" + room +
				", description='" + description + '\'' +
				", configured=" + configured +
				", started=" + started +
				", audio_codec='" + audio_codec + '\'' +
				", video_codec='" + video_codec + '\'' +
				", unpublished='" + unpublished + '\'' +
				", leaving=" + leaving +
				", id=" + id +
				", private_id=" + private_id +
				", publishers=" + Arrays.toString(publishers) + '\'' +
				", paused=" + paused +
				", changes=" + changes +
				", switched=" + switched +
				", streams=" + Arrays.toString(streams) + '\'' +
				'}';
		}
	}

	@NonNull
	@Override
	public String toString() {
		return "RoomEvent{" +
			"janus='" + janus + '\'' +
			", sender='" + sender + '\'' +
			", transaction='" + transaction + '\'' +
			", plugindata=" + plugindata +
			", jsep=" + jsep +
			'}';
	}
}
