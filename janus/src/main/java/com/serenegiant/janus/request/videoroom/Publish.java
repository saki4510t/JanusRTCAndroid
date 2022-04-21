package com.serenegiant.janus.request.videoroom;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * VideoRoomプラグイン用メッセージボディー
 * publishリクエスト用
 */
public class Publish {
	public final String request;
	public final String audiocodec;	// "<audio codec to prefer among the negotiated ones; optional>",
	public final String videocodec;	// "<video codec to prefer among the negotiated ones; optional>",
	public final int bitrate;		// <bitrate cap to return via REMB; optional, overrides the global room value if present>,
	public final boolean record;	// <true|false, whether this publisher should be recorded or not; optional>,
	public final String filename;	// "<if recording, the base path/file to use for the recording files; optional>",
	public final String display;	// "<new display name to use in the room; optional>",
	public final int audio_level_average;	// "<if provided, overrided the room audio_level_average for this user; optional>",
	public final int audio_active_packets;	// "<if provided, overrided the room audio_active_packets for this user; optional>",
	@Nullable
	public final Description[] descriptions;	// Other descriptions, if any

	public Publish(final String audiocodec, final String videocodec, final int bitrate, final boolean record, final String filename, final String display, final int audio_level_average, final int audio_active_packets, @Nullable final Description[] descriptions) {
		this.request = "publish";
		this.audiocodec = audiocodec;
		this.videocodec = videocodec;
		this.bitrate = bitrate;
		this.record = record;
		this.filename = filename;
		this.display = display;
		this.audio_level_average = audio_level_average;
		this.audio_active_packets = audio_active_packets;
		this.descriptions = descriptions;
	}

	public static class Description {
		public final String mid;		// "<unique mid of a stream being published>"
		public final String description; // "<text description of the stream (e.g., My front webcam)>"

		public Description(final String mid, final String description) {
			this.mid = mid;
			this.description = description;
		}

		@NonNull
		@Override
		public String toString() {
			return "Description{" +
				"mid=" + mid +
				", description='" + description + '\'' +
				'}';
		}
	}

	@NonNull
	@Override
	public String toString() {
		return "Publish{" +
			"request='" + request + '\'' +
			", audiocodec='" + audiocodec + '\'' +
			", videocodec='" + videocodec + '\'' +
			", bitrate=" + bitrate +
			", record=" + record +
			", filename='" + filename + '\'' +
			", display='" + display + '\'' +
			", audio_level_average=" + audio_level_average +
			", audio_active_packets=" + audio_active_packets +
			", descriptions=" + Arrays.toString(descriptions) +
			'}';
	}
}
