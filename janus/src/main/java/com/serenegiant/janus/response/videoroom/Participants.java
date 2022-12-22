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

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * listparticipantsリクエストの結果
 */
public class Participants implements Parcelable {
	public final String videoroom;
	public Long room;
	public Participant[] participants;

	public Participants(final String videoroom, final Long room, final Participant[] participants) {
		this.videoroom = videoroom;
		this.room = room;
		this.participants = participants;
	}

	protected Participants(@NonNull final Parcel src) {
		videoroom = src.readString();
		if (src.readByte() == 0) {
			room = null;
		} else {
			room = src.readLong();
		}
		participants = src.createTypedArray(Participant.CREATOR);
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
		dst.writeTypedArray(participants, flags);
	}

	public static final Creator<Participants> CREATOR = new Creator<Participants>() {
		@Override
		public Participants createFromParcel(@NonNull final Parcel src) {
			return new Participants(src);
		}

		@Override
		public Participants[] newArray(final int size) {
			return new Participants[size];
		}
	};

	public static class Participant implements Parcelable {
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

		protected Participant(@NonNull final Parcel src) {
			if (src.readByte() == 0) {
				id = null;
			} else {
				id = src.readLong();
			}
			display = src.readString();
			publisher = src.readByte() != 0;
			talking = src.readByte() != 0;
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

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			if (id == null) {
				dst.writeByte((byte) 0);
			} else {
				dst.writeByte((byte) 1);
				dst.writeLong(id);
			}
			dst.writeString(display);
			dst.writeByte((byte) (publisher ? 1 : 0));
			dst.writeByte((byte) (talking ? 1 : 0));
		}

		public static final Creator<Participant> CREATOR = new Creator<Participant>() {
			@Override
			public Participant createFromParcel(@NonNull final Parcel src) {
				return new Participant(src);
			}

			@Override
			public Participant[] newArray(final int size) {
				return new Participant[size];
			}
		};
	}
}
