package com.serenegiant.janus.response.videoroom
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

import android.os.Parcel
import android.os.Parcelable

class PublisherInfo : Parcelable {
	/** パブリッシャーのID, XXX これはStringの方がいいのかも  */
	val id: Long?
	val display: String?
	val audio_codec: String?
	val video_codec: String?
	var talking: Boolean

	constructor(
		id: Long?,
		display: String?,
		audio_codec: String?, video_codec: String?,
		talking: Boolean
	) {
		this.id = id
		this.display = display
		this.audio_codec = audio_codec
		this.video_codec = video_codec
		this.talking = talking
	}

	constructor(src: Parcel) {
		id = if (src.readByte().toInt() == 0) {
			null
		} else {
			src.readLong()
		}
		display = src.readString()
		audio_codec = src.readString()
		video_codec = src.readString()
		talking = src.readByte().toInt() != 0
	}

	/**
	 * 引数がPublisherの場合にidの比較のみを行う
	 * @param other
	 * @return
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || javaClass != other.javaClass) return false
		val publisher = other as PublisherInfo

		return (id != null) && id == publisher.id
	}

	/**
	 * idのhashCodeを返す
	 * @return
	 */
	override fun hashCode(): Int {
		return id?.hashCode() ?: super.hashCode()
	}

	override fun toString(): String {
		return "PublisherInfo{" +
			"id=" + id +
			", display='" + display + '\'' +
			", audio_codec='" + audio_codec + '\'' +
			", video_codec='" + video_codec + '\'' +
			", talking=" + talking +
			'}'
	}

	override fun describeContents(): Int {
		return 0
	}

	override fun writeToParcel(dst: Parcel, flags: Int) {
		if (id == null) {
			dst.writeByte(0.toByte())
		} else {
			dst.writeByte(1.toByte())
			dst.writeLong(id)
		}
		dst.writeString(display)
		dst.writeString(audio_codec)
		dst.writeString(video_codec)
		dst.writeByte((if (talking) 1 else 0).toByte())
	}

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<PublisherInfo> {
			override fun createFromParcel(src: Parcel): PublisherInfo {
				return PublisherInfo(src)
			}

			override fun newArray(size: Int): Array<PublisherInfo?> {
				return arrayOfNulls(size)
			}
		}
	}
}
