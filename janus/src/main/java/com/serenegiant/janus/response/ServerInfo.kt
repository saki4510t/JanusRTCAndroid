package com.serenegiant.janus.response
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2026 saki t_saki@serenegiant.com
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

internal class ServerInfo(
	val janus: String,
	val transaction: String,
	val name: String,
	val version: Int,
	val version_string: String,
	val author: String,
	val commit_hash: String,
	val compile_time: String,
	val data_channels: Boolean,
	val session_timeout: Int,
	val ipv6: Boolean,
	val ice_tcp: Boolean,
	val transports: Transports,
	val plugins: PluginInfos?
) {
	class Transports(val transports: List<Transport>?) {
		override fun toString(): String {
			val sb = StringBuilder()
			sb.append("Transports{")
			if (!transports.isNullOrEmpty()) {
				for (transport in transports) {
					sb.append(transport).append(",")
				}
			}
			sb.append('}')
			return sb.toString()
		}
	}

	class PluginInfo(
		val name: String,
		val author: String,
		val description: String,
		val version_string: String,
		val version: Int
	) {
		override fun toString(): String {
			return "PluginInfo{" +
				"name='" + name + '\'' +
				", author='" + author + '\'' +
				", description='" + description + '\'' +
				", version_string='" + version_string + '\'' +
				", version=" + version +
				'}'
		}
	}

	class PluginInfos(val plugins: List<PluginInfo>?) {
		override fun toString(): String {
			val sb = StringBuilder()
			sb.append("PluginInfos{")
			if (!plugins.isNullOrEmpty()) {
				for (plugin in plugins) {
					sb.append(plugin).append(",")
				}
			}
			sb.append('}')
			return sb.toString()
		}
	}

	fun plugins(): List<PluginInfo>? {
		return plugins?.plugins
	}

	override fun toString(): String {
		return "ServerInfo{" +
			"janus='" + janus + '\'' +
			", transaction='" + transaction + '\'' +
			", name='" + name + '\'' +
			", version=" + version +
			", version_string='" + version_string + '\'' +
			", author='" + author + '\'' +
			", data_channels=" + data_channels +
			", ipv6=" + ipv6 +
			", ice_tcp=" + ice_tcp +
			", transports=" + transports +
			", plugins=" + plugins +
			'}'
	}
}
