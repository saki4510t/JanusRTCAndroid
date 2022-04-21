package com.serenegiant.janus.response.videoroom;

import java.math.BigInteger;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * listparticipantsリクエストの結果
 */
public class Participants {
	public final String videoroom;
	public BigInteger room;
	public Participant[] participants;

	public Participants(final String videoroom, final BigInteger room, final Participant[] participants) {
		this.videoroom = videoroom;
		this.room = room;
		this.participants = participants;
	}

	public static class Participant {
		public final BigInteger id;		// <unique numeric ID of the participant>,
		public final String display;	// "<display name of the participant, if any; optional>",
		public final boolean publisher;	// "<true|false, whether user is an active publisher in the room>",
		public final boolean talking;	// <true|false, whether user is talking or not (only if audio levels are used)>

		public Participant(final BigInteger id, final String display, final boolean publisher, final boolean talking) {
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
