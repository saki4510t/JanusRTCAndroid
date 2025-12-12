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

import com.serenegiant.janus.response.PluginInfo
import com.serenegiant.janus.response.Session
import com.serenegiant.janus.response.videoroom.PublisherInfo
import java.util.Arrays
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * VideoRoomプラグイン用のヘルパークラス
 * @param session
 * @param info
 */
class Room(session: Session, info: PluginInfo)
	: Plugin(session, info) {

	/**
	 * クライアントID
	 * EventRoom.plugindata.data.idの値
	 * publisherとしての自id
	 */
	@JvmField
	var publisherId: Long? = null

	private val mLock = ReentrantLock()
	/**
	 * holds list of connected remote publisher
	 */
	private val publishers = mutableListOf<PublisherInfo>()

	/**
	 * 現在保持しているPublisherInfoリストのコピーを返す
	 * @return
	 */
	fun getPublishers(): List<PublisherInfo> {
		mLock.withLock {
			return ArrayList(this.publishers)
		}
	}

	/**
	 * janus-gateway serverに接続されているリモートPublisherの一覧を更新
	 * @param newPublishers 自分が未接続のpublisherだけが入っているみたい
	 * @return 追加されたPublisherのリスト
	 */
	fun updatePublishers(
		newPublishers: Array<PublisherInfo>?
	): List<PublisherInfo> {
		val result = mutableListOf<PublisherInfo>()
		if (newPublishers != null) {
			val newList = listOf(*newPublishers)
			result.addAll(newList)

			mLock.withLock {
				// 既にRoomに登録されているPublisherを除く=未登録分
				result.removeAll(this.publishers)
			}
		}
		return result
	}

	/**
	 * 指定したidのPublisherを一覧から取り除く
	 * @param id
	 * @return
	 */
	fun removePublisher(id: Long): List<PublisherInfo> {
		mLock.withLock {
			var found: PublisherInfo? = null
			for (info in publishers) {
				if (id == info.id) {
					found = info
					break
				}
			}
			if (found != null) {
				// 見つかったPublisherを取り除く
				publishers.remove(found)
			}
			return ArrayList(this.publishers)
		}
	}

	/**
	 * 指定したidのPublisherが存在すればそのtalkingフラグを更新する
	 * @param id
	 * @param talking
	 */
	fun updatePublisher(id: Long, talking: Boolean) {
		mLock.withLock {
			var found: PublisherInfo? = null
			for (info in publishers) {
				if (id == info.id) {
					found = info
					break
				}
			}
			if (found != null) {
				found.talking = talking
			}
		}
	}

	/**
	 * このRoomインスタンスが保持しているPublisherの数を返す
	 * @return
	 */
	fun getNumPublishers(): Int {
		mLock.withLock {
			return publishers.size
		}
	}
}
