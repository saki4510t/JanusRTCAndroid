package com.serenegiant.janus.request;
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.math.BigInteger;

/**
 * message body
 */
public class Join {
	public final String request;
	public final int room;
	public final String ptype;
	public final String display;
	public final BigInteger feed;
	
	public Join(final int room, @NonNull final String pType,
		@Nullable final String display,
		@Nullable final BigInteger feed) {

		this.request = "join";
		this.room = room;
		this.ptype = pType;
		this.display = display;
		this.feed = feed;
	}
	
	@Override
	public String toString() {
		return "Join{" +
			"request='" + request + '\'' +
			", room=" + room +
			", ptype='" + ptype + '\'' +
			", display='" + display + '\'' +
			", feed='" + feed + '\'' +
			'}';
	}
}
