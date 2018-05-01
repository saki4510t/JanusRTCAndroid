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

import com.serenegiant.janus.Room;
import com.serenegiant.janus.TransactionManager;

import java.math.BigInteger;

public class TrickleCompleted {
	@NonNull
	public final String janus;
	@NonNull
	public final String transaction;
	@NonNull
	public final BigInteger session_id;
	@NonNull
	public final BigInteger handle_id;
	@NonNull
	public final Candidate candidate;

	public TrickleCompleted(@NonNull final BigInteger session_id,
		@NonNull final BigInteger handle_id,
		@Nullable final TransactionManager.TransactionCallback callback) {

		this.janus = "trickle";
		this.transaction = TransactionManager.get(12, callback);
		this.session_id = session_id;
		this.handle_id = handle_id;
		this.candidate = new Candidate();
	}
	
	public TrickleCompleted(@NonNull final Room room,
		@Nullable final TransactionManager.TransactionCallback callback) {

	   this(room.sessionId, room.pluginId, callback);
	}
	
	public static class Candidate {
		public final boolean completed;
		
		public Candidate() {
			this.completed = true;
		}
		
		@Override
		public String toString() {
			return "Candidate{" +
				"completed=" + completed +
				'}';
		}
	}

	@Override
	public String toString() {
		return "Trickle{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", session_id=" + session_id +
			", candidate=" + candidate +
			'}';
	}
}
