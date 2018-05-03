package com.serenegiant.janus.response;
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

import java.math.BigInteger;

public class Session {

	public final String janus;
	public final String transaction;
	public final Data data;
	
	public Session(final String janus, final String transaction, final Data data) {
		this.janus = janus;
		this.transaction = transaction;
		this.data = data;
	}
	
	public static class Data {
		public final BigInteger id;
		
		public Data(final BigInteger id) {
			this.id = id;
		}
	}
	
	public BigInteger id() {
		return data != null ? data.id : null;
	}
	
	@Override
	public String toString() {
		return "Session{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", id=" + id() +
			'}';
	}
}
