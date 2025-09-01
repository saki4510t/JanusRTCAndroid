package com.serenegiant.janusrtcandroid
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2022 saki t_saki@serenegiant.com
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.serenegiant.dialog.PermissionDescriptionDialogV4
import com.serenegiant.dialog.RationalDialogV4
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils

abstract class BaseActivity : AppCompatActivity(),
	PermissionDescriptionDialogV4.DialogResultListener {

	private lateinit var mMissingPermissions: Array<String>
	private lateinit var mPermissions: PermissionUtils

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		try {
			val missingPermissions = PermissionUtils.missingPermissions(this)
			mMissingPermissions = missingPermissions.toTypedArray<String>()
		} catch (e: PackageManager.NameNotFoundException) {
			Log.w(TAG, e)
			mMissingPermissions = arrayOf("")
		}
		mPermissions = PermissionUtils(this, mPermissionCallback)
			.prepare(this, mMissingPermissions)
	}

	/**
	 * MessageDialogFragmentメッセージダイアログからのコールバックリスナー
	 *
	 * @param dialog
	 * @param requestCode
	 * @param permissions
	 * @param result
	 */
	@SuppressLint("NewApi")
	override fun onDialogResult(
		dialog: PermissionDescriptionDialogV4,
		requestCode: Int, permissions: Array<String>, result: Boolean) {
		if (DEBUG) Log.v(TAG, "onDialogResult:result=$result,permissions=$permissions")
		if (result) {
			// メッセージダイアログでOKを押された時はパーミッション要求する
			if (BuildCheck.isMarshmallow()) {
				requestPermissions(permissions, requestCode)
				return
			}
		}
		// メッセージダイアログでキャンセルされた時とAndroid6でない時は自前でチェックして#checkPermissionResultを呼び出す
		for (permission in permissions) {
			checkPermissionResult(
				requestCode, permission,
				PermissionUtils.hasPermission(this, permission)
			)
		}
	}

	/**
	 * パーミッション要求結果を受け取るためのメソッド
	 *
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults) // 何もしてないけど一応呼んどく
		val n = Math.min(permissions.size, grantResults.size)
		for (i in 0 until n) {
			checkPermissionResult(
				requestCode, permissions[i],
				grantResults[i] == PackageManager.PERMISSION_GRANTED
			)
		}
	}

	/**
	 * パーミッション要求の結果をチェック
	 * ここではパーミッションを取得できなかった時にToastでメッセージ表示するだけ
	 *
	 * @param requestCode
	 * @param permission
	 * @param result
	 */
	private fun checkPermissionResult(
		requestCode: Int, permission: String?, result: Boolean) {
		// パーミッションがないときにはメッセージを表示する
		if (!result && permission != null) {
			if (Manifest.permission.RECORD_AUDIO == permission) {
				Toast.makeText(
					applicationContext,
					R.string.permission_audio, Toast.LENGTH_SHORT
				).show()
			}
			if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission) {
				Toast.makeText(
					applicationContext,
					R.string.permission_ext_storage, Toast.LENGTH_SHORT
				).show()
			}
			if (Manifest.permission.CAMERA == permission) {
				Toast.makeText(
					applicationContext,
					R.string.permission_camera, Toast.LENGTH_SHORT
				).show()
			}
			if (Manifest.permission.INTERNET == permission) {
				Toast.makeText(
					applicationContext,
					R.string.permission_network, Toast.LENGTH_SHORT
				).show()
			}
			if (Manifest.permission.BLUETOOTH == permission
				|| Manifest.permission.BLUETOOTH_CONNECT == permission
			) {
				Toast.makeText(
					applicationContext,
					R.string.permission_bluetooth, Toast.LENGTH_SHORT
				).show()
			}
			if (Manifest.permission.ACCESS_COARSE_LOCATION == permission
				|| Manifest.permission.ACCESS_FINE_LOCATION == permission
			) {
				Toast.makeText(
					applicationContext,
					R.string.permission_location, Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	/**
	 * アプリの実行に必要なパーミッションを一括して要求する
	 * @return
	 */
	protected fun checkPermissions(): Boolean {
		if (DEBUG) Log.v(TAG, "checkPermissions:missingPermissions=${mMissingPermissions.contentToString()}")
		return mPermissions.requestPermission(mMissingPermissions, false)
	}

	private val mPermissionCallback = object : PermissionUtils.PermissionCallback {
		override fun onPermissionShowRational(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:$permission")
			val dialog = RationalDialogV4.showDialog(this@BaseActivity, permission)
			if (dialog == null) {
				if (DEBUG) Log.v(TAG, "onPermissionShowRational:デフォルトのダイアログ表示ができなかったので自前で表示しないといけない,$permission")
				// FIXME 未実装
			}
		}

		override fun onPermissionShowRational(permissions: Array<String>) {
			if (DEBUG) Log.v(TAG, "onPermissionShowRational:" + permissions.contentToString())
			// FIXME 未実装
		}

		override fun onPermissionDenied(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionDenied:$permission")
			// ユーザーがパーミッション要求を拒否したときの処理
			// FIXME 未実装
		}

		override fun onPermission(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermission:$permission")
			// ユーザーがパーミッション要求を承認したときの処理
		}

		override fun onPermissionNeverAskAgain(permission: String) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:$permission")
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			// FIXME 未実装
		}

		override fun onPermissionNeverAskAgain(permissions: Array<String>) {
			if (DEBUG) Log.v(TAG, "onPermissionNeverAskAgain:" + permissions.contentToString())
			// 端末のアプリ設定画面を開くためのボタンを配置した画面へ遷移させる
			// FIXME 未実装
		}
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = BaseActivity::class.java.simpleName
	}
}
