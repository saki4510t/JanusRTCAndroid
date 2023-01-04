package org.appspot.apprtc;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

/**
 * AppRTCAudioManagerとAppRTCAudioManager共通の定数等を定義するインターフェース
 * FIXME インターフェースの代わりに抽象クラスにして共通処理を実装した方がいいかも
 */
public interface IAppRTCAudioManager
	extends AppRTCBluetoothManager.UpdateBluetoothStateListener {

	/**
	 * スピーカーフォン設定を自動切り替え
	 */
	static final String SPEAKERPHONE_AUTO = "auto";
	/**
	 * スピーカーフォンを有効に
	 */
	static final String SPEAKERPHONE_TRUE = "true";
	/**
	 * スピーカーフォンを無効に
	 */
	static final String SPEAKERPHONE_FALSE = "false";
	/**
	 * ユーザーが他の音声機器を明示的に選択しない限りスピーカーフォンを有効にする
	 */
	static final String SPEAKERPHONE_AS_POSSIBLE = "asPossible";

	/**
	 * AudioDevice is the names of possible audio devices that we currently
	 * support.
	 */
	public enum AudioDevice implements Parcelable {
		SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE;

		AudioDevice() {
		}

		AudioDevice(@NonNull final Parcel src) {
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(@NonNull final Parcel dst, final int flags) {
			dst.writeInt(ordinal());
		}

		public static final Creator<AudioDevice> CREATOR = new Creator<AudioDevice>() {
			@Override
			public AudioDevice createFromParcel(@NonNull final Parcel src) {
				return AudioDevice.values()[src.readInt()];
			}

			@Override
			public AudioDevice[] newArray(final int size) {
				return new AudioDevice[size];
			}
		};

	}

	/**
	 * AudioManager state.
	 */
	public enum AudioManagerState {
		UNINITIALIZED,
		PREINITIALIZED,
		RUNNING,
	}

	/**
	 * Selected audio device change event.
	 */
	public interface AudioManagerEvents {
		// Callback fired once audio device is changed or list of available audio devices changed.
		public void onAudioDeviceChanged(
			@NonNull final AudioDevice selected,
			@NonNull final Set<AudioDevice> availables);
	}

	public boolean isStarted();

	@UiThread
	public void start(@NonNull final AudioManagerEvents audioManagerEvents);

	@UiThread
	public void stop();

	@UiThread
	public void setDefaultAudioDevice(@NonNull final AudioDevice defaultDevice);
	@UiThread
	public void selectAudioDevice(@NonNull final AudioDevice device);
	@UiThread
	@NonNull
	public Set<AudioDevice> getAudioDevices();
	@UiThread
	@NonNull
	public AudioDevice getSelectedAudioDevice();

}
