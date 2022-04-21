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
import com.serenegiant.janus.request.Hangup;
import com.serenegiant.janus.request.Message;
import com.serenegiant.janus.request.Trickle;
import com.serenegiant.janus.request.TrickleCompleted;
import com.serenegiant.janus.response.Event;
import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.videoroom.RoomEvent;
import com.serenegiant.janus.response.ServerInfo;
import com.serenegiant.janus.response.Session;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;

/**
 * API interface of videoroom plugin on janus-gateway over http://https
 */
public interface VideoRoomWSAPI {
	/**
	 * janus-gatewayサーバーの情報を取得
	 * @return
	 */
	@Send
	public Flowable<ServerInfo> getInfo();

	/**
	 * セッションを作成
	 * セッションエンドポイント
	 * @param create
	 * @return
	 */
	@Send
	public Flowable<Session> createSession(@NonNull final CreateSession create);

	/**
	 * 指定したプラグインへ接続
	 * セッションエンドポイント
	 * @param attach
	 * @return
	 */
	@Send
	public Flowable<PluginInfo> attachPlugin(@NonNull final Attach attach);

	/**
	 * 指定したプラグインから切断
	 * これ自体はプラグインエンドポイントだけど#attachPluginの対なのでセッションエンドポイントとしてここに入れておく
	 * @param detach
	 * @return
	 */
	@Send
	public Flowable<Void> detachPlugin(@NonNull final Detach detach);

	/**
	 * セッションを破棄
	 * セッションエンドポイント
	 * @param destroy
	 * @return
	 */
	@Send
	public Flowable<Void> destroySession(@NonNull final DestroySession destroy);

//--------------------------------------------------------------------------------
// ここからしたがvideoroomプラグイン固有のエンドポイント定義
//--------------------------------------------------------------------------------
	@Send
	public Flowable<RoomEvent> join(@NonNull final Message message);

	@Send
	public Flowable<RoomEvent> offer(@NonNull final Message message);

	@Send
	public Flowable<RoomEvent> trickle(@NonNull final Trickle trickle);
	
	@Send
	public Flowable<RoomEvent> trickleCompleted(@NonNull final TrickleCompleted trickle);

	@Send
	public Flowable<Object> send(@NonNull final Message message);

	@Send
	public Flowable<Void> hangup(@NonNull final Hangup hangup);

	@Receive
	public Flowable<Event> observeEvent();

	@Receive
	public Flowable<RoomEvent> observeRoomEvent();

}
