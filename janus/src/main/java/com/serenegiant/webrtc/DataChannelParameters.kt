package com.serenegiant.webrtc
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
class DataChannelParameters(
	@JvmField val ordered: Boolean,
	@JvmField val maxRetransmitTimeMs: Int,
	@JvmField val maxRetransmits: Int,
	@JvmField val protocol: String?,
	@JvmField val negotiated: Boolean,
	@JvmField val id: Int
) {
	/**
	 * DataChannelParameters生成のためのビルダークラス
	 */
	class Builder {
		private var ordered = false
		private var maxRetransmitTimeMs = 0
		private var maxRetransmits = 0
		private var protocol: String? = null
		private var negotiated = false
		private var id = 0

		/**
		 * デフォルトコンストラクタ
		 */
		constructor()

		/**
		 * 既存のBuilderの内容を引き継いでBuilderを生成するためのコピーコンストラクタ
		 * @param src
		 */
		constructor(src: Builder) {
			ordered = src.ordered
			maxRetransmitTimeMs = src.maxRetransmitTimeMs
			maxRetransmits = src.maxRetransmits
			protocol = src.protocol
			negotiated = src.negotiated
			id = src.id
		}

		fun setOrdered(ordered: Boolean) {
			this.ordered = ordered
		}

		fun setMaxRetransmitTimeMs(maxRetransmitTimeMs: Int) {
			this.maxRetransmitTimeMs = maxRetransmitTimeMs
		}

		fun setMaxRetransmits(maxRetransmits: Int) {
			this.maxRetransmits = maxRetransmits
		}

		fun setProtocol(protocol: String?) {
			this.protocol = protocol
		}

		fun setNegotiated(negotiated: Boolean) {
			this.negotiated = negotiated
		}

		fun setId(id: Int) {
			this.id = id
		}

		/**
		 * DataChannelParametersを生成する
		 * @return
		 * @throws IllegalArgumentException
		 */
		@Throws(IllegalArgumentException::class)
		fun build(): DataChannelParameters {
			return DataChannelParameters(
				ordered,
				maxRetransmitTimeMs, maxRetransmits,
				protocol, negotiated, id
			)
		}

		override fun toString(): String {
			return "Builder{" +
				"ordered=" + ordered +
				", maxRetransmitTimeMs=" + maxRetransmitTimeMs +
				", maxRetransmits=" + maxRetransmits +
				", protocol='" + protocol + '\'' +
				", negotiated=" + negotiated +
				", id=" + id +
				'}'
		}
	}
}
