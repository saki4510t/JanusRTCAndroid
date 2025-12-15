package com.serenegiant.janus.request
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

import com.serenegiant.janus.Room
import com.serenegiant.janus.TransactionCallback
import com.serenegiant.janus.TransactionManager

internal class TrickleCompleted(
	val room: Room,
	val callback: TransactionCallback?
) {
	val janus = "trickle"
	val transaction = TransactionManager.get(12, callback)
	val session_id = room.sessionId()
	val handle_id = room.pluginId()
	val candidate: Candidate

	init {
		this.candidate = Candidate()
	}

	class Candidate {
		val completed: Boolean = true
	}
}
