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

/**
 * listリクエストの結果
 */
class RoomInfo : Parcelable {
	val room: Long? // <unique numeric ID>,
	val description: String? // "<Name of the room>",
	val pin_required: Boolean // <true|false, whether a PIN is required to join this room>,
	val is_private: Boolean // <true|false, whether this room is 'private' (as in hidden) or not>,
	val max_publishers: Int // <how many publishers can actually publish via WebRTC at the same time>,
	val bitrate: Int // <bitrate cap that should be forced (via REMB) on all publishers by default>,
	val bitrate_cap: Boolean // <true|false, whether the above cap should act as a limit to dynamic bitrate changes by publishers (optional)>,
	val fir_freq: Int // <how often a keyframe request is sent via PLI/FIR to active publishers>,
	val require_pvtid: Boolean // <true|false, whether subscriptions in this room require a private_id>,
	val require_e2ee: Boolean // <true|false, whether end-to-end encrypted publishers are required>,
	val notify_joining: Boolean // <true|false, whether an event is sent to notify all participants if a new participant joins the room>,
	val audiocodec: String? // "<comma separated list of allowed audio codecs>",
	val videocodec: String? // "<comma separated list of allowed video codecs>",
	val opus_fec: Boolean // <true|false, whether inband FEC must be negotiated (note: only available for Opus) (optional)>,
	val opus_dtx: Boolean // <true|false, whether DTX must be negotiated (note: only available for Opus) (optional)>,
	val video_svc: Boolean // <true|false, whether SVC must be done for video (note: only available for VP9 right now) (optional)>,
	val record: Boolean // <true|false, whether the room is being recorded>,
	val rec_dir: String? // "<if recording, the path where the .mjr files are being saved>",
	val lock_record: Boolean // <true|false, whether the room recording state can only be changed providing the secret>,
	val num_participants: Int // <count of the participants (publishers, active or not; not subscribers)>
	val audiolevel_ext: Boolean // <true|false, whether the ssrc-audio-level extension must be negotiated or not for new publishers>,
	val audiolevel_event: Boolean // <true|false, whether to emit event to other users about audiolevel>,
	val audio_active_packets: Int // <amount of packets with audio level for checkup (optional, only if audiolevel_event is true)>,
	val audio_level_average: Int // <average audio level (optional, only if audiolevel_event is true)>,
	val videoorient_ext: Boolean // <true|false, whether the video-orientation extension must be negotiated or not for new publishers>,
	val playoutdelay_ext: Boolean // <true|false, whether the playout-delay extension must be negotiated or not for new publishers>,
	val transport_wide_cc_ext: Boolean // <true|false, whether the transport wide cc extension must be negotiated or not for new publishers>

	constructor(
		room: Long?,
		description: String?,
		pin_required: Boolean,
		is_private: Boolean,
		max_publishers: Int,
		bitrate: Int,
		bitrate_cap: Boolean,
		fir_freq: Int,
		require_pvtid: Boolean,
		require_e2ee: Boolean,
		notify_joining: Boolean,
		audiocodec: String?,
		videocodec: String?,
		opus_fec: Boolean,
		opus_dtx: Boolean,
		video_svc: Boolean,
		record: Boolean,
		rec_dir: String?,
		lock_record: Boolean,
		num_participants: Int,
		audiolevel_ext: Boolean,
		audiolevel_event: Boolean,
		audio_active_packets: Int,
		audio_level_average: Int,
		videoorient_ext: Boolean,
		playoutdelay_ext: Boolean,
		transport_wide_cc_ext: Boolean
	) {
		this.room = room
		this.description = description
		this.pin_required = pin_required
		this.is_private = is_private
		this.max_publishers = max_publishers
		this.bitrate = bitrate
		this.bitrate_cap = bitrate_cap
		this.fir_freq = fir_freq
		this.require_pvtid = require_pvtid
		this.require_e2ee = require_e2ee
		this.notify_joining = notify_joining
		this.audiocodec = audiocodec
		this.videocodec = videocodec
		this.opus_fec = opus_fec
		this.opus_dtx = opus_dtx
		this.video_svc = video_svc
		this.record = record
		this.rec_dir = rec_dir
		this.lock_record = lock_record
		this.num_participants = num_participants
		this.audiolevel_ext = audiolevel_ext
		this.audiolevel_event = audiolevel_event
		this.audio_active_packets = audio_active_packets
		this.audio_level_average = audio_level_average
		this.videoorient_ext = videoorient_ext
		this.playoutdelay_ext = playoutdelay_ext
		this.transport_wide_cc_ext = transport_wide_cc_ext
	}

