package com.serenegiant.janus.request;

import androidx.annotation.NonNull;

public class Pause {
	public final String request ="pause";

	@NonNull
	@Override
	public String toString() {
		return "Pause{" +
			"request='" + request + '\'' +
			'}';
	}
}
