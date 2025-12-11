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

import com.serenegiant.janus.request.Attach
import com.serenegiant.janus.request.CreateSession
import com.serenegiant.janus.request.DestroySession
import com.serenegiant.janus.request.Detach
import com.serenegiant.janus.request.Hangup
import com.serenegiant.janus.request.Message
import com.serenegiant.janus.request.Trickle
import com.serenegiant.janus.request.TrickleCompleted
import com.serenegiant.janus.response.Event
import com.serenegiant.janus.response.PluginInfo
import com.serenegiant.janus.response.ServerInfo
import com.serenegiant.janus.response.Session
import com.serenegiant.janus.response.videoroom.RoomEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send

/**
 * API interface of videoroom plugin on janus-gateway over websocket
 */
interface VideoRoomWSAPI {
	@Send
	suspend fun getInfo(): ServerInfo?

	/**
	 * セッションを作成
	 * セッションエンドポイント
	 * @param create
	 * @return
	 */
	@Send
	suspend fun createSession(create: CreateSession): Session?

	/**
	 * 指定したプラグインへ接続
	 * セッションエンドポイント
	 * @param attach
	 * @return
	 */
	@Send
	suspend fun attachPlugin(attach: Attach): PluginInfo?

	/**
	 * 指定したプラグインから切断
	 * これ自体はプラグインエンドポイントだけど#attachPluginの対なのでセッションエンドポイントとしてここに入れておく
	 * @param detach
	 * @return
	 */
	@Send
	suspend fun detachPlugin(detach: Detach)

	/**
	 * セッションを破棄
	 * セッションエンドポイント
	 * @param destroy
	 * @return
	 */
	@Send
	suspend fun destroySession(destroy: DestroySession)

	//--------------------------------------------------------------------------------
	// ここからしたがvideoroomプラグイン固有のエンドポイント定義
	//--------------------------------------------------------------------------------
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
