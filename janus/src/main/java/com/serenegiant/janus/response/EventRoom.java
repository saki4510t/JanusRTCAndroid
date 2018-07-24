package com.serenegiant.janus.response;
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

import com.serenegiant.janus.request.JsepSdp;

import java.math.BigInteger;
import java.util.Arrays;

public class EventRoom {
	public final String janus;
	public final BigInteger sender;
	public final String transaction;
	public final PluginData plugindata;
	public final JsepSdp jsep;
	
	public EventRoom(final String janus, final BigInteger sender,
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
		public final int room;
		public final String description;
		public final boolean configured;
		public final boolean started;
		public final String audio_codec;
		public final String video_codec;
		public final String unpublished;
		public final BigInteger leaving;
		public final BigInteger id;
		public final BigInteger private_id;
		public PublisherInfo[] publishers;
		
		public Data(final String videoroom, final int room,
			final String description,
			final boolean configured, final boolean started,
			final String audio_codec, final String video_codec,
			final String unpublished,
			final BigInteger leaving,
			final BigInteger id, final BigInteger private_id,
			final PublisherInfo[] publishers) {

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
		}
		
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
				", publishers=" + Arrays.toString(publishers) +
				'}';
		}
	}
	
	@Override
	public String toString() {
		return "EventRoom{" +
			"janus='" + janus + '\'' +
			", sender='" + sender + '\'' +
			", transaction='" + transaction + '\'' +
			", plugindata=" + plugindata +
			", jsep=" + jsep +
			'}';
	}
}
