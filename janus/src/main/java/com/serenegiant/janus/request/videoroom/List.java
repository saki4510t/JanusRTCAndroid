package com.serenegiant.janus.request.videoroom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * VideoRoomプラグイン用メッセージボディー
 * listリクエスト用
 */
public class List {
	@NonNull
	public final String request;
	@Nullable
	public final String admin_key;

	public List(@Nullable final String admin_key) {
		this.request = "list";
		this.admin_key = admin_key;
	}

	@NonNull
	@Override
	public String toString() {
		return "List{" +
			"request='" + request + '\'' +
			", admin_key='" + admin_key + '\'' +
			'}';
	}
}
