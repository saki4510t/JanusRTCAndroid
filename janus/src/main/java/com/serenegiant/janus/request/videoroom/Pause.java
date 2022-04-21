package com.serenegiant.janus.request.videoroom;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 */
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
