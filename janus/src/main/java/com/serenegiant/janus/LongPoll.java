package com.serenegiant.janus;
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

import com.serenegiant.janus.response.EventRoom;

import java.math.BigInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * API interface of janus-gateway for long poll over http/https
 */
public interface LongPoll {
	@GET("{api}/{session_id}")
	public Call<ResponseBody> getEvent(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId);

	@GET("{api}/{session_id}")
	public Call<EventRoom> getRoomEvent(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId);
}
