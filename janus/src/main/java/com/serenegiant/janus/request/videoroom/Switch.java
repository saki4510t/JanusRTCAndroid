package com.serenegiant.janus.request.videoroom;

import com.serenegiant.janus.response.StreamInfo;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 * switchリクエスト用(feed/mdi/sub_midを切り替える場合)
 * こちらはsubscribeと違ってICEが再起動されない
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
