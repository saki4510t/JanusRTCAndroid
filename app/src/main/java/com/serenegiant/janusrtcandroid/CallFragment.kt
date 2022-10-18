package com.serenegiant.janusrtcandroid

import android.content.Context
import android.widget.TextView
import android.widget.ImageButton
import android.widget.SeekBar
import org.webrtc.RendererCommon.ScalingType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import java.util.*

/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 - 2022 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */ /**
 * Fragment for call control.
 */
class CallFragment : Fragment() {
	private lateinit var contactView: TextView
	private lateinit var cameraSwitchButton: ImageButton
	private lateinit var videoScalingButton: ImageButton
	private lateinit var toggleMuteButton: ImageButton
	private lateinit var captureFormatText: TextView
	private lateinit var captureFormatSlider: SeekBar
	private var scalingType: ScalingType? = null
	private var videoCallEnabled = true

	/**
	 * Call control interface for container activity.
	 */
	interface OnCallEvents {
		fun onCallHangUp()
		fun onCameraSwitch()
		fun onVideoScalingSwitch(scalingType: ScalingType?)
		fun onCaptureFormatChange(width: Int, height: Int, framerate: Int)
		fun onToggleMic(): Boolean
	}

	// TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
	override fun onAttach(context: Context) {
		super.onAttach(context)
		if (DEBUG) Log.v(TAG, "onAttach:")
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		if (DEBUG) Log.v(TAG, "onCreateView:")

		val controlView = inflater.inflate(R.layout.fragment_call, container, false)
		// Create UI controls.
		contactView = controlView.findViewById(R.id.contact_name_call)
		val disconnectButton = controlView.findViewById<ImageButton>(R.id.button_call_disconnect)
		cameraSwitchButton = controlView.findViewById(R.id.button_call_switch_camera)
		videoScalingButton = controlView.findViewById(R.id.button_call_scaling_mode)
		toggleMuteButton = controlView.findViewById(R.id.button_call_toggle_mic)
		captureFormatText = controlView.findViewById(R.id.capture_format_text_call)
		captureFormatSlider = controlView.findViewById(R.id.capture_format_slider_call)

		val callEvents = requireOnCallEvents()
		// Add buttons click events.
		disconnectButton.setOnClickListener { callEvents.onCallHangUp() }
		cameraSwitchButton.setOnClickListener { callEvents.onCameraSwitch() }
		videoScalingButton.setOnClickListener {
			scalingType = if (scalingType == ScalingType.SCALE_ASPECT_FILL) {
				videoScalingButton.setBackgroundResource(R.drawable.ic_action_full_screen)
				ScalingType.SCALE_ASPECT_FIT
			} else {
				videoScalingButton.setBackgroundResource(R.drawable.ic_action_return_from_full_screen)
				ScalingType.SCALE_ASPECT_FILL
			}
			callEvents.onVideoScalingSwitch(scalingType)
		}
		scalingType = ScalingType.SCALE_ASPECT_FILL
		toggleMuteButton.setOnClickListener {
			val enabled = callEvents.onToggleMic()
			toggleMuteButton.alpha = if (enabled) 1.0f else 0.3f
		}
		return controlView
	}

	override fun onStart() {
		super.onStart()
		if (DEBUG) Log.v(TAG, "onStart:")
		var captureSliderEnabled = false
		val args = arguments
		if (args != null) {
			val roomId = args.getLong(CallActivity.EXTRA_ROOMID, 0)
			contactView.text = String.format(Locale.US, "%d", roomId)
			videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true)
			captureSliderEnabled = (videoCallEnabled
				&& args.getBoolean(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, false))
		}
		if (!videoCallEnabled) {
			cameraSwitchButton.visibility = View.INVISIBLE
		}
		if (captureSliderEnabled) {
			captureFormatSlider.setOnSeekBarChangeListener(
				CaptureQualityController(captureFormatText, requireOnCallEvents())
			)
		} else {
			captureFormatText.visibility = View.GONE
			captureFormatSlider.visibility = View.GONE
		}
	}

	override fun onDetach() {
		if (DEBUG) Log.v(TAG, "onDetach:")
		super.onDetach()
	}

	private fun requireOnCallEvents(): OnCallEvents {
		return requireContext() as OnCallEvents
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = CallFragment::class.java.simpleName
	}
}
