package com.serenegiant.janus;
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

import com.serenegiant.janus.request.Hangup;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.request.videoroom.Kick;
import com.serenegiant.janus.request.videoroom.List;
import com.serenegiant.janus.response.videoroom.Kicked;
import com.serenegiant.janus.response.videoroom.ListResponse;
import com.serenegiant.janus.response.videoroom.RoomEvent;
import com.serenegiant.janus.response.videoroom.RoomInfo;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API interface of videoroom plugin on janus-gateway over http://https
 * FIXME RxJavaを使うように変える
 */
public interface VideoRoomAPI extends JanusAPI {
	@GET("{api}/{session_id}/{plugin_id}")
	public Call<ListResponse<RoomInfo>> getRoomList(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final List list);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> join(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Message message);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> offer(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Message message);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> trickle(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Trickle trickle);
	
	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> trickleCompleted(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final TrickleCompleted trickle);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<ResponseBody> send(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Message message);
	
	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Void> hangup(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Hangup hangup);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> configure(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Message message);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Kicked> kick(
		@Path("api") final String api,
		@Path("session_id") final long sessionId,
		@Path("plugin_id") final long pluginId,
		@Body final Kick kick);

}
