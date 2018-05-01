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

import com.serenegiant.janus.TransactionManager;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;

public class Destroy {
	@NonNull
	public final String janus;
	@NonNull
	public final String transaction;
	@NonNull
	public final BigInteger session_id;
	
	public Destroy(@NonNull final BigInteger session_id,
		@Nullable final TransactionManager.TransactionCallback callback) {

		this.janus = "destroy";
		this.transaction = TransactionManager.get(12, callback);
		this.session_id = session_id;
	}
	
	public Destroy(@NonNull final Session session,
		@NonNull final TransactionManager.TransactionCallback callback) {
		this(session.data.id, callback);
	}
	
	@Override
	public String toString() {
		return "Destroy{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", session_id=" + session_id +
			'}';
	}
}
