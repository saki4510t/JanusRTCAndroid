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

public class Transport {
	public final String name;
	public final String author;
	public final String description;
	public final String version_string;
	public final int version;
	
	public Transport(final String name, final String author, final String description,
		final String version_string, final int version) {

		this.name = name;
		this.author = author;
		this.description = description;
		this.version_string = version_string;
		this.version = version;
	}

	@NonNull
	@Override
	public String toString() {
		return "Transport{" +
			"name='" + name + '\'' +
			", author='" + author + '\'' +
			", description='" + description + '\'' +
			", version_string='" + version_string + '\'' +
			", version=" + version +
			'}';
	}
}
