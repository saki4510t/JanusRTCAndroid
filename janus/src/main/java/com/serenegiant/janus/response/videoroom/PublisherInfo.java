package com.serenegiant.janus.response.videoroom;
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

import androidx.annotation.NonNull;

public class PublisherInfo {
	/** パブリッシャーのID, XXX これはStringの方がいいのかも */
	public final Long id;
	public final String display;
	public final String audio_codec;
	public final String video_codec;
	public boolean talking;
	
	public PublisherInfo(final Long id,
		final String display,
		final String audio_codec, final String video_codec,
		final boolean talking) {

		this.id = id;
		this.display = display;
		this.audio_codec = audio_codec;
		this.video_codec = video_codec;
		this.talking = talking;
	}
	
	/**
	 * 引数がPublisherの場合にidの比較のみを行う
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(final Object o) {

		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final PublisherInfo publisher = (PublisherInfo) o;
		
		return (id != null) && id.equals(publisher.id);
	}
	
	/**
	 * idのhashCodeを返す
	 * @return
	 */
	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : super.hashCode();
	}

	@NonNull
	@Override
	public String toString() {
		return "PublisherInfo{" +
			"id=" + id +
			", display='" + display + '\'' +
			", audio_codec='" + audio_codec + '\'' +
			", video_codec='" + video_codec + '\'' +
			", talking=" + talking +
			'}';
	}
}
