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

import com.serenegiant.janus.request.Attach;
import com.serenegiant.janus.request.CreateSession;
import com.serenegiant.janus.request.DestroySession;
import com.serenegiant.janus.request.Detach;
import com.serenegiant.janus.request.Hangup;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.request.videoroom.ConfigPublisher;
import com.serenegiant.janus.request.videoroom.ConfigSubscriber;
import com.serenegiant.janus.response.videoroom.Configured;
import com.serenegiant.janus.request.videoroom.List;
import com.serenegiant.janus.response.videoroom.ListResponse;
import com.serenegiant.janus.response.videoroom.RoomEvent;
import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;
import com.serenegiant.janus.response.videoroom.RoomInfo;

import java.math.BigInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * API interface of videoroom plugin on janus-gateway over http://https
 * retrofit2でエンドポイント定義のインターフェースを継承していいのかどうかわからないので
 * Janusインターフェースを継承せずに直接ここで定義
 */
public interface VideoRoomAPI /*extends JanusAPI*/ {
	/**
	 * janus-gatewayサーバーの情報を取得
	 * @param api
	 * @return
	 */
	@GET("{api}/info")
	public Call<ServerInfo> getInfo(@Path("api") final String api);

	/**
	 * セッションを作成
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
	 * @param api
	 * @param sessionId
	 * @param attach
	 * @return
	 */
	@POST("{api}/{session_id}")
	public Call<PluginInfo> attachPlugin(
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

//--------------------------------------------------------------------------------
// ここから下がvideoroomプラグインのエンドポイント定義
//--------------------------------------------------------------------------------
	@GET("{api}/{session_id}/{plugin_id}")
	public Call<ListResponse<RoomInfo>> getRoomList(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final List list);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> join(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> offer(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> trickle(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Trickle trickle);
	
	@POST("{api}/{session_id}/{plugin_id}")
	public Call<RoomEvent> trickleCompleted(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final TrickleCompleted trickle);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<ResponseBody> send(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Message message);
	
	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Void> hangup(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final Hangup hangup);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Configured> configure(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final ConfigPublisher config);

	@POST("{api}/{session_id}/{plugin_id}")
	public Call<Configured> configure(
		@Path("api") final String api,
		@Path("session_id") final BigInteger sessionId,
		@Path("plugin_id") final BigInteger pluginId,
		@Body final ConfigSubscriber config);

}
