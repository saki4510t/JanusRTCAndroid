package com.serenegiant.janus.response;

import java.math.BigInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * subscribe/switchリクエストの結果
 */
public class StreamInfo {
	public final BigInteger feed;	// <unique ID of the publisher the new source is from>,
	public final BigInteger mid;	// "<unique mid of the source we want to switch to>",
	@Nullable
	public final BigInteger sub_mid;// "<unique mid of the stream we want to pipe the new source to>"
	// Optionally, simulcast or SVC targets (defaults if missing)

	public StreamInfo(final BigInteger feed, final BigInteger mid, @Nullable final BigInteger sub_mid) {
		this.feed = feed;
		this.mid = mid;
		this.sub_mid = sub_mid;
	}

	@NonNull
	@Override
	public String toString() {
		return "StreamInfo{" +
			"feed=" + feed +
			", mid=" + mid +
			", sub_mid=" + sub_mid +
			'}';
	}
}
