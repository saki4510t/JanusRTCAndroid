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
import com.serenegiant.janus.request.Hangup;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.response.EventRoom;
import com.serenegiant.janus.response.Plugin;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API interface of videoroom plugin on janus-gateway over http://https
 */
public interface VideoRoom {
	@POST("janus")
	public Call<Session> create(@Body final Creator create);

	@GET("janus/info")
	public Call<ServerInfo> getInfo();

	@POST("janus/{session_id}")
	public Call<Plugin> attach(
		@Path("session_id") final BigInteger sessionId,
		@Body final Attach attach);
	
	@POST("janus/{session_id}/{plugin_id}")
	public Call<EventRoom> join(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);

	@POST("janus/{session_id}/{plugin_id}")
	public Call<EventRoom> offer(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);

	@POST("janus/{session_id}/{plugin_id}")
	public Call<EventRoom> trickle(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Trickle trickle);
	
	@POST("janus/{session_id}/{plugin_id}")
	public Call<EventRoom> trickleCompleted(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final TrickleCompleted trickle);

	@POST("janus/{session_id}/{plugin_id}")
	public Call<ResponseBody> send(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);
	
	@POST("janus/{session_id}/{plugin_id}")
	public Call<Void> detach(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Detach detach);
	
	@POST("janus/{session_id}/{plugin_id}")
	public Call<Void> hangup(
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Hangup hangup);
	
	@POST("janus/{session_id}")
	public Call<Void> destroy(
		@Path("session_id") final BigInteger sessionId,
		@Body final Destroy destroy);
}
