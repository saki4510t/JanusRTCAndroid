package org.appspot.apprtc.util;
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 saki t_saki@serenegiant.com
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

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

import static org.appspot.apprtc.AppRTCConst.*;

public class SdpUtils {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = SdpUtils.class.getSimpleName();

	@SuppressWarnings("StringSplitter")
	public static String setStartBitrate(
		final String codec, final boolean isVideoCodec,
		final String sdpDescription, final int bitrateKbps) {

		final String[] lines = sdpDescription.split("\r\n");
		int rtpmapLineIndex = -1;
		boolean sdpFormatUpdated = false;
		String codecRtpMap = null;
		// Search for codec rtpmap in format
		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
		String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
		Pattern codecPattern = Pattern.compile(regex);
		for (int i = 0; i < lines.length; i++) {
			Matcher codecMatcher = codecPattern.matcher(lines[i]);
			if (codecMatcher.matches()) {
				codecRtpMap = codecMatcher.group(1);
				rtpmapLineIndex = i;
				break;
			}
		}
		if (codecRtpMap == null) {
			Log.w(TAG, "No rtpmap for " + codec + " codec");
			return sdpDescription;
		}
		if (DEBUG)
			Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex]);
		
		// Check if a=fmtp string already exist in remote SDP for this codec and
		// update it with new bitrate parameter.
		regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
		codecPattern = Pattern.compile(regex);
		for (int i = 0; i < lines.length; i++) {
			Matcher codecMatcher = codecPattern.matcher(lines[i]);
			if (codecMatcher.matches()) {
				if (DEBUG) Log.d(TAG, "Found " + codec + " " + lines[i]);
				if (isVideoCodec) {
					lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
				} else {
					lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
				}
				if (DEBUG) Log.d(TAG, "Update remote SDP line: " + lines[i]);
				sdpFormatUpdated = true;
				break;
			}
		}
		
		final StringBuilder newSdpDescription = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			newSdpDescription.append(lines[i]).append("\r\n");
			// Append new a=fmtp line if no such line exist for a codec.
			if (!sdpFormatUpdated && i == rtpmapLineIndex) {
				String bitrateSet;
				if (isVideoCodec) {
					bitrateSet =
						"a=fmtp:" + codecRtpMap + " " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + bitrateKbps;
				} else {
					bitrateSet = "a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
						+ (bitrateKbps * 1000);
				}
				if (DEBUG) Log.d(TAG, "Add remote SDP line: " + bitrateSet);
				newSdpDescription.append(bitrateSet).append("\r\n");
			}
		}
		return newSdpDescription.toString();
	}
	
	/**
	 * Returns the line number containing "m=audio|video", or -1 if no such line exists.
	 */
	private static int findMediaDescriptionLine(
		final boolean isAudio, final String[] sdpLines) {

		final String mediaDescription = isAudio ? "m=audio " : "m=video ";
		for (int i = 0; i < sdpLines.length; ++i) {
			if (sdpLines[i].startsWith(mediaDescription)) {
				return i;
			}
		}
		return -1;
	}
	
	private static String joinString(
		final Iterable<? extends CharSequence> s,
		final String delimiter, final boolean delimiterAtEnd) {

		Iterator<? extends CharSequence> iter = s.iterator();
		if (!iter.hasNext()) {
			return "";
		}
		StringBuilder buffer = new StringBuilder(iter.next());
		while (iter.hasNext()) {
			buffer.append(delimiter).append(iter.next());
		}
		if (delimiterAtEnd) {
			buffer.append(delimiter);
		}
		return buffer.toString();
	}
	
	private static @Nullable
	String movePayloadTypesToFront(
		final List<String> preferredPayloadTypes, final String mLine) {

		// The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
		final List<String> origLineParts = Arrays.asList(mLine.split(" "));
		if (origLineParts.size() <= 3) {
			Log.e(TAG, "Wrong SDP media description format: " + mLine);
			return null;
		}
		final List<String> header = origLineParts.subList(0, 3);
		final List<String> unpreferredPayloadTypes =
			new ArrayList<>(origLineParts.subList(3, origLineParts.size()));
		unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
		// Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
		// types.
		final List<String> newLineParts = new ArrayList<>();
		newLineParts.addAll(header);
		newLineParts.addAll(preferredPayloadTypes);
		newLineParts.addAll(unpreferredPayloadTypes);
		return joinString(newLineParts, " ", false /* delimiterAtEnd */);
	}
	
	public static String preferCodec(
		final String sdpDescription, final String codec, final boolean isAudio) {

		final String[] lines = sdpDescription.split("\r\n");
		final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
		if (mLineIndex == -1) {
			Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
			return sdpDescription;
		}
		// A list with all the payload types with name |codec|. The payload types are integers in the
		// range 96-127, but they are stored as strings here.
		final List<String> codecPayloadTypes = new ArrayList<>();
		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
		final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
		for (String line : lines) {
			Matcher codecMatcher = codecPattern.matcher(line);
			if (codecMatcher.matches()) {
				codecPayloadTypes.add(codecMatcher.group(1));
			}
		}
		if (codecPayloadTypes.isEmpty()) {
			Log.w(TAG, "No payload types with name " + codec);
			return sdpDescription;
		}
		
		final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
		if (newMLine == null) {
			return sdpDescription;
		}
		if (DEBUG)
			Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
		lines[mLineIndex] = newMLine;
		return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
	}
}
