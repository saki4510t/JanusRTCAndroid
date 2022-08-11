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

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * listparticipantsリクエストの結果
 */
public class Participants {
	public final String videoroom;
	public Long room;
	public Participant[] participants;

	public Participants(final String videoroom, final Long room, final Participant[] participants) {
		this.videoroom = videoroom;
		this.room = room;
		this.participants = participants;
	}

	public static class Participant {
		public final Long id;		// <unique numeric ID of the participant>,
		public final String display;	// "<display name of the participant, if any; optional>",
		public final boolean publisher;	// "<true|false, whether user is an active publisher in the room>",
		public final boolean talking;	// <true|false, whether user is talking or not (only if audio levels are used)>

		public Participant(final Long id, final String display, final boolean publisher, final boolean talking) {
			this.id = id;
			this.display = display;
			this.publisher = publisher;
			this.talking = talking;
		}

		@NonNull
		@Override
		public String toString() {
			return "Participant{" +
				"id=" + id +
				", display='" + display + '\'' +
				", publisher=" + publisher +
				", talking=" + talking +
				'}';
		}
	}

	@NonNull
	@Override
	public String toString() {
		return "Participants{" +
			"videoroom='" + videoroom + '\'' +
			", room=" + room +
			", participants=" + Arrays.toString(participants) +
			'}';
	}
}
