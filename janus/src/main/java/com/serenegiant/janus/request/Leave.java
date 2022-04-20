package com.serenegiant.janus.request;

import androidx.annotation.NonNull;

public class Leave {
	public final String request = "leave";

	@NonNull
	@Override
	public String toString() {
		return "Leave{" +
			"request='" + request + '\'' +
			'}';
	}
}
