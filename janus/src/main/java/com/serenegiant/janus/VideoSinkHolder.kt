package com.serenegiant.janus
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

import org.webrtc.MediaStream
import org.webrtc.VideoSink
import org.webrtc.VideoTrack

/**
 * １つのパブリッシャーの映像描画関係のオブジェクトを保持するためのホルダークラス
 * @param feedId フィードID(対応するサブスクライバーのパブリッシャーID)
 * @param videoTrack
 * @param videoSinks
 */
class VideoSinkHolder(
	val feedId: Long,
	private val videoTrack: VideoTrack,
	val videoSinks: List<VideoSink>
) {
	/**
	 * フィードID(対応するサブスクライバーのパブリッシャーID)を取得
	 * @return
	 */
	var mMediaStream: MediaStream? = null

	init {
		for (videoSink in videoSinks) {
			videoTrack.addSink(videoSink)
		}
	}

	/**
	 * 映像を描画するかどうかを設定
	 * @param enableRender
	 */
	fun setEnabled(enableRender: Boolean) {
		videoTrack.setEnabled(enableRender)
	}

	fun setMediaStream(stream: MediaStream?) {
		mMediaStream = stream
	}
}
