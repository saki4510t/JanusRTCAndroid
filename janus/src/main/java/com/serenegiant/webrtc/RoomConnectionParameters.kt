package com.serenegiant.webrtc
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.os.Build
import android.text.TextUtils

/**
 * Struct holding the connection parameters of an AppRTC room.
 * @param roomUrl
 * @param apiName
 * @param roomId
 * @param loopback
 * @param urlParameters
 * @param userName nullまたは空文字列ならBuild.MODELを使う
 * @param displayName  nullまたは空文字列ならBuild.MODELを使う
 */
class RoomConnectionParameters(
	@JvmField val roomUrl: String,
	@JvmField val apiName: String,
	@JvmField val roomId: Long,
	@JvmField val loopback: Boolean = false,
	@JvmField val urlParameters: String? = null,
	@JvmField val userName: String? = null,
	@JvmField val displayName: String? = null,
) {
	/**
	 * RoomConnectionParameters生成のためのビルダークラス
	 */
	class Builder {
		private var roomUrl: String? = null
		private var apiName: String? = "janus"

		/** ルームID  */
		private var roomId: Long = 1234
		private var loopback = false
		private var urlParameters: String? = null
		private var userName: String? = Build.MODEL
		private var displayName: String? = Build.MODEL

		/**
		 * デフォルトコンストラクタ
		 */
		constructor()

		/**
		 * 既存のBuilderの内容を引き継いで新しいBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		constructor(src: Builder) {
			roomUrl = src.roomUrl
			apiName = src.apiName
			roomId = src.roomId
			loopback = src.loopback
			urlParameters = src.urlParameters
			userName = src.userName
			displayName = src.displayName
		}

		/**
		 * janus-gatewayのルームURLを設定
		 * @param roomUrl
		 * @return
		 */
		fun setRoomUrl(roomUrl: String): Builder {
			this.roomUrl = roomUrl
			return this
		}

		/**
		 * janus-gatewayのAPI名を設定
		 * @param apiName
		 * @return
		 */
		fun setApiName(apiName: String): Builder {
			this.apiName = apiName
			return this
		}

		/**
		 * janus-gatewayのルームIDを設定
		 * @param roomId
		 * @return
		 */
		fun setRoomId(roomId: Long): Builder {
			this.roomId = roomId
			return this
		}

		/**
		 * ループバック接続するかどうかを設定
		 * @param loopback
		 * @return
		 */
		fun setLoopback(loopback: Boolean): Builder {
			this.loopback = loopback
			return this
		}

		/**
		 * URLパラメータを設定
		 * @param urlParameters
		 * @return
		 */
		fun setUrlParameters(urlParameters: String?): Builder {
			this.urlParameters = urlParameters
			return this
		}

		/**
		 * ユーザー名を設定
		 * 設定していない/null/空文字列ならばBuild.MODELになる
		 * @param userName
		 * @return
		 */
		fun setUserName(userName: String?): Builder {
			this.userName = if (TextUtils.isEmpty(userName)) Build.MODEL else userName
			return this
		}

		/**
		 * ユーザーの表示名を設定
		 * 設定していない/null/空文字列ならばBuild.MODELになる
		 * @param displayName
		 * @return
		 */
		fun setDisplayName(displayName: String?): Builder {
			this.displayName = if (TextUtils.isEmpty(displayName)) Build.MODEL else displayName
			return this
		}

		/**
		 * RoomConnectionParametersを生成する
		 * @return
		 * @throws IllegalArgumentException
		 */
		@Throws(IllegalArgumentException::class)
		fun build(): RoomConnectionParameters {
			require(
				!(TextUtils.isEmpty(roomUrl)
					|| TextUtils.isEmpty(apiName)
					|| (roomId == 0L))
			) { "wrong build parameters,$this" }
			return RoomConnectionParameters(
				roomUrl!!, apiName!!,
				roomId, loopback, urlParameters,
				userName, displayName
			)
		}

		override fun toString(): String {
			return "Builder{" +
				"roomUrl='" + roomUrl + '\'' +
				", apiName='" + apiName + '\'' +
				", roomId=" + roomId +
				", loopback=" + loopback +
				", urlParameters='" + urlParameters + '\'' +
				", userName='" + userName + '\'' +
				", displayName='" + displayName + '\'' +
				'}'
		}
	}
}
