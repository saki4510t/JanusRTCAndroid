package com.serenegiant.janus.response;
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2022 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
*/

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * subscribe/switchリクエストの結果
 */
public class StreamInfo {
	public final Long feed;	// <unique ID of the publisher the new source is from>,
	@Nullable
	public final String mid;		// "<unique mid of the source we want to switch to>"
	@Nullable
	public final String sub_mid;// "<unique mid of the stream we want to pipe the new source to>"
	// Optionally, simulcast or SVC targets (defaults if missing)

	public StreamInfo(final Long feed, @Nullable final String mid, @Nullable final String sub_mid) {
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
