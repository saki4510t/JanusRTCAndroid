package com.serenegiant.janus.response.videoroom;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * listリクエストの結果
 */
public class ListResponse {
	public final String videoroom;
	public final RoomInfo[] list;

	public ListResponse(final String videoroom, final RoomInfo[] list) {
		this.videoroom = videoroom;
		this.list = list;
	}

	@NonNull
	@Override
	public String toString() {
		return "ListResponse{" +
			"videoroom='" + videoroom + '\'' +
			", list=" + Arrays.toString(list) +
			'}';
	}
}
