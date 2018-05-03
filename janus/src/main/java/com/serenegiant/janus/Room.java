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
	 * @param newPublishers
	 * @return 追加または削除されたPublisherのリスト
	 */
	@NonNull
	public List<PublisherInfo> updatePublisher(
		@Nullable final PublisherInfo[] newPublishers) {

		final List<PublisherInfo> result;
		if (newPublishers != null) {
			final List<PublisherInfo> src = Arrays.asList(newPublishers);
			result = new ArrayList<>(src);
			
			synchronized (this.publishers) {
				// 既にRoomに登録されているPublisherを除く=未登録分
				result.removeAll(this.publishers);
				// 既にRoomに登録されているものから新しいPublisherを除く=削除分
				this.publishers.removeAll(src);
				result.addAll(this.publishers);
				this.publishers.clear();
				this.publishers.addAll(src);
			}
		} else {
			synchronized (this.publishers) {
				result = new ArrayList<>(this.publishers);
				this.publishers.clear();
			}
		}
		return result;
	}
}
