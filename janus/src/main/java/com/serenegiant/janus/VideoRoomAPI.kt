package com.serenegiant.janus
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

import com.serenegiant.janus.request.Hangup
import com.serenegiant.janus.request.Message
import com.serenegiant.janus.request.Trickle
import com.serenegiant.janus.request.TrickleCompleted
import com.serenegiant.janus.request.videoroom.List
import com.serenegiant.janus.response.videoroom.ListResponse
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.serenegiant.janus.response.videoroom.RoomInfo
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API interface of videoroom plugin on janus-gateway over http://https
 */
interface VideoRoomAPI : JanusAPI {
	@GET("{api}/{session_id}/{plugin_id}")
	suspend fun getRoomList(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body list: List?
	): ListResponse<RoomInfo>

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun join(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body message: Message
	): RoomEvent

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun offer(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body message: Message
	): RoomEvent

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun trickle(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body trickle: Trickle
	): RoomEvent

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun trickleCompleted(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body trickle: TrickleCompleted
	): RoomEvent

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun send(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body message: Message
	): ResponseBody

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun hangup(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body hangup: Hangup
	)

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun configure(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body message: Message
	): RoomEvent

	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun kick(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body message: Message
	): RoomEvent
}
