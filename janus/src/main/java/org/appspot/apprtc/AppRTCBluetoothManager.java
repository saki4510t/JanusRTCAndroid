package org.appspot.apprtc;/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import org.appspot.apprtc.util.AppRTCUtils;
import org.webrtc.ThreadUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;

/**
 * AppRTCProximitySensor manages functions related to Bluetoth devices in the
 * AppRTC demo.
 */
@SuppressLint("MissingPermission")
public class AppRTCBluetoothManager {
	private static final boolean DEBUG = false; // set false on production
	private static final String TAG = AppRTCBluetoothManager.class.getSimpleName();

	/**
	 * Bluetoothの接続状態が変化したときのコールバックリスナー
	 */
	public interface UpdateBluetoothStateListener {
		@UiThread
		public void onUpdateBluetoothHeadsetState(
			final State prevState, final State newState);
	}

	// Timeout interval for starting or stopping audio to a Bluetooth SCO device.
	private static final int BLUETOOTH_SCO_TIMEOUT_MS = 4000;
	// Maximum number of SCO connection attempts.
	private static final int MAX_SCO_CONNECTION_ATTEMPTS = 2;

	// Bluetooth connection state.
	public enum State {
		// Bluetooth is not available; no adapter or Bluetooth is off.
		UNINITIALIZED,
		// Bluetooth error happened when trying to start Bluetooth.
		ERROR,
		// Bluetooth proxy object for the Headset profile exists, but no connected headset devices,
		// SCO is not started or disconnected.
		HEADSET_UNAVAILABLE,
		// Bluetooth proxy object for the Headset profile connected, connected Bluetooth headset
		// present, but SCO is not started or disconnected.
		HEADSET_AVAILABLE,
		// Bluetooth audio SCO connection with remote device is closing.
		SCO_DISCONNECTING,
		// Bluetooth audio SCO connection with remote device is initiated.
		SCO_CONNECTING,
		// Bluetooth audio SCO connection with remote device is established.
		SCO_CONNECTED
	}

	public static final EnumSet<State> HAS_BT_SCO = EnumSet.of(
		State.HEADSET_AVAILABLE, State.SCO_CONNECTING,  State.SCO_CONNECTED
	);

	public static final EnumSet<State> BT_SCO_START = EnumSet.of(
		State.SCO_CONNECTING,  State.SCO_CONNECTED
	);

	public static final EnumSet<State> NEED_UPDATE = EnumSet.of(
		State.HEADSET_AVAILABLE, State.HEADSET_UNAVAILABLE, State.SCO_DISCONNECTING
	);

	public static final EnumSet<State> INTERMEDIATE_STATE = EnumSet.of(
		State.SCO_CONNECTING
	);

	private final Context apprtcContext;
	private final UpdateBluetoothStateListener mListener;
	@Nullable
	private final AudioManager audioManager;
	private final Handler handler;

	private int scoConnectionAttempts;
	private State bluetoothState;
	private final BluetoothProfile.ServiceListener bluetoothServiceListener;
	@Nullable
	private BluetoothAdapter bluetoothAdapter;
	@Nullable
	private BluetoothHeadset bluetoothHeadset;
	@Nullable
	private BluetoothDevice bluetoothDevice;
	private final BroadcastReceiver bluetoothHeadsetReceiver;

	// Runs when the Bluetooth timeout expires. We use that timeout after calling
	// startScoAudio() or stopScoAudio() because we're not guaranteed to get a
	// callback after those calls.
	private final Runnable bluetoothTimeoutRunnable = new Runnable() {
		@Override
		public void run() {
			bluetoothTimeout();
		}
	};

