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
import com.serenegiant.janus.request.CreateSession;
import com.serenegiant.janus.request.DestroySession;
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
	/**
	 * janus-gatewayサーバーの情報を取得
	 * @param api
	 * @return
	 */
	@GET("{api}/info")
	public Call<ServerInfo> getInfo(@Path("api") final String api);

	/**
	 * セッションを作成
	 * セッションエンドポイント
	 * @param api
	 * @param create
	 * @return
	 */
	@POST("{api}")
	public Call<Session> createSession(
		@Path("api") final String api,
		@Body final CreateSession create);

	/**
	 * 指定したプラグインへ接続
	 * セッションエンドポイント
	 * @param api
	 * @param sessionId
	 * @param attach
	 * @return
	 */
	@POST("{api}/{session_id}")
	public Call<Plugin> attachPlugin(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Body final Attach attach);

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
	public Call<Void> detachPlugin(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Detach detach);

	/**
	 * セッションを破棄
	 * セッションエンドポイント
	 * @param api
	 * @param sessionId
	 * @param destroy
	 * @return
	 */
	@POST("{api}/{session_id}")
	public Call<Void> destroySession(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Body final DestroySession destroy);
}
