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

import androidx.annotation.NonNull;

/**
 * Peer connection parameters.
 */
public class DataChannelParameters {
	/**
	 * DataChannelParameters生成のためのビルダークラス
	 */
	public static class Builder {
		private boolean ordered;
		private int maxRetransmitTimeMs;
		private int maxRetransmits;
		private String protocol;
		private boolean negotiated;
		private int id;

		/**
		 * デフォルトコンストラクタ
		 */
		public Builder() {
		}

		/**
		 * 既存のBuilderの内容を引き継いでBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		public Builder(@NonNull final Builder src) {
			ordered = src.ordered;
			maxRetransmitTimeMs = src.maxRetransmitTimeMs;
			maxRetransmits = src.maxRetransmits;
			protocol = src.protocol;
			negotiated = src.negotiated;
			id = src.id;
		}

		public void setOrdered(final boolean ordered) {
			this.ordered = ordered;
		}

		public void setMaxRetransmitTimeMs(final int maxRetransmitTimeMs) {
			this.maxRetransmitTimeMs = maxRetransmitTimeMs;
		}

		public void setMaxRetransmits(final int maxRetransmits) {
			this.maxRetransmits = maxRetransmits;
		}

		public void setProtocol(final String protocol) {
			this.protocol = protocol;
		}

		public void setNegotiated(final boolean negotiated) {
			this.negotiated = negotiated;
		}

		public void setId(final int id) {
			this.id = id;
		}

		/**
		 * DataChannelParametersを生成する
		 * @return
		 * @throws IllegalArgumentException
		 */
		public DataChannelParameters build() throws IllegalArgumentException {
			return new DataChannelParameters(
				ordered,
				maxRetransmitTimeMs, maxRetransmits,
				protocol, negotiated, id);
		}

		@NonNull
		@Override
		public String toString() {
			return "Builder{" +
				"ordered=" + ordered +
				", maxRetransmitTimeMs=" + maxRetransmitTimeMs +
				", maxRetransmits=" + maxRetransmits +
				", protocol='" + protocol + '\'' +
				", negotiated=" + negotiated +
				", id=" + id +
				'}';
		}
	}

	public final boolean ordered;
	public final int maxRetransmitTimeMs;
	public final int maxRetransmits;
	public final String protocol;
	public final boolean negotiated;
	public final int id;

	/**
	 * FIXME Builderを使うようにするためにprivate/protectedへ変更する
	 */
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
