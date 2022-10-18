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

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		if (DEBUG) Log.v(TAG, "onCreatePreferences:rootKey=$rootKey")
		// XMLリソースから設定画面を読み込む
		setPreferencesFromResource(R.xml.preferences, rootKey)
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = SettingsFragment::class.java.simpleName
	}
}
