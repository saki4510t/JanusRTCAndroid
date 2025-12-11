package com.serenegiant.janus.response.videoroom
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

import android.os.Parcel
import android.os.Parcelable
import com.serenegiant.janus.request.JsepSdp
import com.serenegiant.janus.response.StreamInfo

class RoomEvent(
	@JvmField val janus: String,
	@JvmField val sender: Long,
	@JvmField val transaction: String,
	@JvmField val plugindata: PluginData,
	@JvmField val jsep: JsepSdp
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

	class PluginData(val plugin: String, @JvmField val data: Data) {
		override fun toString(): String {
			return "PluginData{" +
				"plugin='" + plugin + '\'' +
				", data=" + data +
				'}'
		}
	}

	class Data : Parcelable {
		@JvmField
		val videoroom: String?

		/** ルームID  */
		val room: Long?
		val description: String?
		val configured: Boolean
		val started: String?
		val audio_codec: String?
		val video_codec: String?
		@JvmField
		val unpublished: Long?
		@JvmField
		val leaving: Long?

		/** これは参加者のID, XXX これはStringの方がいいかも  */
		@JvmField
		val id: Long?

		/** これはルームconfigで指定したID, XXX これはStringの方がいいかも  */
		val private_id: Long?
		@JvmField
		var publishers: Array<PublisherInfo>?
		val paused: String?
		val switched: String?
		val changes: String?
		val streams: Array<StreamInfo>?

		constructor(
			videoroom: String?, room: Long,
			description: String?,
			configured: Boolean, started: String?,
			audio_codec: String?, video_codec: String?,
			unpublished: Long?,
			leaving: Long?,
			id: Long?, private_id: Long?,
			publishers: Array<PublisherInfo>?,
			paused: String?,
			switched: String?,
			changes: String?,
			streams: Array<StreamInfo>?
		) {
			this.videoroom = videoroom
			this.room = room
			this.description = description
			this.configured = configured
			this.started = started
			this.audio_codec = audio_codec
			this.video_codec = video_codec
			this.unpublished = unpublished
			this.leaving = leaving
			this.id = id
			this.private_id = private_id
			this.publishers = publishers
			this.paused = paused
			this.switched = switched
			this.changes = changes
			this.streams = streams
		}

		protected constructor(src: Parcel) {
			videoroom = src.readString()
			room = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			description = src.readString()
			configured = src.readByte().toInt() != 0
			started = src.readString()
			audio_codec = src.readString()
			video_codec = src.readString()
			unpublished = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			leaving = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			id = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			private_id = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			publishers = src.createTypedArray(PublisherInfo.CREATOR)
			paused = src.readString()
			switched = src.readString()
			changes = src.readString()
			streams = src.createTypedArray(StreamInfo.CREATOR)
		}

		override fun toString(): String {
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
				", publishers=" + publishers.contentToString() + '\'' +
				", paused=" + paused +
				", changes=" + changes +
				", switched=" + switched +
				", streams=" + streams.contentToString() + '\'' +
				'}'
		}

		override fun describeContents(): Int {
			return 0
		}

		override fun writeToParcel(dst: Parcel, flags: Int) {
			dst.writeString(videoroom)
			if (room == null) {
				dst.writeByte(0.toByte())
			} else {
				dst.writeByte(1.toByte())
				dst.writeLong(room)
			}
			dst.writeString(description)
			dst.writeByte((if (configured) 1 else 0).toByte())
			dst.writeString(started)
			dst.writeString(audio_codec)
			dst.writeString(video_codec)
			if (unpublished == null) {
				dst.writeByte(0.toByte())
			} else {
				dst.writeByte(1.toByte())
				dst.writeLong(unpublished)
			}
			if (leaving == null) {
				dst.writeByte(0.toByte())
			} else {
				dst.writeByte(1.toByte())
				dst.writeLong(leaving)
			}
			if (id == null) {
				dst.writeByte(0.toByte())
			} else {
				dst.writeByte(1.toByte())
				dst.writeLong(id)
			}
			if (private_id == null) {
				dst.writeByte(0.toByte())
			} else {
				dst.writeByte(1.toByte())
				dst.writeLong(private_id)
			}
			dst.writeTypedArray(publishers, flags)
			dst.writeString(paused)
			dst.writeString(switched)
			dst.writeString(changes)
			dst.writeTypedArray(streams, flags)
		}

		companion object {
			@JvmField
			val CREATOR = object : Parcelable.Creator<Data> {
				override fun createFromParcel(src: Parcel): Data {
					return Data(src)
				}

				override fun newArray(size: Int): Array<Data?> {
					return arrayOfNulls(size)
				}
			}
		}
	}
}
