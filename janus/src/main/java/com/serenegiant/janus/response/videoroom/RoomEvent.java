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

import android.os.Parcel;
import android.os.Parcelable;

import com.serenegiant.janus.request.JsepSdp;
import com.serenegiant.janus.response.StreamInfo;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class RoomEvent {
	public final String janus;
	public final Long sender;
	public final String transaction;
	public final PluginData plugindata;
	public final JsepSdp jsep;
	
	public RoomEvent(
		final String janus, final long sender,
		final String transaction,
		final PluginData plugindata, final JsepSdp jsep) {
		
		this.janus = janus;
		this.sender = sender;
		this.transaction = transaction;
		this.plugindata = plugindata;
		this.jsep = jsep;
	}

	@NonNull
	@Override
	public String toString() {
		return "RoomEvent{" +
			"janus='" + janus + '\'' +
			", sender=" + sender +
			", transaction='" + transaction + '\'' +
			", plugindata=" + plugindata +
			", jsep=" + jsep +
			'}';
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
	
	public static class Data implements Parcelable {
		public final String videoroom;
		/** ルームID */
		public final Long room;
		public final String description;
		public final boolean configured;
		public final String started;
		public final String audio_codec;
		public final String video_codec;
		public final Long unpublished;
		public final Long leaving;
		/** これは参加者のID, XXX これはStringの方がいいかも */
		public final Long id;
		/** これはルームconfigで指定したID, XXX これはStringの方がいいかも */
		public final Long private_id;
		public PublisherInfo[] publishers;
		public final String paused;
		public final String switched;
		public final String changes;
		public final StreamInfo[] streams;
		
		public Data(final String videoroom, final long room,
			final String description,
			final boolean configured, final String started,
			final String audio_codec, final String video_codec,
			final Long unpublished,
			final Long leaving,
			final Long id, final Long private_id,
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

		protected Data(@NonNull final Parcel src) {
			videoroom = src.readString();
			if (src.readByte() == 0) {
				room = null;
			} else {
				room = src.readLong();
			}
			description = src.readString();
			configured = src.readByte() != 0;
			started = src.readString();
			audio_codec = src.readString();
			video_codec = src.readString();
			if (src.readByte() == 0) {
				unpublished = null;
			} else {
				unpublished = src.readLong();
			}
			if (src.readByte() == 0) {
				leaving = null;
			} else {
				leaving = src.readLong();
			}
			if (src.readByte() == 0) {
				id = null;
			} else {
				id = src.readLong();
			}
			if (src.readByte() == 0) {
				private_id = null;
			} else {
				private_id = src.readLong();
			}
			publishers = src.createTypedArray(PublisherInfo.CREATOR);
			paused = src.readString();
			switched = src.readString();
			changes = src.readString();
			streams = src.createTypedArray(StreamInfo.CREATOR);
		}

		public static final Creator<Data> CREATOR = new Creator<Data>() {
			@Override
			public Data createFromParcel(@NonNull final Parcel src) {
				return new Data(src);
			}

			@Override
			public Data[] newArray(int size) {
				return new Data[size];
			}
		};

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

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeString(videoroom);
			if (room == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(room);
			}
			dst.writeString(description);
			dst.writeByte((byte) (configured ? 1 : 0));
			dst.writeString(started);
			dst.writeString(audio_codec);
			dst.writeString(video_codec);
			if (unpublished == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(unpublished);
			}
			if (leaving == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(leaving);
			}
			if (id == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(id);
			}
			if (private_id == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(private_id);
			}
			dst.writeTypedArray(publishers, flags);
			dst.writeString(paused);
			dst.writeString(switched);
			dst.writeString(changes);
			dst.writeTypedArray(streams, flags);
		}
	}

}
