package com.serenegiant.janus.request.videoroom;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 */
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
