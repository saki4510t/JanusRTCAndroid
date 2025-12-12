package com.serenegiant.janus
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2025 saki t_saki@serenegiant.com
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

internal abstract class JanusPlugin protected constructor(val session: Session) {
	interface PluginCallback {
		/**
		 * callback when attached to plugin
		 * @param plugin
		 */
		fun onAttach(plugin: JanusPlugin)

		/**
		 * callback when detached from plugin
		 * @param plugin
		 */
		fun onDetach(plugin: JanusPlugin)
	}

	var info: PluginInfo? = null

	/**
	 * セッションIDを取得
	 * @return
	 */
	fun sessionId(): Long {
		return session.id()
	}

	/**
	 * プラグインIDを取得
	 * @return
	 */
	fun pluginId(): Long {
		return if (info != null) info!!.id() else 0L
	}

	/**
	 * プラグインと接続中かどうか
	 * @return
	 */
	fun attached(): Boolean {
		return (info != null)
	}

	/**
	 * プラグインへ接続
	 */
	abstract fun attach()

	/**
	 * プラグインから切断
	 */
	abstract fun detach()

	companion object {
		private const val DEBUG = false // set false on production
		private val TAG: String = JanusPlugin::class.java.simpleName
	}
}
