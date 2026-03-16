package com.serenegiant.webrtc.util
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2026 saki t_saki@serenegiant.com
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

import android.util.Log
import com.serenegiant.webrtc.AppRTCConst
import java.util.Arrays
import java.util.regex.Pattern

object SdpUtils {
	private const val DEBUG = false // set false on production
	private val TAG: String = SdpUtils::class.java.simpleName

	@JvmStatic
	fun setStartBitrate(
		codec: String, isVideoCodec: Boolean,
		sdpDescription: String, bitrateKbps: Int
	): String {
		val lines = sdpDescription.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		var rtpmapLineIndex = -1
		var sdpFormatUpdated = false
		var codecRtpMap: String? = null
		// Search for codec rtpmap in format
		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
		var regex = "^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$"
		var codecPattern = Pattern.compile(regex)
		for (i in lines.indices) {
			val codecMatcher = codecPattern.matcher(lines[i])
			if (codecMatcher.matches()) {
				codecRtpMap = codecMatcher.group(1)
				rtpmapLineIndex = i
				break
			}
		}
		if (codecRtpMap == null) {
			Log.w(TAG, "No rtpmap for $codec codec")
			return sdpDescription
		}
		if (DEBUG) Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex])


		// Check if a=fmtp string already exist in remote SDP for this codec and
		// update it with new bitrate parameter.
		regex = "^a=fmtp:$codecRtpMap \\w+=\\d+.*[\r]?$"
		codecPattern = Pattern.compile(regex)
		for (i in lines.indices) {
			val codecMatcher = codecPattern.matcher(lines[i])
			if (codecMatcher.matches()) {
				if (DEBUG) Log.d(TAG, "Found " + codec + " " + lines[i])
				if (isVideoCodec) {
					lines[i] += "; " + AppRTCConst.VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps
				} else {
					lines[i] += "; " + AppRTCConst.AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000)
				}
				if (DEBUG) Log.d(TAG, "Update remote SDP line: " + lines[i])
				sdpFormatUpdated = true
				break
			}
		}

		val newSdpDescription = StringBuilder()
		for (i in lines.indices) {
			newSdpDescription.append(lines[i]).append("\r\n")
			// Append new a=fmtp line if no such line exist for a codec.
			if (!sdpFormatUpdated && i == rtpmapLineIndex) {
				val bitrateSet = if (isVideoCodec) {
					"a=fmtp:" + codecRtpMap + " " + AppRTCConst.VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps
				} else {
					("a=fmtp:" + codecRtpMap + " " + AppRTCConst.AUDIO_CODEC_PARAM_BITRATE + "="
						+ (bitrateKbps * 1000))
				}
				if (DEBUG) Log.d(TAG, "Add remote SDP line: $bitrateSet")
				newSdpDescription.append(bitrateSet).append("\r\n")
			}
		}
		return newSdpDescription.toString()
	}

	/**
	 * Returns the line number containing "m=audio|video", or -1 if no such line exists.
	 */
	private fun findMediaDescriptionLine(
		isAudio: Boolean, sdpLines: Array<String>
	): Int {
		val mediaDescription = if (isAudio) "m=audio " else "m=video "
		for (i in sdpLines.indices) {
			if (sdpLines[i].startsWith(mediaDescription)) {
				return i
			}
		}
		return -1
	}

	private fun joinString(
		s: Iterable<CharSequence?>,
		delimiter: String, delimiterAtEnd: Boolean
	): String {
		val iter = s.iterator()
		if (!iter.hasNext()) {
			return ""
		}
		val buffer = StringBuilder(iter.next())
		while (iter.hasNext()) {
			buffer.append(delimiter).append(iter.next())
		}
		if (delimiterAtEnd) {
			buffer.append(delimiter)
		}
		return buffer.toString()
	}

	private fun movePayloadTypesToFront(
		preferredPayloadTypes: List<String?>, mLine: String
	): String? {
		// The format of the media description line should be: m=<media> <port> <proto> <fmt> ...

		val origLineParts =
			Arrays.asList(*mLine.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
		if (origLineParts.size <= 3) {
			Log.e(TAG, "Wrong SDP media description format: $mLine")
			return null
		}
		val header: List<String?> = origLineParts.subList(0, 3)
		val unpreferredPayloadTypes: MutableList<String?> =
			ArrayList(origLineParts.subList(3, origLineParts.size))
		unpreferredPayloadTypes.removeAll(preferredPayloadTypes)
		// Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
		// types.
		val newLineParts: MutableList<String?> = ArrayList()
		newLineParts.addAll(header)
		newLineParts.addAll(preferredPayloadTypes)
		newLineParts.addAll(unpreferredPayloadTypes)
		return joinString(newLineParts, " ", false /* delimiterAtEnd */)
	}

	@JvmStatic
	fun preferCodec(
		sdpDescription: String, codec: String, isAudio: Boolean
	): String {
		val lines =
			sdpDescription.split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val mLineIndex = findMediaDescriptionLine(isAudio, lines)
		if (mLineIndex == -1) {
			Log.w(TAG, "No mediaDescription line, so can't prefer $codec")
			return sdpDescription
		}
		// A list with all the payload types with name |codec|. The payload types are integers in the
		// range 96-127, but they are stored as strings here.
		val codecPayloadTypes: MutableList<String?> = ArrayList()
		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
		val codecPattern = Pattern.compile("^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$")
		for (line in lines) {
			val codecMatcher = codecPattern.matcher(line)
			if (codecMatcher.matches()) {
				codecPayloadTypes.add(codecMatcher.group(1))
			}
		}
		if (codecPayloadTypes.isEmpty()) {
			Log.w(TAG, "No payload types with name $codec")
			return sdpDescription
		}

		val newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex])
			?: return sdpDescription
		if (DEBUG) Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine)
		lines[mLineIndex] = newMLine
		return joinString(Arrays.asList(*lines), "\r\n", true /* delimiterAtEnd */)
	}
}
