package com.serenegiant.janus.response.videoroom;

import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * listリクエストの結果
 */
public class ListResponse<T> {
	public final String videoroom;
	public final T[] list;

	public ListResponse(final String videoroom, final T[] list) {
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
