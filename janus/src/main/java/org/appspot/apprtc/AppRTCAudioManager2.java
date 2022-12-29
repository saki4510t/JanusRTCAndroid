package org.appspot.apprtc;/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

import android.util.Log;

import com.serenegiant.janus.R;

import org.appspot.apprtc.util.AppRTCUtils;
import org.webrtc.ThreadUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AppRTCAudioManager manages all audio related parts of the AppRTC demo.
 */
public class AppRTCAudioManager2 implements IAppRTCAudioManager {
	private static final boolean DEBUG = true; // set false on production
	private static final String TAG = AppRTCAudioManager2.class.getSimpleName();

	private final Context apprtcContext;
	@Nullable
	private final AudioManager audioManager;

	@Nullable
	private AudioManagerEvents audioManagerEvents;
	private AudioManagerState amState;
	private int savedAudioMode = AudioManager.MODE_INVALID;
	private boolean savedIsSpeakerPhoneOn = false;
	private boolean savedIsMicrophoneMute = false;
	private boolean hasWiredHeadset = false;

	// Default audio device; speaker phone for video calls or earpiece for audio
	// only calls.
	private AudioDevice defaultAudioDevice;

	// Contains the currently selected audio device.
	// This device is changed automatically using a certain scheme where e.g.
	// a wired headset "wins" over speaker phone. It is also possible for a
	// user to explicitly select a device (and overrid any predefined scheme).
	// See |userSelectedAudioDevice| for details.
	private AudioDevice selectedAudioDevice;

	// Contains the user-selected audio device which overrides the predefined
	// selection scheme.
	// TODO(henrika): always set to AudioDevice.NONE today. Add support for
	// explicit selection based on choice by userSelectedAudioDevice.
	private AudioDevice userSelectedAudioDevice;

	// Contains speakerphone setting: auto, true or false
	private final String useSpeakerphone;

	// Proximity sensor object. It measures the proximity of an object in cm
	// relative to the view screen of a device and can therefore be used to
	// assist device switching (close to ear <=> use headset earpiece if
	// available, far from ear <=> use speaker phone).
	@Nullable
	private AppRTCProximitySensor proximitySensor;

	// Handles all tasks related to Bluetooth headset devices.
	private final AppRTCBluetoothManager bluetoothManager;

	// Contains a list of available audio devices. A Set collection is used to
	// avoid duplicate elements.
	@NonNull
	private final Set<AudioDevice> audioDevices = new HashSet<>();

	// Broadcast receiver for wired headset intent broadcasts.
	private final BroadcastReceiver wiredHeadsetReceiver;

	// Callback method for changes in audio focus.
	@Nullable
	private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

	/**
	 * This method is called when the proximity sensor reports a state change,
	 * e.g. from "NEAR to FAR" or from "FAR to NEAR".
	 */
	private void onProximitySensorChangedState() {
		if (!useSpeakerphone.equals(SPEAKERPHONE_AUTO)) {
			return;
		}

		// The proximity sensor should only be activated when there are exactly two
		// available audio devices.
		if (audioDevices.size() == 2 && audioDevices.contains(AudioDevice.EARPIECE)
			&& audioDevices.contains(AudioDevice.SPEAKER_PHONE)) {
			if (proximitySensor.sensorReportsNearState()) {
				// Sensor reports that a "handset is being held up to a person's ear",
				// or "something is covering the light sensor".
				setAudioDeviceInternal(AudioDevice.EARPIECE);
			} else {
				// Sensor reports that a "handset is removed from a person's ear", or
				// "the light sensor is no longer covered".
				setAudioDeviceInternal(AudioDevice.SPEAKER_PHONE);
			}
		}
	}

