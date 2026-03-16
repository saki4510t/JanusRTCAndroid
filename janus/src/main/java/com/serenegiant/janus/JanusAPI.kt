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

import com.serenegiant.janus.request.Attach
import com.serenegiant.janus.request.CreateSession
import com.serenegiant.janus.request.DestroySession
import com.serenegiant.janus.request.Detach
import com.serenegiant.janus.response.PluginInfo
import com.serenegiant.janus.response.ServerInfo
import com.serenegiant.janus.response.Session
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * API interface of janus-gateway over http://https
 */
internal interface JanusAPI {
	/**
	 * janus-gatewayサーバーの情報を取得
	 * @param api
	 * @return
	 */
	@GET("{api}/info")
	suspend fun getInfo(@Path("api") api: String): ServerInfo

	/**
	 * セッションを作成
	 * セッションエンドポイント
	 * @param api
	 * @param create
	 * @return
	 */
	@POST("{api}")
	suspend fun createSession(
		@Path("api") api: String,
		@Body create: CreateSession
	): Session

	/**
	 * 指定したプラグインへ接続
	 * セッションエンドポイント
	 * @param api
	 * @param sessionId
	 * @param attach
	 * @return
	 */
	@POST("{api}/{session_id}")
	suspend fun attachPlugin(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Body attach: Attach
	): PluginInfo

	/**
	 * 指定したプラグインから切断
	 * これ自体はプラグインエンドポイントだけど#attachPluginの対なのでセッションエンドポイントとしてここに入れておく
	 * @param api
	 * @param sessionId
	 * @param pluginId
	 * @param detach
	 * @return
	 */
	@POST("{api}/{session_id}/{plugin_id}")
	suspend fun detachPlugin(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Path("plugin_id") pluginId: Long,
		@Body detach: Detach
	)

	/**
	 * セッションを破棄
	 * セッションエンドポイント
	 * @param api
	 * @param sessionId
	 * @param destroy
	 * @return
	 */
	@POST("{api}/{session_id}")
	suspend fun destroySession(
		@Path("api") api: String,
		@Path("session_id") sessionId: Long,
		@Body destroy: DestroySession
	)
}
