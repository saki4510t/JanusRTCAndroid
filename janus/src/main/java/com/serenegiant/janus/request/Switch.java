package com.serenegiant.janus.request;

import com.serenegiant.janus.response.StreamInfo;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * switchリクエスト用
 */
public class Switch {
	public final String request;
	@NonNull
	public final StreamInfo[] streams;

	public Switch(@NonNull final StreamInfo[] streams) {
		this.request = "switch";
		this.streams = streams;
	}

	@NonNull
	@Override
	public String toString() {
		return "Switch{" +
			"request='" + request + '\'' +
			", streams=" + Arrays.toString(streams) +
			'}';
	}
}
