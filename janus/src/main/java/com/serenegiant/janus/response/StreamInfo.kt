package com.serenegiant.janus.response
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2025 saki t_saki@serenegiant.com
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

/**
 * subscribe/switchリクエストの結果
 */
class StreamInfo : Parcelable {
	val feed: Long? // <unique ID of the publisher the new source is from>,
	val mid: String? // "<unique mid of the source we want to switch to>"
	val sub_mid: String? // "<unique mid of the stream we want to pipe the new source to>"

	// Optionally, simulcast or SVC targets (defaults if missing)
	constructor(feed: Long?, mid: String?, sub_mid: String?) {
		this.feed = feed
		this.mid = mid
		this.sub_mid = sub_mid
	}

	protected constructor(src: Parcel) {
		feed = if (src.readByte().toInt() == 0) {
			null
		} else {
			src.readLong()
		}
		mid = src.readString()
		sub_mid = src.readString()
	}

	override fun toString(): String {
		return "StreamInfo{" +
			"feed=" + feed +
			", mid=" + mid +
			", sub_mid=" + sub_mid +
			'}'
	}

	override fun describeContents(): Int {
		return 0
	}

	override fun writeToParcel(dst: Parcel, flags: Int) {
		if (feed == null) {
			dst.writeByte(0.toByte())
		} else {
			dst.writeByte(1.toByte())
			dst.writeLong(feed)
		}
		dst.writeString(mid)
		dst.writeString(sub_mid)
	}

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<StreamInfo> {
			override fun createFromParcel(src: Parcel): StreamInfo {
				return StreamInfo(src)
			}

			override fun newArray(size: Int): Array<StreamInfo?> {
				return arrayOfNulls(size)
			}
		}
	}
}
