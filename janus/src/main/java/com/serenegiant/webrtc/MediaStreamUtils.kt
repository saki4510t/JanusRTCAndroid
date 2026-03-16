package com.serenegiant.webrtc
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
import org.webrtc.MediaStreamTrack

/**
 * MediaStream操作用のヘルパー関数
 */
object MediaStreamUtils {
	/**
	 * 指定したMediaStreamの音声トラックをミュート/アンミュートする
	 * @param stream
	 * @param mute
	 */
	fun setMute(stream: MediaStream?, mute: Boolean) {
		if (stream != null) {
			val tracks = stream.audioTracks
			for (track in tracks) {
				track.setEnabled(!mute)
			}
		}
	}

	/**
	 * 指定したMediaStreamの音声トラックの音量をセットする
	 * ミュート状態は変更しない
	 * @param stream
	 * @param volume
	 */
	fun setVolume(stream: MediaStream?, volume: Double) {
		if (stream != null) {
			val tracks = stream.audioTracks
			for (track in tracks) {
				track.setVolume(volume)
			}
		}
	}

	/**
	 * 指定したMediaStreamの映像トラックの有効無効を切り替える
	 * @param stream
	 * @param enable
	 */
	fun setVideoEnabled(stream: MediaStream?, enable: Boolean) {
		if (stream != null) {
			val tracks = stream.videoTracks
			for (track in tracks) {
				track.setEnabled(enable)
			}
		}
	}

	/**
	 * 指定したMediaStreamのすべてのトラックの有効無効を切り替える
	 * @param stream
	 * @param enable
	 */
	fun setEnabled(stream: MediaStream?, enable: Boolean) {
		setMute(stream, !enable)
		setVideoEnabled(stream, enable)
	}

	/**
	 * 指定したMediaStreamが有効な音声トラックを保持しているかどうかを取得
	 * (muteされているかどうかは関係しない)
	 * @param stream
	 * @return
	 */
	fun hasValidAudioTrack(stream: MediaStream?): Boolean {
		if (stream != null) {
			val tracks = stream.audioTracks
			for (track in tracks) {
				if (track.state() == MediaStreamTrack.State.LIVE) {
					return true
				}
			}
		}
		return false
	}

	/**
	 * 指定したMediaStreamが有効な映像トラックを保持しているかどうかを取得
	 * (enableになっているかどうかは関係しない)
	 * @param stream
	 * @return
	 */
	fun hasValidVideoTrack(stream: MediaStream?): Boolean {
		if (stream != null) {
			val tracks = stream.videoTracks
			for (track in tracks) {
				if (track.state() == MediaStreamTrack.State.LIVE) {
					return true
				}
			}
		}
		return false
	}

	/**
	 * 指定したMediaStreamの最初に見つかった有効な音声トラックのidを取得する
	 * (muteされているかどうかは関係しない)
	 * @param stream
	 * @return 見つからなければnullを返す
	 */
	fun getAudioId(stream: MediaStream?): String? {
		if (stream != null) {
			val tracks = stream.audioTracks
			for (track in tracks) {
				if (track.state() == MediaStreamTrack.State.LIVE) {
					return track.id()
				}
			}
		}
		return null
	}

	/**
	 * 指定したMediaStreamの最初に見つかった有効な映像トラックのidを取得する
	 * (muteされているかどうかは関係しない)
	 * @param stream
	 * @return 見つからなければnullを返す
	 */
	fun getVideoId(stream: MediaStream?): String? {
		if (stream != null) {
			val tracks = stream.videoTracks
			for (track in tracks) {
				if (track.state() == MediaStreamTrack.State.LIVE) {
					return track.id()
				}
			}
		}
		return null
	}
}
