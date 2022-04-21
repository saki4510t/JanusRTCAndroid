package com.serenegiant.janus.request.videoroom;

import com.serenegiant.janus.response.StreamInfo;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 * unsubscribeリクエスト用(受信するリモートストリーム設定を削除する時)
 * ICEが再起動される
 */
public class UnSubscribe {
	public final String request;
	public final StreamInfo[] streams;

	public UnSubscribe(final StreamInfo[] streams) {
		this.request = "unsubscribe";
		this.streams = streams;
	}

	@NonNull
	@Override
	public String toString() {
		return "UnSubscribe{" +
			"request='" + request + '\'' +
			", streams=" + Arrays.toString(streams) +
			'}';
	}
}
