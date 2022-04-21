package com.serenegiant.janus.request.videoroom;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 * unpublishリクエスト用
 */
public class UnPublish {
	public final String request = "unpublish";

	@NonNull
	@Override
	public String toString() {
		return "UnPublish{" +
			"request='" + request + '\'' +
			'}';
	}
}
