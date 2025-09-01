package com.serenegiant.janusrtcandroid
/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.PreferenceFragmentCompat

/**
 * Settings fragment for AppRTC.
 */
class SettingsFragment : PreferenceFragmentCompat() {

	@Deprecated("Deprecated in Java")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (DEBUG) Log.v(TAG, "onCreate:")
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		optimizeEdgeToEdge(view);
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		if (DEBUG) Log.v(TAG, "onCreatePreferences:rootKey=$rootKey")
		// XMLリソースから設定画面を読み込む
		setPreferencesFromResource(R.xml.preferences, rootKey)
	}

	private fun optimizeEdgeToEdge(rootView: View) {
		ViewCompat.setOnApplyWindowInsetsListener(rootView) { root, windowInsets ->
			val insets = windowInsets.getInsets(
				// システムバー＝ステータスバー、ナビゲーションバー
				WindowInsetsCompat.Type.systemBars() or
					// ディスプレイカットアウト
					WindowInsetsCompat.Type.displayCutout(),
			)
			if (DEBUG) Log.v(TAG, "onApplyWindowInsets:insets=$insets,root=$root")
			root.updatePadding(
				top = insets.top,
				left = insets.left,
				right = insets.right,
				bottom = insets.bottom,
			)
			// このFragmentでWindowInsetsを消費する(子Viewには伝播しない)
			WindowInsetsCompat.CONSUMED
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SettingsFragment::class.java.simpleName
	}
}
