package com.serenegiant.janus.request;

import java.math.BigInteger;

import androidx.annotation.NonNull;

public class ListParticipants {
	@NonNull
	public final String request;
	@NonNull
	public final BigInteger room;

	public ListParticipants(@NonNull final BigInteger room) {
		this.request = "listparticipants";
		this.room = room;
	}

	@NonNull
	@Override
	public String toString() {
		return "ListParticipants{" +
			"request='" + request + '\'' +
			", room=" + room +
			'}';
	}
}
