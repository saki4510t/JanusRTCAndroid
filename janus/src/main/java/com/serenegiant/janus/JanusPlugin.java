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

import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.Session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/*package*/ abstract class JanusPlugin {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = JanusPlugin.class.getSimpleName();

	public interface PluginCallback {
		/**
		 * callback when attached to plugin
		 * @param plugin
		 */
		public void onAttach(@NonNull final JanusPlugin plugin);
		/**
		 * callback when detached from plugin
		 * @param plugin
		 */
		public void onDetach(@NonNull final JanusPlugin plugin);
	}

	@NonNull
	private final Session mSession;
	@Nullable
	private PluginInfo mInfo;

	protected JanusPlugin(@NonNull final Session session) {
		mSession = session;
	}

	@NonNull
	Session getSession() {
		return mSession;
	}

	@Nullable
	PluginInfo getInfo() {
		return mInfo;
	}

	void setInfo(@Nullable PluginInfo info) {
		mInfo = info;
	}

	/**
	 * セッションIDを取得
	 * @return
	 */
	long sessionId() {
		return mSession.id();
	}

	/**
	 * プラグインIDを取得
	 * @return
	 */
	long pluginId() {
		return mInfo != null ? mInfo.id() : 0L;
	}

	/**
	 * プラグインと接続中かどうか
	 * @return
	 */
	boolean attached() {
		return (mInfo != null);
	}

	/**
	 * プラグインへ接続
	 */
	public abstract void attach();

	/**
	 * プラグインから切断
	 */
	public abstract void detach();
}
