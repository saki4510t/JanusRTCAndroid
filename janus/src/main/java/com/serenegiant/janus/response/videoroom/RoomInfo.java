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

import androidx.annotation.NonNull;

/**
 * listリクエストの結果
 */
public class RoomInfo {
	public final Long room;					// <unique numeric ID>,
	public final String description;		// "<Name of the room>",
	public final boolean pin_required;		// <true|false, whether a PIN is required to join this room>,
	public final boolean is_private;		// <true|false, whether this room is 'private' (as in hidden) or not>,
	public final int max_publishers;		// <how many publishers can actually publish via WebRTC at the same time>,
	public final int bitrate;				// <bitrate cap that should be forced (via REMB) on all publishers by default>,
	public final boolean bitrate_cap;		// <true|false, whether the above cap should act as a limit to dynamic bitrate changes by publishers (optional)>,
	public final int fir_freq;				// <how often a keyframe request is sent via PLI/FIR to active publishers>,
	public final boolean require_pvtid;		// <true|false, whether subscriptions in this room require a private_id>,
	public final boolean require_e2ee;		// <true|false, whether end-to-end encrypted publishers are required>,
	public final boolean notify_joining;	// <true|false, whether an event is sent to notify all participants if a new participant joins the room>,
	public final String audiocodec;			// "<comma separated list of allowed audio codecs>",
	public final String videocodec;			// "<comma separated list of allowed video codecs>",
	public final boolean opus_fec;			// <true|false, whether inband FEC must be negotiated (note: only available for Opus) (optional)>,
	public final boolean opus_dtx;			// <true|false, whether DTX must be negotiated (note: only available for Opus) (optional)>,
	public final boolean video_svc;			// <true|false, whether SVC must be done for video (note: only available for VP9 right now) (optional)>,
	public final boolean record;			// <true|false, whether the room is being recorded>,
	public final String rec_dir;			// "<if recording, the path where the .mjr files are being saved>",
	public final boolean lock_record;		// <true|false, whether the room recording state can only be changed providing the secret>,
	public final int num_participants;		// <count of the participants (publishers, active or not; not subscribers)>
	public final boolean audiolevel_ext;	// <true|false, whether the ssrc-audio-level extension must be negotiated or not for new publishers>,
	public final boolean audiolevel_event;	// <true|false, whether to emit event to other users about audiolevel>,
	public final int audio_active_packets;	// <amount of packets with audio level for checkup (optional, only if audiolevel_event is true)>,
	public final int audio_level_average;	// <average audio level (optional, only if audiolevel_event is true)>,
	public final boolean videoorient_ext;	// <true|false, whether the video-orientation extension must be negotiated or not for new publishers>,
	public final boolean playoutdelay_ext;	// <true|false, whether the playout-delay extension must be negotiated or not for new publishers>,
	public final boolean transport_wide_cc_ext; // <true|false, whether the transport wide cc extension must be negotiated or not for new publishers>

	public RoomInfo(final Long room, final String description, final boolean pin_required, final boolean is_private, final int max_publishers, final int bitrate, final boolean bitrate_cap, final int fir_freq, final boolean require_pvtid, final boolean require_e2ee, final boolean notify_joining, final String audiocodec, final String videocodec, final boolean opus_fec, final boolean opus_dtx, final boolean video_svc, final boolean record, final String rec_dir, final boolean lock_record, final int num_participants, final boolean audiolevel_ext, final boolean audiolevel_event, final int audio_active_packets, final int audio_level_average, final boolean videoorient_ext, final boolean playoutdelay_ext, final boolean transport_wide_cc_ext) {
		this.room = room;
		this.description = description;
		this.pin_required = pin_required;
		this.is_private = is_private;
		this.max_publishers = max_publishers;
		this.bitrate = bitrate;
		this.bitrate_cap = bitrate_cap;
		this.fir_freq = fir_freq;
		this.require_pvtid = require_pvtid;
		this.require_e2ee = require_e2ee;
		this.notify_joining = notify_joining;
		this.audiocodec = audiocodec;
		this.videocodec = videocodec;
		this.opus_fec = opus_fec;
		this.opus_dtx = opus_dtx;
		this.video_svc = video_svc;
		this.record = record;
		this.rec_dir = rec_dir;
		this.lock_record = lock_record;
		this.num_participants = num_participants;
		this.audiolevel_ext = audiolevel_ext;
		this.audiolevel_event = audiolevel_event;
		this.audio_active_packets = audio_active_packets;
		this.audio_level_average = audio_level_average;
		this.videoorient_ext = videoorient_ext;
		this.playoutdelay_ext = playoutdelay_ext;
		this.transport_wide_cc_ext = transport_wide_cc_ext;
	}

	@NonNull
	@Override
	public String toString() {
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
			'}';
	}
}
