package com.serenegiant.janus.request.videoroom;

import androidx.annotation.NonNull;

/**
 * VideoRoomプラグイン用メッセージボディー
 * サブスクライバーでのデータ取得を停止する
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
