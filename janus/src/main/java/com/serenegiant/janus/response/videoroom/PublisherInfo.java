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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class PublisherInfo implements Parcelable {
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

	protected PublisherInfo(@NonNull final Parcel src) {
		if (src.readByte() == 0) {
			id = null;
		} else {
			id = src.readLong();
		}
		display = src.readString();
		audio_codec = src.readString();
		video_codec = src.readString();
		talking = src.readByte() != 0;
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(@NonNull final Parcel dst, final int flags) {
		if (id == null) {
			dst.writeByte((byte) 0);
		} else {
			dst.writeByte((byte) 1);
			dst.writeLong(id);
		}
		dst.writeString(display);
		dst.writeString(audio_codec);
		dst.writeString(video_codec);
		dst.writeByte((byte) (talking ? 1 : 0));
	}

	public static final Creator<PublisherInfo> CREATOR = new Creator<PublisherInfo>() {
		@Override
		public PublisherInfo createFromParcel(@NonNull final Parcel src) {
			return new PublisherInfo(src);
		}

		@Override
		public PublisherInfo[] newArray(final int size) {
			return new PublisherInfo[size];
		}
	};
}
