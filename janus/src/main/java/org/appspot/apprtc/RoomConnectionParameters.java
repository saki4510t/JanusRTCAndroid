package org.appspot.apprtc;
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

import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Struct holding the connection parameters of an AppRTC room.
 */
public class RoomConnectionParameters {
	/**
	 * RoomConnectionParameters生成のためのビルダークラス
	 */
	public static class Builder {
		@Nullable
		private String roomUrl;
		@Nullable
		private String apiName = "janus";
		/** ルームID */
		private long roomId = 1234;
		private boolean loopback = false;
		@Nullable
		private String urlParameters = null;
		@Nullable
		private String userName = Build.MODEL;
		@Nullable
		private String displayName = Build.MODEL;

		/**
		 * デフォルトコンストラクタ
		 */
		public Builder() {
		}

		/**
		 * 既存のBuilderの内容を引き継いで新しいBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		public Builder(@NonNull final Builder src) {
			roomUrl = src.roomUrl;
			apiName = src.apiName;
			roomId = src.roomId;
			loopback = src.loopback;
			urlParameters = src.urlParameters;
			userName = src.userName;
			displayName = src.displayName;
		}

		/**
		 * janus-gatewayのルームURLを設定
		 * @param roomUrl
		 * @return
		 */
		public Builder setRoomUrl(@NonNull final String roomUrl) {
			this.roomUrl = roomUrl;
			return this;
		}

		/**
		 * janus-gatewayのAPI名を設定
		 * @param apiName
		 * @return
		 */
		public Builder setApiName(@NonNull final String apiName) {
			this.apiName = apiName;
			return this;
		}

		/**
		 * janus-gatewayのルームIDを設定
		 * @param roomId
		 * @return
		 */
		public Builder setRoomId(final long roomId) {
			this.roomId = roomId;
			return this;
		}

		/**
		 * ループバック接続するかどうかを設定
		 * @param loopback
		 * @return
		 */
		public Builder setLoopback(final boolean loopback) {
			this.loopback = loopback;
			return this;
		}

		/**
		 * URLパラメータを設定
		 * @param urlParameters
		 * @return
		 */
		public Builder setUrlParameters(@Nullable final String urlParameters) {
			this.urlParameters = urlParameters;
			return this;
		}

		/**
		 * ユーザー名を設定
		 * 設定していない/null/空文字列ならばBuild.MODELになる
		 * @param userName
		 * @return
		 */
		public Builder setUserName(@Nullable final String userName) {
			this.userName = TextUtils.isEmpty(userName) ? Build.MODEL : userName;
			return this;
		}

		/**
		 * ユーザーの表示名を設定
		 * 設定していない/null/空文字列ならばBuild.MODELになる
		 * @param displayName
		 * @return
		 */
		public Builder setDisplayName(@Nullable final String displayName) {
			this.displayName = TextUtils.isEmpty(displayName) ? Build.MODEL : displayName;
			return this;
		}

		/**
		 * RoomConnectionParametersを生成する
		 * @return
		 * @throws IllegalArgumentException
		 */
		@NonNull
		public RoomConnectionParameters build() throws IllegalArgumentException {
			if (TextUtils.isEmpty(roomUrl)
				|| TextUtils.isEmpty(apiName)
				|| (roomId == 0)) {
				throw new IllegalArgumentException("wrong build parameters," + this);
			}
			return new RoomConnectionParameters(
				roomUrl, apiName,
				roomId, loopback, urlParameters,
				userName, displayName);
		}

		@NonNull
		@Override
		public String toString() {
			return "Builder{" +
				"roomUrl='" + roomUrl + '\'' +
				", apiName='" + apiName + '\'' +
				", roomId=" + roomId +
				", loopback=" + loopback +
				", urlParameters='" + urlParameters + '\'' +
				", userName='" + userName + '\'' +
				", displayName='" + displayName + '\'' +
				'}';
		}
	}

	@NonNull
	public final String roomUrl;
	@NonNull
	public final String apiName;
	/** ルームID */
	public final long roomId;
	public final boolean loopback;
	@Nullable
	public final String urlParameters;
	@Nullable
	public final String userName;
	@Nullable
	public final String displayName;

	/**
	 * コンストラクタ
	 * FIXME Builderを使うようにするためprivateに変更する
	 * @param roomUrl
	 * @param apiName
	 * @param roomId
	 * @param loopback
	 * @param urlParameters
	 * @param userName nullまたは空文字列ならBuild.MODELを使う
	 * @param displayName  nullまたは空文字列ならBuild.MODELを使う
	 */
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final long roomId, final boolean loopback, @Nullable final String urlParameters,
		@Nullable final String userName, @Nullable final String displayName) {
		this.roomUrl = roomUrl;
		this.apiName = apiName;
		this.roomId = roomId;
		this.loopback = loopback;
		this.urlParameters = urlParameters;
		this.userName = userName;
		this.displayName = displayName;
	}

	/**
	 * コンストラクタ
	 * urlParameters, userName, displayNameはnullなので
	 * userNameとdisplayNameはBuild.MODELを使う
	 * FIXME Builderを使うようにするため削除する
	 * @param roomUrl
	 * @param apiName
	 * @param roomId
	 * @param loopback
	 */
	@Deprecated
	public RoomConnectionParameters(
		@Nullable final String roomUrl, @NonNull final String apiName,
		final long roomId, boolean loopback) {
		this(roomUrl, apiName, roomId,
			loopback, null,
			null, null);
	}
}
