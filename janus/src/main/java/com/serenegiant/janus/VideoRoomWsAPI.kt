package com.serenegiant.janus
/*
* JanusRTCAndroid
* Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
*
* Copyright (c) 2018 - 2026 saki t_saki@serenegiant.com
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
import com.serenegiant.janus.response.Event
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send

/**
 * API interface of videoroom plugin on janus-gateway over websocket
 * videoroomプラグイン固有のエンドポイント定義
 */
internal interface VideoRoomWsAPI : JanusWsAPI {
	@Send
	suspend fun join(message: Message): RoomEvent?

	@Send
	suspend fun offer(message: Message): RoomEvent?

	@Send
	suspend fun trickle(trickle: Trickle): RoomEvent?

	@Send
	suspend fun trickleCompleted(trickle: TrickleCompleted): RoomEvent?

	@Send
	suspend fun send(message: Message): Any?

	@Send
	suspend fun hangup(hangup: Hangup)

	@Receive
	suspend fun observeEvent(): Event?

	@Receive
	suspend fun observeRoomEvent(): RoomEvent?
}
