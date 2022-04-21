package com.serenegiant.janus.request.videoroom;

import com.serenegiant.janus.response.StreamInfo;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 */
public class Subscribe {
	public final String request;
	public final StreamInfo[] streams;

	public Subscribe(final StreamInfo[] streams) {
		this.request = "subscribe";
		this.streams = streams;
	}

	@NonNull
	@Override
	public String toString() {
		return "Subscribe{" +
			"request='" + request + '\'' +
			", streams=" + Arrays.toString(streams) +
			'}';
	}
}
