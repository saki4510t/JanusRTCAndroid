package com.serenegiant.janus.response.videoroom
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
 * listparticipantsリクエストの結果
 */
class Participants : Parcelable {
	val videoroom: String?
	var room: Long? = null
	var participants: Array<Participant>?

	constructor(videoroom: String?, room: Long?, participants: Array<Participant>?) {
		this.videoroom = videoroom
		this.room = room
		this.participants = participants
	}

	protected constructor(src: Parcel) {
		videoroom = src.readString()
		room = if (src.readByte().toInt() == 0) {
			null
		} else {
			src.readLong()
		}
		participants = src.createTypedArray(Participant.CREATOR)
	}

	override fun toString(): String {
		return "Participants{" +
			"videoroom='" + videoroom + '\'' +
			", room=" + room +
			", participants=" + participants.contentToString() +
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
			dst.writeLong(room!!)
		}
		dst.writeTypedArray(participants, flags)
	}

	class Participant : Parcelable {
		val id: Long? // <unique numeric ID of the participant>,
		val display: String? // "<display name of the participant, if any; optional>",
		val publisher: Boolean // "<true|false, whether user is an active publisher in the room>",
		val talking: Boolean // <true|false, whether user is talking or not (only if audio levels are used)>

		constructor(id: Long?, display: String?, publisher: Boolean, talking: Boolean) {
			this.id = id
			this.display = display
			this.publisher = publisher
			this.talking = talking
		}

		protected constructor(src: Parcel) {
			id = if (src.readByte().toInt() == 0) {
				null
			} else {
				src.readLong()
			}
			display = src.readString()
			publisher = src.readByte().toInt() != 0
			talking = src.readByte().toInt() != 0
		}

		override fun toString(): String {
			return "Participant{" +
				"id=" + id +
				", display='" + display + '\'' +
				", publisher=" + publisher +
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
			dst.writeByte((if (publisher) 1 else 0).toByte())
			dst.writeByte((if (talking) 1 else 0).toByte())
		}

		companion object {
			@JvmField
			val CREATOR = object : Parcelable.Creator<Participant> {
					override fun createFromParcel(src: Parcel): Participant {
						return Participant(src)
					}

					override fun newArray(size: Int): Array<Participant?> {
						return arrayOfNulls(size)
					}
				}
		}
	}

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<Participants> {
			override fun createFromParcel(src: Parcel): Participants {
				return Participants(src)
			}

			override fun newArray(size: Int): Array<Participants?> {
				return arrayOfNulls(size)
			}
		}
	}
}