	/**
	 * Implementation of an interface that notifies BluetoothProfile IPC clients when they have been
	 * connected to or disconnected from the service.
	 */
	private class BluetoothServiceListener implements BluetoothProfile.ServiceListener {
		@Override
		// Called to notify the client when the proxy object has been connected to the service.
		// Once we have the profile proxy object, we can use it to monitor the state of the
		// connection and perform other operations that are relevant to the headset profile.
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
				return;
			}
			if (DEBUG) Log.d(TAG, "onServiceConnected: BT state=" + bluetoothState);
			// Android only supports one connected Bluetooth Headset at a time.
			bluetoothHeadset = (BluetoothHeadset) proxy;
			updateAudioDeviceState(updateDeviceInternal());
			if (DEBUG) Log.d(TAG, "onServiceConnected done: BT state=" + bluetoothState);
		}

		/** Notifies the client when the proxy object has been disconnected from the service. */
		@Override
		public void onServiceDisconnected(int profile) {
			if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) {
				return;
			}
			if (DEBUG) Log.d(TAG, "onServiceDisconnected: BT state=" + bluetoothState);
			stopScoAudioInternal();
			bluetoothHeadset = null;
			bluetoothDevice = null;
			updateAudioDeviceState(State.HEADSET_UNAVAILABLE);
			if (DEBUG) Log.d(TAG, "onServiceDisconnected done: BT state=" + bluetoothState);
		}
	}

	// Intent broadcast receiver which handles changes in Bluetooth device availability.
	// Detects headset changes and Bluetooth SCO state changes.
	private class BluetoothHeadsetBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (bluetoothState == State.UNINITIALIZED) {
				return;
			}
			final String action = intent.getAction();
			if (DEBUG) Log.v(TAG, "onReceive:action=" + action);
			// Change in connection state of the Headset profile. Note that the
			// change does not tell us anything about whether we're streaming
			// audio to BT over SCO. Typically received when user turns on a BT
			// headset while audio is active using another audio device.
			if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
				final int state =
					intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
				if (DEBUG) Log.d(TAG, "onReceive:action=ACTION_CONNECTION_STATE_CHANGED, "
					+ "state=" + connectionStateToString(state) + ", "
					+ "isInitialStickyBroadcast=" + isInitialStickyBroadcast() + ", "
					+ "bluetoothState=" + bluetoothState);
				if (state == BluetoothHeadset.STATE_CONNECTED) {
					if (DEBUG) Log.d(TAG, "onReceive:STATE_CONNECTED");
					scoConnectionAttempts = 0;
					updateAudioDeviceState(updateDeviceInternal());
				} else if (state == BluetoothHeadset.STATE_CONNECTING) {
					if (DEBUG) Log.d(TAG, "onReceive:STATE_CONNECTING, No action needed");
				} else if (state == BluetoothHeadset.STATE_DISCONNECTING) {
					if (DEBUG) Log.d(TAG, "onReceive:STATE_DISCONNECTING");
					if ((bluetoothState == State.SCO_CONNECTING)
						|| (bluetoothState == State.SCO_CONNECTED)) {
						updateAudioDeviceState(State.SCO_DISCONNECTING);
					}
				} else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
					if (DEBUG) Log.d(TAG, "onReceive:STATE_DISCONNECTED");
					// Bluetooth is probably powered off during the call.
					updateAudioDeviceState(updateDeviceInternal());
				}
				// Change in the audio (SCO) connection state of the Headset profile.
				// Typically received after call to startScoAudio() has finalized.
			} else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
				final int state = intent.getIntExtra(
					BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
				if (DEBUG) Log.d(TAG, "onReceive:action=ACTION_AUDIO_STATE_CHANGED, "
					+ "audioState=" + audioStateToString(state) + ", "
					+ "isInitialStickyBroadcast=" + isInitialStickyBroadcast() + ", "
					+ "bluetoothState=" + bluetoothState);
				if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
					cancelTimer();
					if (bluetoothState == State.SCO_CONNECTING) {
						if (DEBUG) Log.d(TAG, "+++ Bluetooth audio SCO is now connected");
						scoConnectionAttempts = 0;
						updateAudioDeviceState(State.SCO_CONNECTED);
					} else {
						Log.w(TAG, "Unexpected state " + bluetoothState + ",expected=SCO_CONNECTING");
					}
				} else if (state == BluetoothHeadset.STATE_AUDIO_CONNECTING) {
					if (DEBUG) Log.d(TAG, "+++ Bluetooth audio SCO is now connecting...");
				} else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
					if (DEBUG) Log.d(TAG, "+++ Bluetooth audio SCO is now disconnected");
					if (isInitialStickyBroadcast()) {
						if (DEBUG) Log.d(TAG, "Ignore STATE_AUDIO_DISCONNECTED initial sticky broadcast.");
						return;
					}
					if ((bluetoothState == State.SCO_CONNECTING)
						|| (bluetoothState == State.SCO_CONNECTED)) {
						updateAudioDeviceState(State.SCO_DISCONNECTING);
					} else {
						updateAudioDeviceState(updateDeviceInternal());
					}
				}
			}
			if (DEBUG) Log.d(TAG, "onReceive done: BT state=" + bluetoothState);
		}
	}

	/**
	 * Construction.
	 */
	@UiThread
	static AppRTCBluetoothManager create(
		@NonNull final Context context,
		@NonNull final UpdateBluetoothStateListener listener) {
		if (DEBUG) Log.d(TAG, "create" + AppRTCUtils.getThreadInfo());
		return new AppRTCBluetoothManager(context, listener);
	}

	@UiThread
	protected AppRTCBluetoothManager(
		@NonNull final Context context,
		@NonNull final UpdateBluetoothStateListener listener) {

		if (DEBUG) Log.v(TAG, "ctor");
		ThreadUtils.checkIsOnMainThread();
		apprtcContext = context;
		mListener = listener;
		this.audioManager = getAudioManager(context);
		bluetoothState = State.UNINITIALIZED;
		bluetoothServiceListener = new BluetoothServiceListener();
		bluetoothHeadsetReceiver = new BluetoothHeadsetBroadcastReceiver();
		handler = new Handler(Looper.getMainLooper());
	}

	/**
	 * Returns the internal state.
	 */
	@UiThread
	public State getState() {
		ThreadUtils.checkIsOnMainThread();
		return bluetoothState;
	}

	@UiThread
	public boolean hasBTSco() {
		return AppRTCBluetoothManager.HAS_BT_SCO.contains(getState());
	}

	/**
	 * Activates components required to detect Bluetooth devices and to enable
	 * BT SCO (audio is routed via BT SCO) for the headset profile. The end
	 * state will be HEADSET_UNAVAILABLE but a state machine has started which
	 * will start a state change sequence where the final outcome depends on
	 * if/when the BT headset is enabled.
	 * Example of state change sequence when start() is called while BT device
	 * is connected and enabled:
	 * UNINITIALIZED --> HEADSET_UNAVAILABLE --> HEADSET_AVAILABLE -->
	 * SCO_CONNECTING --> SCO_CONNECTED <==> audio is now routed via BT SCO.
	 * Note that the AppRTCAudioManager is also involved in driving this state
	 * change.
	 */
	@UiThread
	public void start() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "start");
		if (!hasPermission(apprtcContext, android.Manifest.permission.BLUETOOTH)) {
			Log.w(TAG, "Process (pid=" + Process.myPid() + ") lacks BLUETOOTH permission");
			return;
		}
		if (bluetoothState != State.UNINITIALIZED) {
			Log.w(TAG, "Invalid BT state");
			return;
		}
		bluetoothHeadset = null;
		bluetoothDevice = null;
		scoConnectionAttempts = 0;
		// Get a handle to the default local Bluetooth adapter.
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			Log.w(TAG, "Device does not support Bluetooth");
			return;
		}
		// Ensure that the device supports use of BT SCO audio for off call use cases.
		if (!audioManager.isBluetoothScoAvailableOffCall()) {
			Log.e(TAG, "Bluetooth SCO audio is not available off call");
			return;
		}
		logBluetoothAdapterInfo(bluetoothAdapter);
		// Establish a connection to the HEADSET profile (includes both Bluetooth Headset and
		// Hands-Free) proxy object and install a listener.
		if (!getBluetoothProfileProxy(
			apprtcContext, bluetoothServiceListener, BluetoothProfile.HEADSET)) {
			Log.e(TAG, "BluetoothAdapter.getProfileProxy(HEADSET) failed");
			return;
		}
		// Register receivers for BluetoothHeadset change notifications.
		IntentFilter bluetoothHeadsetFilter = new IntentFilter();
		// Register receiver for change in connection state of the Headset profile.
		bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
		// Register receiver for change in audio connection state of the Headset profile.
		bluetoothHeadsetFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
		registerReceiver(bluetoothHeadsetReceiver, bluetoothHeadsetFilter);
		if (DEBUG) Log.d(TAG, "HEADSET profile state: "
			+ connectionStateToString(bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)));
		if (DEBUG) Log.d(TAG, "Bluetooth proxy for headset profile has started");
		bluetoothState = State.HEADSET_UNAVAILABLE;
		if (DEBUG) Log.d(TAG, "start done: BT state=" + bluetoothState);
	}

	/**
	 * Stops and closes all components related to Bluetooth audio.
	 */
	@UiThread
	public void stop() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "stop: BT state=" + bluetoothState);
		if (bluetoothAdapter == null) {
			return;
		}
		// Stop BT SCO connection with remote device if needed.
		if (stopScoAudioInternal()) {
			bluetoothState = State.SCO_DISCONNECTING;
		}
		// Close down remaining BT resources.
		if (bluetoothState == State.UNINITIALIZED) {
			return;
		}
		unregisterReceiver(bluetoothHeadsetReceiver);
		cancelTimer();
		if (bluetoothHeadset != null) {
			bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset);
			bluetoothHeadset = null;
		}
		bluetoothAdapter = null;
		bluetoothDevice = null;
		bluetoothState = State.UNINITIALIZED;
		if (DEBUG) Log.d(TAG, "stop done: BT state=" + bluetoothState);
	}

	/**
	 * Starts Bluetooth SCO connection with remote device.
	 * Note that the phone application always has the priority on the usage of the SCO connection
	 * for telephony. If this method is called while the phone is in call it will be ignored.
	 * Similarly, if a call is received or sent while an application is using the SCO connection,
	 * the connection will be lost for the application and NOT returned automatically when the call
	 * ends. Also note that: up to and including API version JELLY_BEAN_MR1, this method initiates a
	 * virtual voice call to the Bluetooth headset. After API version JELLY_BEAN_MR2 only a raw SCO
	 * audio connection is established.
	 * TODO(henrika): should we add support for virtual voice call to BT headset also for JBMR2 and
	 * higher. It might be required to initiates a virtual voice call since many devices do not
	 * accept SCO audio without a "call".
	 */
	@UiThread
	public boolean startScoAudio() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "startScoAudio: BT state=" + bluetoothState + ", "
			+ "attempts: " + scoConnectionAttempts + ", "
			+ "SCO is on: " + isScoOn());
		if (scoConnectionAttempts >= MAX_SCO_CONNECTION_ATTEMPTS) {
			Log.e(TAG, "BT SCO connection fails - no more attempts");
			return false;
		}
		if (bluetoothState != State.HEADSET_AVAILABLE) {
			Log.e(TAG, "BT SCO connection fails - no headset available");
			return false;
		}
		// Start BT SCO channel and wait for ACTION_AUDIO_STATE_CHANGED.
		if (DEBUG) Log.d(TAG, "Starting Bluetooth SCO and waits for ACTION_AUDIO_STATE_CHANGED...");
		// The SCO connection establishment can take several seconds, hence we cannot rely on the
		// connection to be available when the method returns but instead register to receive the
		// intent ACTION_SCO_AUDIO_STATE_UPDATED and wait for the state to be SCO_AUDIO_STATE_CONNECTED.
		bluetoothState = State.SCO_CONNECTING;
		audioManager.startBluetoothSco();
		audioManager.setBluetoothScoOn(true);
		scoConnectionAttempts++;
		startTimer();
		if (DEBUG) Log.d(TAG, "startScoAudio done: BT state=" + bluetoothState + ", "
			+ "SCO is on: " + isScoOn());
		return true;
	}

	/**
	 * Stops Bluetooth SCO connection with remote device.
	 */
	@UiThread
	public void stopScoAudio() {
		if (stopScoAudioInternal()) {
			bluetoothState = State.SCO_DISCONNECTING;
			if (DEBUG) Log.d(TAG, "stopScoAudio done: BT state=" + bluetoothState + ", "
				+ "SCO is on: " + isScoOn());
		}
	}

	/**
	 * sco audioの停止処理
	 * @return true: 実際に停止処理を行った, false: すでに停止中なので何もしなかった
	 */
	@UiThread
	private boolean stopScoAudioInternal() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "stopScoAudioInternal: BT state=" + bluetoothState + ", "
			+ "SCO is on: " + isScoOn());
		if (bluetoothState != State.SCO_CONNECTING && bluetoothState != State.SCO_CONNECTED) {
			if (DEBUG) Log.d(TAG, "stopScoAudioInternal:Not a connecting/connected state, skip");
			return false;
		}
		cancelTimer();
		audioManager.stopBluetoothSco();
		audioManager.setBluetoothScoOn(false);
		return true;
	}

	/**
	 * Use the BluetoothHeadset proxy object (controls the Bluetooth Headset
	 * Service via IPC) to update the list of connected devices for the HEADSET
	 * profile. The internal state will change to HEADSET_UNAVAILABLE or to
	 * HEADSET_AVAILABLE and |bluetoothDevice| will be mapped to the connected
	 * device if available.
	 */
	public State updateDevice() {
		if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
			return bluetoothState;
		}
		if (DEBUG) Log.v(TAG, "updateDevice");
		bluetoothState = updateDeviceInternal();
		return bluetoothState;
	}

	private State updateDeviceInternal() {
		if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
			return bluetoothState;
		}
		if (DEBUG) Log.v(TAG, "updateDeviceInternal");
		// Get connected devices for the headset profile. Returns the set of
		// devices which are in state STATE_CONNECTED. The BluetoothDevice class
		// is just a thin wrapper for a Bluetooth hardware address.
		final List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
		final State newState;
		if (devices.isEmpty()) {
			bluetoothDevice = null;
			newState = State.HEADSET_UNAVAILABLE;
			if (DEBUG) Log.d(TAG, "No connected bluetooth headset");
		} else {
			// Always use first device in list. Android only supports one device.
			bluetoothDevice = devices.get(0);
			newState = State.HEADSET_AVAILABLE;
			if (DEBUG) Log.d(TAG, "Connected bluetooth headset: "
				+ "name=" + bluetoothDevice.getName() + ", "
				+ "state=" + connectionStateToString(bluetoothHeadset.getConnectionState(bluetoothDevice))
				+ ", SCO audio=" + bluetoothHeadset.isAudioConnected(bluetoothDevice));
		}
		if (DEBUG) Log.d(TAG, "updateDeviceInternal done: BT state=" + newState);
		return newState;
	}

	/**
	 * Stubs for test mocks.
	 */
	@Nullable
	protected AudioManager getAudioManager(Context context) {
		return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	protected void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
		apprtcContext.registerReceiver(receiver, filter);
	}

	protected void unregisterReceiver(BroadcastReceiver receiver) {
		apprtcContext.unregisterReceiver(receiver);
	}

	protected boolean getBluetoothProfileProxy(
		Context context, BluetoothProfile.ServiceListener listener, int profile) {
		return bluetoothAdapter.getProfileProxy(context, listener, profile);
	}

	protected boolean hasPermission(Context context, String permission) {
		return apprtcContext.checkPermission(permission, Process.myPid(), Process.myUid())
			== PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Logs the state of the local Bluetooth adapter.
	 */
	@SuppressLint("HardwareIds")
	protected void logBluetoothAdapterInfo(BluetoothAdapter localAdapter) {
		if (DEBUG) Log.d(TAG, "BluetoothAdapter: "
			+ "enabled=" + localAdapter.isEnabled() + ", "
			+ "state=" + connectionStateToString(localAdapter.getState()) + ", "
			+ "name=" + localAdapter.getName() + ", "
			+ "address=" + localAdapter.getAddress());
		// Log the set of BluetoothDevice objects that are bonded (paired) to the local adapter.
		Set<BluetoothDevice> pairedDevices = localAdapter.getBondedDevices();
		if (!pairedDevices.isEmpty()) {
			if (DEBUG) Log.d(TAG, "paired devices:");
			for (BluetoothDevice device : pairedDevices) {
				if (DEBUG) Log.d(TAG, " name=" + device.getName() + ", address=" + device.getAddress());
			}
		}
	}

	/**
	 * Ensures that the audio manager updates its list of available audio devices.
	 */
	@UiThread
	private void updateAudioDeviceState(final State newState) {
		ThreadUtils.checkIsOnMainThread();
		final State prev = bluetoothState;
		if (DEBUG) Log.v(TAG, "updateAudioDeviceState:" + prev + "=>" + newState);
		if (prev != newState) {
			bluetoothState = newState;
			mListener.onUpdateBluetoothHeadsetState(prev, bluetoothState);
		}
	}

	/**
	 * Starts timer which times out after BLUETOOTH_SCO_TIMEOUT_MS milliseconds.
	 */
	@UiThread
	private void startTimer() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "startTimer");
		handler.postDelayed(bluetoothTimeoutRunnable, BLUETOOTH_SCO_TIMEOUT_MS);
	}

	/**
	 * Cancels any outstanding timer tasks.
	 */
	@UiThread
	private void cancelTimer() {
		ThreadUtils.checkIsOnMainThread();
		if (DEBUG) Log.v(TAG, "cancelTimer");
		handler.removeCallbacks(bluetoothTimeoutRunnable);
	}

	/**
	 * Called when start of the BT SCO channel takes too long time. Usually
	 * happens when the BT device has been turned on during an ongoing call.
	 */
	@UiThread
	private void bluetoothTimeout() {
		ThreadUtils.checkIsOnMainThread();
		if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) {
			return;
		}
		if (DEBUG) Log.v(TAG, "bluetoothTimeout: BT state=" + bluetoothState + ", "
			+ "attempts: " + scoConnectionAttempts + ", "
			+ "SCO is on: " + isScoOn());
		if (bluetoothState != State.SCO_CONNECTING) {
			return;
		}
		// Bluetooth SCO should be connecting; check the latest result.
		boolean scoConnected = false;
		final List<BluetoothDevice> devices = bluetoothHeadset.getConnectedDevices();
		if (!devices.isEmpty()) {
			for (final BluetoothDevice device: devices) {
				if (bluetoothHeadset.isAudioConnected(device)) {
					if (DEBUG) Log.d(TAG, "SCO connected with " + device.getName());
					scoConnected = true;
					bluetoothDevice = device;
					break;
				} else {
					if (DEBUG) Log.d(TAG, "SCO is not connected with " + device.getName());
				}
			}
		}
		if (scoConnected) {
			// We thought BT had timed out, but it's actually on; updating state.
			scoConnectionAttempts = 0;
			updateAudioDeviceState(State.SCO_CONNECTED);
		} else {
			// Give up and "cancel" our request by calling stopBluetoothSco().
			Log.w(TAG, "BT failed to connect after timeout");
			if (stopScoAudioInternal()) {
				updateAudioDeviceState(State.SCO_DISCONNECTING);
			}
		}
		if (DEBUG) Log.d(TAG, "bluetoothTimeout done: BT state=" + bluetoothState);
	}

	/**
	 * Checks whether audio uses Bluetooth SCO.
	 */
	private boolean isScoOn() {
		return audioManager.isBluetoothScoOn();
	}

	/**
	 * Converts BluetoothAdapter states into local string representations.
	 */
	private String connectionStateToString(int state) {
		switch (state) {
		case BluetoothAdapter.STATE_DISCONNECTED:
			return "DISCONNECTED";
		case BluetoothAdapter.STATE_CONNECTED:
			return "CONNECTED";
		case BluetoothAdapter.STATE_CONNECTING:
			return "CONNECTING";
		case BluetoothAdapter.STATE_DISCONNECTING:
			return "DISCONNECTING";
		case BluetoothAdapter.STATE_OFF:
			return "OFF";
		case BluetoothAdapter.STATE_ON:
			return "ON";
		case BluetoothAdapter.STATE_TURNING_OFF:
			// Indicates the local Bluetooth adapter is turning off. Local clients should immediately
			// attempt graceful disconnection of any remote links.
			return "TURNING_OFF";
		case BluetoothAdapter.STATE_TURNING_ON:
			// Indicates the local Bluetooth adapter is turning on. However local clients should wait
			// for STATE_ON before attempting to use the adapter.
			return "TURNING_ON";
		default:
			return "INVALID";
		}
	}

	/**
	 * Converts audio states into local string representations.
	 */
	private String audioStateToString(int state) {
		switch (state) {
		case BluetoothHeadset.STATE_AUDIO_CONNECTING:
			return "AUDIO_CONNECTING";
		case BluetoothHeadset.STATE_AUDIO_CONNECTED:
			return "AUDIO_CONNECTED";
		case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
			return "AUDIO_DISCONNECTED";
		default:
			return "INVALID";
		}
	}
}
