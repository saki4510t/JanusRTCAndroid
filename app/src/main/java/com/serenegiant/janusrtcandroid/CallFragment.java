package com.serenegiant.janusrtcandroid;/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.webrtc.RendererCommon.ScalingType;

import java.util.Locale;

/**
 * Fragment for call control.
 */
public class CallFragment extends Fragment {
	private static final boolean DEBUG = true;	// set false on production
	private static final String TAG = CallFragment.class.getSimpleName();

	private TextView contactView;
	private ImageButton cameraSwitchButton;
	private ImageButton videoScalingButton;
	private ImageButton toggleMuteButton;
	private TextView captureFormatText;
	private SeekBar captureFormatSlider;
	private OnCallEvents callEvents;
	private ScalingType scalingType;
	private boolean videoCallEnabled = true;
	
	/**
	 * Call control interface for container activity.
	 */
	public interface OnCallEvents {
		void onCallHangUp();
		
		void onCameraSwitch();
		
		void onVideoScalingSwitch(ScalingType scalingType);
		
		void onCaptureFormatChange(int width, int height, int framerate);
		
		boolean onToggleMic();
	}
	
	@Override
	public View onCreateView(
		@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

		final View controlView = inflater.inflate(R.layout.fragment_call, container, false);
		
		// Create UI controls.
		contactView = controlView.findViewById(R.id.contact_name_call);
		final ImageButton disconnectButton = controlView.findViewById(R.id.button_call_disconnect);
		cameraSwitchButton = controlView.findViewById(R.id.button_call_switch_camera);
		videoScalingButton = controlView.findViewById(R.id.button_call_scaling_mode);
		toggleMuteButton = controlView.findViewById(R.id.button_call_toggle_mic);
		captureFormatText = controlView.findViewById(R.id.capture_format_text_call);
		captureFormatSlider = controlView.findViewById(R.id.capture_format_slider_call);
		
		// Add buttons click events.
		disconnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				callEvents.onCallHangUp();
			}
		});
		
		cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				callEvents.onCameraSwitch();
			}
		});
		
		videoScalingButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
					videoScalingButton.setBackgroundResource(R.drawable.ic_action_full_screen);
					scalingType = ScalingType.SCALE_ASPECT_FIT;
				} else {
					videoScalingButton.setBackgroundResource(R.drawable.ic_action_return_from_full_screen);
					scalingType = ScalingType.SCALE_ASPECT_FILL;
				}
				callEvents.onVideoScalingSwitch(scalingType);
			}
		});
		scalingType = ScalingType.SCALE_ASPECT_FILL;
		
		toggleMuteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View view) {
				boolean enabled = callEvents.onToggleMic();
				toggleMuteButton.setAlpha(enabled ? 1.0f : 0.3f);
			}
		});
		
		return controlView;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		boolean captureSliderEnabled = false;
		final Bundle args = getArguments();
		if (args != null) {
			final int roomId = args.getInt(CallActivity.EXTRA_ROOMID);
			contactView.setText(String.format(Locale.US, "%d", roomId));
			videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true);
			captureSliderEnabled = videoCallEnabled
				&& args.getBoolean(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, false);
		}
		if (!videoCallEnabled) {
			cameraSwitchButton.setVisibility(View.INVISIBLE);
		}
		if (captureSliderEnabled) {
			captureFormatSlider.setOnSeekBarChangeListener(
				new CaptureQualityController(captureFormatText, callEvents));
		} else {
			captureFormatText.setVisibility(View.GONE);
			captureFormatSlider.setVisibility(View.GONE);
		}
	}
	
	// TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(final Activity activity) {
		super.onAttach(activity);
		callEvents = (OnCallEvents) activity;
	}
}
