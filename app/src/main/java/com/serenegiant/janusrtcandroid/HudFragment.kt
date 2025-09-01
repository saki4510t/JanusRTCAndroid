package com.serenegiant.janusrtcandroid
/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.widget.TextView
import android.widget.ImageButton
import kotlin.jvm.Volatile
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.Fragment
import org.webrtc.StatsReport
import org.appspot.apprtc.AppRTCConst
import java.lang.StringBuilder
import java.util.HashMap

/**
 * Fragment for HUD statistics display.
 */
class HudFragment : Fragment() {
	private lateinit var encoderStatView: TextView
	private lateinit var hudViewBwe: TextView
	private lateinit var hudViewConnection: TextView
	private lateinit var hudViewVideoSend: TextView
	private lateinit var hudViewVideoRecv: TextView
	private lateinit var toggleDebugButton: ImageButton
	private var videoCallEnabled = false
	private var displayHud = false

	@Volatile
	private var isRunning = false
	private var cpuMonitor: CpuMonitor? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?, savedInstanceState: Bundle?): View {
		val controlView = inflater.inflate(R.layout.fragment_hud, container, false)

		// Create UI controls.
		encoderStatView = controlView.findViewById(R.id.encoder_stat_call)
		hudViewBwe = controlView.findViewById(R.id.hud_stat_bwe)
		hudViewConnection = controlView.findViewById(R.id.hud_stat_connection)
		hudViewVideoSend = controlView.findViewById(R.id.hud_stat_video_send)
		hudViewVideoRecv = controlView.findViewById(R.id.hud_stat_video_recv)
		toggleDebugButton = controlView.findViewById(R.id.button_toggle_debug)
		toggleDebugButton.setOnClickListener {
			if (displayHud) {
				val visibility =
					if (hudViewBwe.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
				hudViewsSetProperties(visibility)
			}
		}
		return controlView
	}

	override fun onStart() {
		super.onStart()
		val args = arguments
		if (args != null) {
			videoCallEnabled = args.getBoolean(CallActivity.EXTRA_VIDEO_CALL, true)
			displayHud = args.getBoolean(CallActivity.EXTRA_DISPLAY_HUD, false)
		}
		val visibility = if (displayHud) View.VISIBLE else View.INVISIBLE
		encoderStatView.visibility = visibility
		toggleDebugButton.visibility = visibility
		hudViewsSetProperties(View.INVISIBLE)
		isRunning = true
	}

	override fun onStop() {
		isRunning = false
		super.onStop()
	}

	fun setCpuMonitor(cpuMonitor: CpuMonitor?) {
		this.cpuMonitor = cpuMonitor
	}

	private fun hudViewsSetProperties(visibility: Int) {
		hudViewBwe.visibility = visibility
		hudViewConnection.visibility = visibility
		hudViewVideoSend.visibility = visibility
		hudViewVideoRecv.visibility = visibility
		hudViewBwe.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5f)
		hudViewConnection.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5f)
		hudViewVideoSend.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5f)
		hudViewVideoRecv.setTextSize(TypedValue.COMPLEX_UNIT_PT, 5f)
	}

	private fun getReportMap(report: StatsReport): Map<String, String> {
		val reportMap: MutableMap<String, String> = HashMap()
		for (value in report.values) {
			reportMap[value.name] = value.value
		}
		return reportMap
	}

	fun updateEncoderStatistics(reports: Array<StatsReport>) {
		if (!isRunning || !displayHud) {
			return
		}
		val encoderStat = StringBuilder(128)
		val bweStat = StringBuilder()
		val connectionStat = StringBuilder()
		val videoSendStat = StringBuilder()
		val videoRecvStat = StringBuilder()
		var fps: String? = null
		var targetBitrate: String? = null
		var actualBitrate: String? = null
		for (report in reports) {
			if (report.type == "ssrc" && report.id.contains("ssrc") && report.id.contains("send")) {
				// Send video statistics.
				val reportMap = getReportMap(report)
				val trackId = reportMap["googTrackId"]
				if (trackId != null && trackId.contains(AppRTCConst.VIDEO_TRACK_ID)) {
					fps = reportMap["googFrameRateSent"]
					videoSendStat.append(report.id).append("\n")
					for (value in report.values) {
						val name = value.name.replace("goog", "")
						videoSendStat.append(name).append("=").append(value.value).append("\n")
					}
				}
			} else if (report.type == "ssrc" && report.id.contains("ssrc")
				&& report.id.contains("recv")) {
				// Receive video statistics.
				val reportMap = getReportMap(report)
				// Check if this stat is for video track.
				val frameWidth = reportMap["googFrameWidthReceived"]
				if (frameWidth != null) {
					videoRecvStat.append(report.id).append("\n")
					for (value in report.values) {
						val name = value.name.replace("goog", "")
						videoRecvStat.append(name).append("=").append(value.value).append("\n")
					}
				}
			} else if (report.id == "bweforvideo") {
				// BWE statistics.
				val reportMap = getReportMap(report)
				targetBitrate = reportMap["googTargetEncBitrate"]
				actualBitrate = reportMap["googActualEncBitrate"]
				bweStat.append(report.id).append("\n")
				for (value in report.values) {
					val name = value.name.replace("goog", "").replace("Available", "")
					bweStat.append(name).append("=").append(value.value).append("\n")
				}
			} else if (report.type == "googCandidatePair") {
				// Connection statistics.
				val reportMap = getReportMap(report)
				val activeConnection = reportMap["googActiveConnection"]
				if (activeConnection != null && activeConnection == "true") {
					connectionStat.append(report.id).append("\n")
					for (value in report.values) {
						val name = value.name.replace("goog", "")
						connectionStat.append(name).append("=").append(value.value).append("\n")
					}
				}
			}
		}
		hudViewBwe.text = bweStat.toString()
		hudViewConnection.text = connectionStat.toString()
		hudViewVideoSend.text = videoSendStat.toString()
		hudViewVideoRecv.text = videoRecvStat.toString()
		if (videoCallEnabled) {
			if (fps != null) {
				encoderStat.append("Fps:  ").append(fps).append("\n")
			}
			if (targetBitrate != null) {
				encoderStat.append("Target BR: ").append(targetBitrate).append("\n")
			}
			if (actualBitrate != null) {
				encoderStat.append("Actual BR: ").append(actualBitrate).append("\n")
			}
		}
		val cpu = cpuMonitor
		if (cpu != null) {
			encoderStat.append("CPU%: ")
				.append(cpu.cpuUsageCurrent)
				.append("/")
				.append(cpu.cpuUsageAverage)
				.append(". Freq: ")
				.append(cpu.frequencyScaleAverage)
		}
		encoderStatView.text = encoderStat.toString()
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = HudFragment::class.java.simpleName
	}
}