	/* Receiver which handles changes in wired headset availability. */
	private class WiredHeadsetReceiver extends BroadcastReceiver {
		private static final int STATE_UNPLUGGED = 0;
		private static final int STATE_PLUGGED = 1;
		private static final int HAS_NO_MIC = 0;
		private static final int HAS_MIC = 1;

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra("state", STATE_UNPLUGGED);
			int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
			String name = intent.getStringExtra("name");
			if (DEBUG) Log.d(TAG, "WiredHeadsetReceiver.onReceive" + AppRTCUtils.getThreadInfo() + ": "
				+ "a=" + intent.getAction() + ", s="
				+ (state == STATE_UNPLUGGED ? "unplugged" : "plugged") + ", m="
				+ (microphone == HAS_MIC ? "mic" : "no mic") + ", n=" + name + ", sb="
				+ isInitialStickyBroadcast());
			hasWiredHeadset = (state == STATE_PLUGGED);
			onUpdateWiredHeadsetState();
		}
	}

	/**
	 * Construction.
	 */
	@UiThread
	public static IAppRTCAudioManager create(Context context) {
		return new AppRTCAudioManager2(context);
	}

	@UiThread
	private AppRTCAudioManager2(Context context) {
		if (DEBUG) Log.d(TAG, "ctor");
		ThreadUtils.checkIsOnMainThread();
		apprtcContext = context;
		audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
		bluetoothManager = AppRTCBluetoothManager.create(context, this);
		wiredHeadsetReceiver = new WiredHeadsetReceiver();
		amState = AudioManagerState.UNINITIALIZED;

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		useSpeakerphone = sharedPreferences.getString(context.getString(R.string.pref_speakerphone_key),
			context.getString(R.string.pref_speakerphone_default));
		if (DEBUG) Log.d(TAG, "useSpeakerphone: " + useSpeakerphone);
		if (useSpeakerphone.equals(SPEAKERPHONE_FALSE)) {
			defaultAudioDevice = AudioDevice.EARPIECE;
		} else {
			defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
		}

		// Create and initialize the proximity sensor.
		// Tablet devices (e.g. Nexus 7) does not support proximity sensors.
		// Note that, the sensor will not be active until start() has been called.
		proximitySensor = AppRTCProximitySensor.create(context,
			// This method will be called each time a state change is detected.
			// Example: user holds his hand over the device (closer than ~5 cm),
			// or removes his hand from the device.
			this::onProximitySensorChangedState);

		if (DEBUG) Log.d(TAG, "defaultAudioDevice: " + defaultAudioDevice);
		AppRTCUtils.logDeviceInfo(TAG);
	}

	/**
	 * get whether audio manager is running or not
	 * @return
	 */
	@Override
	public boolean isStarted() {
		return amState == AudioManagerState.RUNNING;
	}

	@SuppressLint("WrongConstant")
	// TODO(henrika): audioManager.requestAudioFocus() is deprecated.
	@UiThread
	@Override
	public void start(@NonNull final AudioManagerEvents audioManagerEvents) {
		if (DEBUG) Log.d(TAG, "start");
		ThreadUtils.checkIsOnMainThread();
		if (amState == AudioManagerState.RUNNING) {
			if (DEBUG) Log.e(TAG, "AudioManager is already active");
			return;
		}
		// TODO(henrika): perhaps call new method called preInitAudio() here if UNINITIALIZED.

		if (DEBUG) Log.d(TAG, "AudioManager starts...");
		this.audioManagerEvents = audioManagerEvents;
		amState = AudioManagerState.RUNNING;

		// Store current audio state so we can restore it when stop() is called.
		savedAudioMode = audioManager.getMode();
		savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
		savedIsMicrophoneMute = audioManager.isMicrophoneMute();
		hasWiredHeadset = hasWiredHeadset();

		// Create an AudioManager.OnAudioFocusChangeListener instance.
		audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
			// Called on the listener to notify if the audio focus for this listener has been changed.
			// The |focusChange| value indicates whether the focus was gained, whether the focus was lost,
			// and whether that loss is transient, or whether the new focus holder will hold it for an
			// unknown amount of time.
			// TODO(henrika): possibly extend support of handling audio-focus changes. Only contains
			// logging for now.
			@Override
			public void onAudioFocusChange(int focusChange) {
				if (!DEBUG) return;
				final String typeOfChange;
				switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					typeOfChange = "AUDIOFOCUS_GAIN";
					break;
				case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
					typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT";
					break;
				case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
					typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
					break;
				case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
					typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					typeOfChange = "AUDIOFOCUS_LOSS";
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT";
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
					break;
				default:
					typeOfChange = "AUDIOFOCUS_INVALID";
					break;
				}
				Log.d(TAG, "onAudioFocusChange: " + typeOfChange);
			}
		};

		// Request audio playout focus (without ducking) and install listener for changes in focus.
		int result = audioManager.requestAudioFocus(audioFocusChangeListener,
			AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			if (DEBUG) Log.d(TAG, "Audio focus request granted for VOICE_CALL streams");
		} else {
			Log.e(TAG, "Audio focus request failed");
		}

		// Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
		// required to be in this mode when playout and/or recording starts for
		// best possible VoIP performance.
		audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

		// Always disable microphone mute during a WebRTC call.
		setMicrophoneMute(false);

		// Set initial device states.
		userSelectedAudioDevice = AudioDevice.NONE;
		selectedAudioDevice = AudioDevice.NONE;
		audioDevices.clear();

		// Initialize and start Bluetooth if a BT device is available or initiate
		// detection of new (enabled) BT devices.
		bluetoothManager.start();

		if (proximitySensor != null) {
			proximitySensor.start();
		}
		// Do initial selection of audio device. This setting can later be changed
		// either by adding/removing a BT or wired headset or by covering/uncovering
		// the proximity sensor.
		updateAudioDeviceState();

		// Register receiver for broadcast intents related to adding/removing a
		// wired headset.
		registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		if (DEBUG) Log.d(TAG, "AudioManager started");
	}

	@SuppressLint("WrongConstant")
	// TODO(henrika): audioManager.abandonAudioFocus() is deprecated.
	@UiThread
	@Override
	public void stop() {
		if (DEBUG) Log.d(TAG, "stop");
		ThreadUtils.checkIsOnMainThread();
		if (amState != AudioManagerState.RUNNING) {
			if (DEBUG) Log.e(TAG, "Trying to stop AudioManager in incorrect state: " + amState);
			return;
		}
		amState = AudioManagerState.UNINITIALIZED;

		unregisterReceiver(wiredHeadsetReceiver);

		bluetoothManager.stop();

		// Restore previously stored audio states.
		setSpeakerphoneOn(savedIsSpeakerPhoneOn);
		setMicrophoneMute(savedIsMicrophoneMute);
		audioManager.setMode(savedAudioMode);

		// Abandon audio focus. Gives the previous focus owner, if any, focus.
		audioManager.abandonAudioFocus(audioFocusChangeListener);
		audioFocusChangeListener = null;
		if (DEBUG) Log.d(TAG, "Abandoned audio focus for VOICE_CALL streams");

		if (proximitySensor != null) {
			proximitySensor.stop();
			proximitySensor = null;
		}

		audioManagerEvents = null;
		if (DEBUG) Log.d(TAG, "AudioManager stopped");
	}

	/**
	 * Changes selection of the currently active audio device.
	 */
	private void setAudioDeviceInternal(AudioDevice device) {
		if (DEBUG) Log.d(TAG, "setAudioDeviceInternal(device=" + device + ")");
		AppRTCUtils.assertIsTrue(audioDevices.contains(device));

		switch (device) {
		case SPEAKER_PHONE:
			setSpeakerphoneOn(true);
			break;
		case EARPIECE:
		case WIRED_HEADSET:
		case BLUETOOTH:
			setSpeakerphoneOn(false);
			break;
		default:
			Log.e(TAG, "Invalid audio device selection");
			break;
		}
		selectedAudioDevice = device;
	}

	/**
	 * Changes default audio device.
	 * TODO(henrika): add usage of this method in the AppRTCMobile client.
	 */
	@UiThread
	@Override
	public void setDefaultAudioDevice(@NonNull final AudioDevice defaultDevice) {
		ThreadUtils.checkIsOnMainThread();
		switch (defaultDevice) {
		case SPEAKER_PHONE:
			defaultAudioDevice = defaultDevice;
			break;
		case EARPIECE:
			if (hasEarpiece()) {
				defaultAudioDevice = defaultDevice;
			} else {
				defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
			}
			break;
		default:
			Log.e(TAG, "Invalid default audio device selection");
			break;
		}
		if (DEBUG) Log.d(TAG, "setDefaultAudioDevice(device=" + defaultAudioDevice + ")");
		updateAudioDeviceState();
	}

	/**
	 * Changes selection of the currently active audio device.
	 */
	@UiThread
	@Override
	public void selectAudioDevice(@NonNull final AudioDevice device) {
		ThreadUtils.checkIsOnMainThread();
		if (!audioDevices.contains(device)) {
			Log.e(TAG, "Can not select " + device + " from available " + audioDevices);
		}
		userSelectedAudioDevice = device;
		updateAudioDeviceState();
	}

	/**
	 * Returns current set of available/selectable audio devices.
	 */
	@UiThread
	@NonNull
	@Override
	public Set<AudioDevice> getAudioDevices() {
		ThreadUtils.checkIsOnMainThread();
		return Collections.unmodifiableSet(new HashSet<>(audioDevices));
	}

	/**
	 * Returns the currently selected audio device.
	 */
	@UiThread
	@NonNull
	@Override
	public AudioDevice getSelectedAudioDevice() {
		ThreadUtils.checkIsOnMainThread();
		return selectedAudioDevice;
	}

	/**
	 * Helper method for receiver registration.
	 */
	private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		apprtcContext.registerReceiver(receiver, filter);
	}

	/**
	 * Helper method for unregistration of an existing receiver.
	 */
	private void unregisterReceiver(BroadcastReceiver receiver) {
		apprtcContext.unregisterReceiver(receiver);
	}

	/**
	 * Sets the speaker phone mode.
	 */
	private void setSpeakerphoneOn(boolean on) {
		boolean wasOn = audioManager.isSpeakerphoneOn();
		if (DEBUG) Log.d(TAG, "setSpeakerphoneOn:" + wasOn + "=>" + on);
		if (wasOn == on) {
			return;
		}
		audioManager.setSpeakerphoneOn(on);
	}

	/**
	 * Sets the microphone mute state.
	 */
	private void setMicrophoneMute(boolean on) {
		boolean wasMuted = audioManager.isMicrophoneMute();
		if (DEBUG) Log.d(TAG, "setMicrophoneMute:" + wasMuted + "=>" + on);
		if (wasMuted == on) {
			return;
		}
		audioManager.setMicrophoneMute(on);
	}

	/**
	 * Gets the current earpiece state.
	 */
	private boolean hasEarpiece() {
		return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	/**
	 * Checks whether a wired headset is connected or not.
	 * This is not a valid indication that audio playback is actually over
	 * the wired headset as audio routing depends on other conditions. We
	 * only use it as an early indicator (during initialization) of an attached
	 * wired headset.
	 */
	@SuppressWarnings("deprecation")
	private boolean hasWiredHeadset() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return audioManager.isWiredHeadsetOn();
		} else {
     		@SuppressLint("WrongConstant")
			final AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
			for (AudioDeviceInfo device : devices) {
				final int type = device.getType();
				if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
					if (DEBUG) Log.d(TAG, "hasWiredHeadset: found wired headset");
					return true;
				} else if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
					if (DEBUG) Log.d(TAG, "hasWiredHeadset: found USB audio device");
					return true;
				}
			}
			return false;
		}
	}

	@UiThread
	private boolean updateDevices() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.d(TAG, "updateDevices:");
		// Check if any Bluetooth headset is connected. The internal BT state will
		// change accordingly.
		// TODO(henrika): perhaps wrap required state into BT manager.
		if (AppRTCBluetoothManager.NEED_UPDATE.contains(bluetoothManager.getState())) {
			bluetoothManager.updateDevice();
		}

		// Update the set of available audio devices.
		final Set<AudioDevice> newAudioDevices = new HashSet<>();

		if (AppRTCBluetoothManager.HAS_BT_SCO.contains(bluetoothManager.getState())) {
			if (DEBUG) Log.d(TAG, "add BLUETOOTH");
			newAudioDevices.add(AudioDevice.BLUETOOTH);
		}

		if (hasWiredHeadset) {
			// If a wired headset is connected, then it is the only possible option.
			if (DEBUG) Log.d(TAG, "add WIRED_HEADSET");
			newAudioDevices.add(AudioDevice.WIRED_HEADSET);
			if (useSpeakerphone.equals(SPEAKERPHONE_AS_POSSIBLE)) {
				// asPossibleの時は有線ヘッドセットへの自動切り替えをしないので
				// 内蔵マイク/スピーカーも選択肢に含める
				if (DEBUG) Log.d(TAG, "add SPEAKER_PHONE");
				newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
				if (hasEarpiece()) {
					if (DEBUG) Log.d(TAG, "add EARPIECE");
					newAudioDevices.add(AudioDevice.EARPIECE);
				}
			}
		} else {
			// No wired headset, hence the audio-device list can contain speaker
			// phone (on a tablet), or speaker phone and earpiece (on mobile phone).
			if (DEBUG) Log.d(TAG, "add SPEAKER_PHONE");
			newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
			if (hasEarpiece()) {
				if (DEBUG) Log.d(TAG, "add EARPIECE");
				newAudioDevices.add(AudioDevice.EARPIECE);
			}
		}
		// Store state which is set to true if the device list has changed.
		boolean audioDeviceSetUpdated = !audioDevices.equals(newAudioDevices);
		// Update the existing audio device set.
		audioDevices.clear();
		audioDevices.addAll(newAudioDevices);

		return audioDeviceSetUpdated;
	}

	/**
	 * 有線ヘッドセットの接続状態が変化したとき
	 */
	@UiThread
	private void onUpdateWiredHeadsetState() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.d(TAG, "updateWiredHeadsetState: userSelectedAudioDevice=" + userSelectedAudioDevice);
		if (hasWiredHeadset
			&& !useSpeakerphone.equals(SPEAKERPHONE_AS_POSSIBLE)	// asPossible時は自動切り替えしない
			&& (userSelectedAudioDevice != AudioDevice.WIRED_HEADSET)) {
			if (DEBUG) Log.d(TAG, "updateBluetoothHeadsetState: 有線ヘッドセットを選択");
			userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
		}
		updateAudioDeviceState();
	}

	/**
	 * Bluetoothヘッドセットの接続状態が変化したとき
	 * AppRTCBluetoothManagerから呼び出される
	 */
	@UiThread
	@Override
	public void onUpdateBluetoothHeadsetState() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.d(TAG, "onUpdateBluetoothHeadsetState: userSelectedAudioDevice=" + userSelectedAudioDevice);
		if (AppRTCBluetoothManager.NEED_UPDATE.contains(bluetoothManager.getState())) {
			bluetoothManager.updateDevice();
		}
		final boolean hasBTSco = AppRTCBluetoothManager.HAS_BT_SCO.contains(bluetoothManager.getState());
		if (hasBTSco
			&& !useSpeakerphone.equals(SPEAKERPHONE_AS_POSSIBLE)		// asPossible時は自動切り替えしない
			&& (userSelectedAudioDevice != AudioDevice.WIRED_HEADSET)	// WIRED_HEADSETの時は自動切り替えしない
			&& (userSelectedAudioDevice != AudioDevice.BLUETOOTH) ) {
			if (DEBUG) Log.d(TAG, "updateBluetoothHeadsetState: Bluetoothヘッドセットを選択");
			userSelectedAudioDevice = AudioDevice.BLUETOOTH;
		}
		updateAudioDeviceState();
	}

	/**
	 * Updates list of possible audio devices and make new device selection.
	 * TODO(henrika): add unit test to verify all state transitions.
	 */
	@UiThread
	private void updateAudioDeviceState() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.d(TAG, "--- updateAudioDeviceState: "
			+ "wired headset=" + hasWiredHeadset + ", "
			+ "BT state=" + bluetoothManager.getState());
		if (DEBUG) Log.d(TAG, "Device status: "
			+ "available=" + audioDevices + ", "
			+ "selected=" + selectedAudioDevice + ", "
			+ "user selected=" + userSelectedAudioDevice);

		// 接続されている音声機器一覧を更新
		boolean audioDeviceSetUpdated = updateDevices();

		// Bluetoothヘッドセットの接続状態を取得
		final boolean hasBTSco = AppRTCBluetoothManager.HAS_BT_SCO.contains(bluetoothManager.getState());
		// ユーザー選択状態が現在の状態と矛盾しないように調整
		// Correct user selected audio devices if needed.
		if (!hasBTSco
			&& (userSelectedAudioDevice == AudioDevice.BLUETOOTH)) {
			// Bluetoothヘッドセット使用中に取り外された時はユーザー選択を無しにする
			// If BT is not available, it can't be the user selection.
			if (DEBUG) Log.d(TAG, "updateAudioDeviceState: Bluetoothヘッドセットが取り外された");
			userSelectedAudioDevice = AudioDevice.NONE;
		}
		if (!hasWiredHeadset
			&& (userSelectedAudioDevice == AudioDevice.WIRED_HEADSET)) {
			// 有線ヘッドセット使用中に取り外された時はユーザー選択を無しにする
			// If user selected wired headset, but then unplugged wired headset then make
			// speaker phone as user selected device.
			if (DEBUG) Log.d(TAG, "updateAudioDeviceState: 有線ヘッドセットが取り外された");
			userSelectedAudioDevice = AudioDevice.NONE;
		}
		if (useSpeakerphone.equals(SPEAKERPHONE_AS_POSSIBLE)
			&& (userSelectedAudioDevice == AudioDevice.NONE)) {
			// asPossibleでユーザーが明示的に選択していないときは
			// ユーザー選択をスピーカーフォンにする
			userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
		}
		if (DEBUG) Log.d(TAG, "userSelectedAudioDevice=" + userSelectedAudioDevice);

		// Need to start Bluetooth if it is available and user either selected it explicitly or
		// user did not select any output device.
		boolean needBluetoothAudioStart =
			bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
				&& (userSelectedAudioDevice == AudioDevice.NONE
				|| userSelectedAudioDevice == AudioDevice.BLUETOOTH);
		
		// Need to stop Bluetooth audio if user selected different device and
		// Bluetooth SCO connection is established or in the process.
		boolean needBluetoothAudioStop =
			(bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
				|| bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING)
				&& (userSelectedAudioDevice != AudioDevice.NONE
				&& userSelectedAudioDevice != AudioDevice.BLUETOOTH);

		if (AppRTCBluetoothManager.HAS_BT_SCO.contains(bluetoothManager.getState())) {
			if (DEBUG) Log.d(TAG, "Need BT audio: start=" + needBluetoothAudioStart + ", "
				+ "stop=" + needBluetoothAudioStop + ", "
				+ "BT state=" + bluetoothManager.getState());
		}

		// Start or stop Bluetooth SCO connection given states set earlier.
		if (needBluetoothAudioStop) {
			if (DEBUG) Log.d(TAG, "stop Bluetooth SCO audio");
			bluetoothManager.stopScoAudio();
			bluetoothManager.updateDevice();
		}
		
		if (needBluetoothAudioStart && !needBluetoothAudioStop) {
			// Attempt to start Bluetooth SCO audio (takes a few second to start).
			if (DEBUG) Log.d(TAG, "start Bluetooth SCO audio");
			if (!bluetoothManager.startScoAudio()) {
				// Remove BLUETOOTH from list of available devices since SCO failed.
				audioDevices.remove(AudioDevice.BLUETOOTH);
				audioDeviceSetUpdated = true;
			}
		}
		
		// Update selected audio device.
		final AudioDevice newAudioDevice;
		if (useSpeakerphone.equals(SPEAKERPHONE_AS_POSSIBLE)
			&& audioDevices.contains(AudioDevice.SPEAKER_PHONE)) {
			newAudioDevice = AudioDevice.SPEAKER_PHONE;
		} else if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
			// If a Bluetooth is connected, then it should be used as output audio
			// device. Note that it is not sufficient that a headset is available;
			// an active SCO channel must also be up and running.
			newAudioDevice = AudioDevice.BLUETOOTH;
		} else if (hasWiredHeadset) {
			// If a wired headset is connected, but Bluetooth is not, then wired headset is used as
			// audio device.
			newAudioDevice = AudioDevice.WIRED_HEADSET;
		} else {
			// No wired headset and no Bluetooth, hence the audio-device list can contain speaker
			// phone (on a tablet), or speaker phone and earpiece (on mobile phone).
			// |defaultAudioDevice| contains either AudioDevice.SPEAKER_PHONE or AudioDevice.EARPIECE
			// depending on the user's selection.
			newAudioDevice = defaultAudioDevice;
		}
		// Switch to new device but only if there has been any changes.
		if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
			// Do the required device switch.
			setAudioDeviceInternal(newAudioDevice);
			if (DEBUG) Log.d(TAG, "New device status: "
				+ "available=" + audioDevices + ", "
				+ "selected=" + selectedAudioDevice);
			if (audioManagerEvents != null) {
				// Notify a listening client that audio device has been changed.
				audioManagerEvents.onAudioDeviceChanged(selectedAudioDevice, audioDevices);
			}
		}
		if (DEBUG) Log.d(TAG, "--- updateAudioDeviceState done");
	}
}
