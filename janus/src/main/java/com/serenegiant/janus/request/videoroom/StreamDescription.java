package com.serenegiant.janus.request.videoroom;
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 saki t_saki@serenegiant.com
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

public class StreamDescription {
	@NonNull
	public final String mid;		// "<unique mid of a stream being published>"
	@NonNull
	public final String description; // "<text description of the stream (e.g., My front webcam)>"

	public StreamDescription(@NonNull final String mid, @NonNull final String description) {
		this.mid = mid;
		this.description = description;
	}

	@NonNull
	@Override
	public String toString() {
		return "Description{" +
			"mid=" + mid +
			", description='" + description + '\'' +
			'}';
	}
}