	constructor(src: Parcel) {
		room = if (src.readByte().toInt() == 0) {
			null
		} else {
			src.readLong()
		}
		description = src.readString()
		pin_required = src.readByte().toInt() != 0
		is_private = src.readByte().toInt() != 0
		max_publishers = src.readInt()
		bitrate = src.readInt()
		bitrate_cap = src.readByte().toInt() != 0
		fir_freq = src.readInt()
		require_pvtid = src.readByte().toInt() != 0
		require_e2ee = src.readByte().toInt() != 0
		notify_joining = src.readByte().toInt() != 0
		audiocodec = src.readString()
		videocodec = src.readString()
		opus_fec = src.readByte().toInt() != 0
		opus_dtx = src.readByte().toInt() != 0
		video_svc = src.readByte().toInt() != 0
		record = src.readByte().toInt() != 0
		rec_dir = src.readString()
		lock_record = src.readByte().toInt() != 0
		num_participants = src.readInt()
		audiolevel_ext = src.readByte().toInt() != 0
		audiolevel_event = src.readByte().toInt() != 0
		audio_active_packets = src.readInt()
		audio_level_average = src.readInt()
		videoorient_ext = src.readByte().toInt() != 0
		playoutdelay_ext = src.readByte().toInt() != 0
		transport_wide_cc_ext = src.readByte().toInt() != 0
	}

	override fun toString(): String {
		return "RoomInfo{" +
			"room=" + room +
			", description='" + description + '\'' +
			", pin_required=" + pin_required +
			", is_private=" + is_private +
			", max_publishers=" + max_publishers +
			", bitrate=" + bitrate +
			", bitrate_cap=" + bitrate_cap +
			", fir_freq=" + fir_freq +
			", require_pvtid=" + require_pvtid +
			", require_e2ee=" + require_e2ee +
			", notify_joining=" + notify_joining +
			", audiocodec='" + audiocodec + '\'' +
			", videocodec='" + videocodec + '\'' +
			", opus_fec=" + opus_fec +
			", opus_dtx=" + opus_dtx +
			", video_svc=" + video_svc +
			", record=" + record +
			", rec_dir='" + rec_dir + '\'' +
			", lock_record=" + lock_record +
			", num_participants=" + num_participants +
			", audiolevel_ext=" + audiolevel_ext +
			", audiolevel_event=" + audiolevel_event +
			", audio_active_packets=" + audio_active_packets +
			", audio_level_average=" + audio_level_average +
			", videoorient_ext=" + videoorient_ext +
			", playoutdelay_ext=" + playoutdelay_ext +
			", transport_wide_cc_ext=" + transport_wide_cc_ext +
			'}'
	}

	override fun describeContents(): Int {
		return 0
	}

	override fun writeToParcel(dst: Parcel, flags: Int) {
		if (room == null) {
			dst.writeByte(0.toByte())
		} else {
			dst.writeByte(1.toByte())
			dst.writeLong(room)
		}
		dst.writeString(description)
		dst.writeByte((if (pin_required) 1 else 0).toByte())
		dst.writeByte((if (is_private) 1 else 0).toByte())
		dst.writeInt(max_publishers)
		dst.writeInt(bitrate)
		dst.writeByte((if (bitrate_cap) 1 else 0).toByte())
		dst.writeInt(fir_freq)
		dst.writeByte((if (require_pvtid) 1 else 0).toByte())
		dst.writeByte((if (require_e2ee) 1 else 0).toByte())
		dst.writeByte((if (notify_joining) 1 else 0).toByte())
		dst.writeString(audiocodec)
		dst.writeString(videocodec)
		dst.writeByte((if (opus_fec) 1 else 0).toByte())
		dst.writeByte((if (opus_dtx) 1 else 0).toByte())
		dst.writeByte((if (video_svc) 1 else 0).toByte())
		dst.writeByte((if (record) 1 else 0).toByte())
		dst.writeString(rec_dir)
		dst.writeByte((if (lock_record) 1 else 0).toByte())
		dst.writeInt(num_participants)
		dst.writeByte((if (audiolevel_ext) 1 else 0).toByte())
		dst.writeByte((if (audiolevel_event) 1 else 0).toByte())
		dst.writeInt(audio_active_packets)
		dst.writeInt(audio_level_average)
		dst.writeByte((if (videoorient_ext) 1 else 0).toByte())
		dst.writeByte((if (playoutdelay_ext) 1 else 0).toByte())
		dst.writeByte((if (transport_wide_cc_ext) 1 else 0).toByte())
	}

	companion object {
		@JvmField
		val CREATOR = object : Parcelable.Creator<RoomInfo> {
			override fun createFromParcel(src: Parcel): RoomInfo {
				return RoomInfo(src)
			}

			override fun newArray(size: Int): Array<RoomInfo?> {
				return arrayOfNulls(size)
			}
		}
	}
}
