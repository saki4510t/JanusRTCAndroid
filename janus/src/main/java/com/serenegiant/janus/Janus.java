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

import com.serenegiant.janus.request.Attach;
import com.serenegiant.janus.request.Creator;
import com.serenegiant.janus.request.Destroy;
import com.serenegiant.janus.request.Detach;
import com.serenegiant.janus.response.Plugin;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API interface of janus-gateway over http://https
 */
public interface Janus {
	@POST("{api}")
	public Call<Session> createSession(
		@Path("api") final String api,
		@Body final Creator create);

	@GET("{api}/info")
	public Call<ServerInfo> getInfo(@Path("api") final String api);

	@POST("{api}/{session_id}")
	public Call<Plugin> attachPlugin(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Body final Attach attach);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Void> detachPlugin(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Detach detach);

	@POST("{api}/{session_id}")
	public Call<Void> destroySession(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Body final Destroy destroy);
}
