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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.serenegiant.janus.response.Plugin;
import com.serenegiant.janus.response.PublisherInfo;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Room {
	/**
	 * セッションId
	 */
	@NonNull
	public final BigInteger sessionId;

	/**
	 * プラグインId
	 */
	@NonNull
	public final BigInteger pluginId;
	
	/**
	 * 接続状態
 	 */
	public String state;
	
	/**
	 * クライアントID
	 * EventRoom.plugindata.data.idの値
	 * publisherとしての自id
	 */
	public BigInteger publisherId;
	
	/**
	 * holds list of connected remote publisher
	 */
	private final List<PublisherInfo> publishers
		= new ArrayList<>();
	
	/**
	 * Constructor
	 * @param session
	 * @param plugin
	 */
	public Room(@NonNull final Session session, @NonNull final Plugin plugin) {
		this.sessionId = session.id();
		this.pluginId = plugin.id();
	}
	
	/**
	 * 現在保持しているPublisherInfoリストのコピーを返す
	 * @return
	 */
	public List<PublisherInfo> getPublishers() {
		synchronized (this.publishers) {
			return new ArrayList<>(this.publishers);
		}
	}

	/**
	 * janus-gateway serverに接続されているリモートPublisherの一覧を更新
	 * @param newPublishers 自分が未接続のpublisherだけが入っているみたい
	 * @return 追加されたPublisherのリスト
	 */
	@NonNull
	public List<PublisherInfo> updatePublishers(
		@Nullable final PublisherInfo[] newPublishers) {

		final List<PublisherInfo> result = new ArrayList<>();
		if (newPublishers != null) {
			final List<PublisherInfo> newList = Arrays.asList(newPublishers);
			result.addAll(newList);
			
			synchronized (this.publishers) {
				// 既にRoomに登録されているPublisherを除く=未登録分
				result.removeAll(this.publishers);
			}
		}
		return result;
	}
	
	/**
	 * 指定したidのPublisherを一覧から取り除く
	 * @param id
	 * @return
	 */
	@NonNull
	public List<PublisherInfo> removePublisher(
		@NonNull final BigInteger id) {
		
		synchronized (this.publishers) {
			PublisherInfo found = null;
			for (PublisherInfo info: publishers) {
				if (id.equals(info.id)) {
					found = info;
					break;
				}
			}
			if (found != null) {
				publishers.remove(found);
			}
			return new ArrayList<>(this.publishers);
		}
	}
	
	/**
	 * 指定したidのPublisherが存在すればそのtalkingフラグを更新する
	 * @param id
	 * @param talking
	 */
	public void updatePublisher(@NonNull final BigInteger id, final boolean talking) {
		synchronized (this.publishers) {
			PublisherInfo found = null;
			for (PublisherInfo info: publishers) {
				if (id.equals(info.id)) {
					found = info;
					break;
				}
			}
			if (found != null) {
				found.talking = talking;
			}
		}
	}
	
	/**
	 * このRoomインスタンスが保持しているPublisherの数を返す
	 * @return
	 */
	public int getNumPublishers() {
		synchronized (this.publishers) {
			return this.publishers.size();
		}
	}
}
