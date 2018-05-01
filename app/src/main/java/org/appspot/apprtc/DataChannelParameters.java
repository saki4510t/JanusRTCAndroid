package org.appspot.apprtc;
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 by saki t_saki@serenegiant.com
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/**
 * Peer connection parameters.
 */
public class DataChannelParameters {
	public final boolean ordered;
	public final int maxRetransmitTimeMs;
	public final int maxRetransmits;
	public final String protocol;
	public final boolean negotiated;
	public final int id;
	
	public DataChannelParameters(final boolean ordered,
		final int maxRetransmitTimeMs, final int maxRetransmits,
		final String protocol, final boolean negotiated, final int id) {

		this.ordered = ordered;
		this.maxRetransmitTimeMs = maxRetransmitTimeMs;
		this.maxRetransmits = maxRetransmits;
		this.protocol = protocol;
		this.negotiated = negotiated;
		this.id = id;
	}
}
