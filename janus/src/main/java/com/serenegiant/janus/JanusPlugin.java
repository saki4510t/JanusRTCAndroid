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
	protected final Session mSession;
	protected PluginInfo mPlugin;

	protected JanusPlugin(@NonNull final Session session) {
		mSession = session;
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
	long id() {
		return mPlugin != null ? mPlugin.id() : 0;
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
