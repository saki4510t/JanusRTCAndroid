package com.serenegiant.webrtc;

import androidx.annotation.Nullable;

import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.VideoTrack;

import java.util.List;

/**
 * MediaStream操作用のヘルパー関数
 */
public class MediaStreamUtils {
    private MediaStreamUtils() {
        // インスタンス化をエラーとするためにデフォルトコンストラクタをprivateに
    }

    /**
     * 指定したMediaStreamの音声トラックをミュート/アンミュートする
     * @param stream
     * @param mute
     */
    public static void setMute(@Nullable final MediaStream stream, final boolean mute) {
        if (stream != null) {
            final List<AudioTrack> tracks = stream.audioTracks;
            for (final AudioTrack track: tracks) {
                track.setEnabled(!mute);
            }
        }
    }

    /**
     * 指定したMediaStreamの音声トラックの音量をセットする
     * ミュート状態は変更しない
     * @param stream
     * @param volume
     */
    public static void setVolume(@Nullable final MediaStream stream, final double volume) {
        if (stream != null) {
            final List<AudioTrack> tracks = stream.audioTracks;
            for (final AudioTrack track: tracks) {
                track.setVolume(volume);
            }
        }
    }

    /**
     * 指定したMediaStreamの映像トラックの有効無効を切り替える
     * @param stream
     * @param enable
     */
    public static void setVideoEnabled(@Nullable final MediaStream stream, final boolean enable) {
        if (stream != null) {
            final List<VideoTrack> tracks = stream.videoTracks;
            for (final VideoTrack track: tracks) {
                track.setEnabled(enable);
            }
        }
    }

    /**
     * 指定したMediaStreamのすべてのトラックの有効無効を切り替える
     * @param stream
     * @param enable
     */
    public static void setEnabled(@Nullable final MediaStream stream, final boolean enable) {
        setMute(stream, !enable);
        setVideoEnabled(stream, enable);
    }

    /**
     * 指定したMediaStreamが有効な音声トラックを保持しているかどうかを取得
     * (muteされているかどうかは関係しない)
     * @param stream
     * @return
     */
    public static boolean hasValidAudioTrack(@Nullable MediaStream stream) {
        if (stream != null) {
            final List<AudioTrack> tracks = stream.audioTracks;
            for (final AudioTrack track: tracks) {
                if (track.state() == MediaStreamTrack.State.LIVE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 指定したMediaStreamが有効な映像トラックを保持しているかどうかを取得
     * (enableになっているかどうかは関係しない)
     * @param stream
     * @return
     */
    public static boolean hasValidVideoTrack(@Nullable MediaStream stream) {
        if (stream != null) {
            final List<VideoTrack> tracks = stream.videoTracks;
            for (final VideoTrack track: tracks) {
                if (track.state() == MediaStreamTrack.State.LIVE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 指定したMediaStreamの最初に見つかった有効な音声トラックのidを取得する
     * (muteされているかどうかは関係しない)
     * @param stream
     * @return 見つからなければnullを返す
     */
    @Nullable
    public static String getAudioId(@Nullable MediaStream stream) {
        if (stream != null) {
            final List<AudioTrack> tracks = stream.audioTracks;
            for (final AudioTrack track: tracks) {
                if (track.state() == MediaStreamTrack.State.LIVE) {
                    return track.id();
                }
            }
        }
        return null;
    }

    /**
     * 指定したMediaStreamの最初に見つかった有効な映像トラックのidを取得する
     * (muteされているかどうかは関係しない)
     * @param stream
     * @return 見つからなければnullを返す
     */
    @Nullable
    public static String getVideoId(@Nullable MediaStream stream) {
        if (stream != null) {
            final List<VideoTrack> tracks = stream.videoTracks;
            for (final VideoTrack track: tracks) {
                if (track.state() == MediaStreamTrack.State.LIVE) {
                    return track.id();
                }
            }
        }
        return null;
    }
}
