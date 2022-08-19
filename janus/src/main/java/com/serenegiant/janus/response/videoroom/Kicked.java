package com.serenegiant.janus.response.videoroom;
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

/**
 * kickリクエストの結果
 */
public class Kicked {
	@NonNull
	public final String videoroom;

	public Kicked(@NonNull final String videoroom) {
		this.videoroom = videoroom;
	}

	@NonNull
	@Override
	public String toString() {
		return "Configured{" +
			"videoroom='" + videoroom + '\'' +
			'}';
	}
}
