package com.serenegiant.janus.request;

import androidx.annotation.NonNull;

/**
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
