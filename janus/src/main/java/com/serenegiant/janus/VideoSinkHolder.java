package com.serenegiant.janus;

import org.webrtc.MediaStream;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * １つのパブリッシャーの映像描画関係のオブジェクトを保持するためのホルダークラス
 */
public class VideoSinkHolder {
	/**
	 * フィードID(対応するサブスクライバーのパブリッシャーID)
	 */
	private final long mFeedId;
	@NonNull
	private final VideoTrack mVideoTrack;
	@NonNull
	public final List<VideoSink> mVideoSinks;
	@Nullable
	public MediaStream mMediaStream;

	public VideoSinkHolder(final long feedId,
		@NonNull final VideoTrack videoTrack,
		@NonNull final List<VideoSink> videoSinks) {

		mFeedId = feedId;
		mVideoTrack = videoTrack;
		mVideoSinks = videoSinks;
		mMediaStream = null;
		for (final VideoSink videoSink : videoSinks) {
			videoTrack.addSink(videoSink);
		}
	}

	/**
	 * フィードID(対応するサブスクライバーのパブリッシャーID)を取得
	 * @return
	 */
	public long getFeedId() {
		return mFeedId;
	}

	/**
	 * 映像を描画するかどうかを設定
	 * @param enableRender
	 */
	public void setEnabled(final boolean enableRender) {
		mVideoTrack.setEnabled(enableRender);
	}

	public void setMediaStream(@Nullable final MediaStream stream) {
		mMediaStream = stream;
	}
}
