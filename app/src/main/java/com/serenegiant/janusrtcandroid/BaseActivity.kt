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
import androidx.appcompat.app.AppCompatActivity
import com.serenegiant.dialog.PermissionDescriptionDialogV4
import android.annotation.SuppressLint
import com.serenegiant.system.BuildCheck
import com.serenegiant.system.PermissionUtils
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast

abstract class BaseActivity : AppCompatActivity(),
	PermissionDescriptionDialogV4.DialogResultListener {

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
		}
	}

	/**
	 * 外部ストレージへの書き込みパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 *
	 * @return true 外部ストレージへの書き込みパーミッションが有る
	 */
	protected fun checkPermissionWriteExternalStorage(): Boolean {
		if (!PermissionUtils.hasWriteExternalStorage(this)) {
			PermissionDescriptionDialogV4.showDialog(
				this,
				REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_EXT_STORAGE,
				arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
			)
			return false
		}
		return true
	}

	/**
	 * 録音のパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 *
	 * @return true 録音のパーミッションが有る
	 */
	protected fun checkPermissionAudio(): Boolean {
		if (!PermissionUtils.hasAudio(this)) {
			PermissionDescriptionDialogV4.showDialog(
				this,
				REQUEST_PERMISSION_AUDIO_RECORDING,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_AUDIO,
				arrayOf(Manifest.permission.RECORD_AUDIO)
			)
			return false
		}
		return true
	}

	/**
	 * カメラアクセスのパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 *
	 * @return true カメラアクセスのパーミッションがある
	 */
	protected fun checkPermissionCamera(): Boolean {
		if (!PermissionUtils.hasCamera(this)) {
			PermissionDescriptionDialogV4.showDialog(
				this,
				REQUEST_PERMISSION_CAMERA,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_CAMERA,
				arrayOf(Manifest.permission.CAMERA)
			)
			return false
		}
		return true
	}

	/**
	 * ネットワークアクセスのパーミッションが有るかどうかをチェック
	 * なければ説明ダイアログを表示する
	 *
	 * @return true ネットワークアクセスのパーミッションが有る
	 */
	protected fun checkPermissionNetwork(): Boolean {
		if (!PermissionUtils.hasNetwork(this)) {
			PermissionDescriptionDialogV4.showDialog(
				this,
				REQUEST_PERMISSION_NETWORK,
				R.string.permission_title,
				ID_PERMISSION_REQUEST_NETWORK,
				arrayOf(Manifest.permission.INTERNET)
			)
			return false
		}
		return true
	}

	companion object {
		private const val DEBUG = true // set false on production
		private val TAG = BaseActivity::class.java.simpleName

		private val ID_PERMISSION_REQUEST_AUDIO = R.string.permission_audio_request
		private val ID_PERMISSION_REQUEST_NETWORK = R.string.permission_network_request
		private val ID_PERMISSION_REQUEST_EXT_STORAGE = R.string.permission_ext_storage_request
		private val ID_PERMISSION_REQUEST_CAMERA = R.string.permission_camera_request

		protected const val REQUEST_PERMISSION_AUDIO_RECORDING = 0x234567
		protected const val REQUEST_PERMISSION_NETWORK = 0x456789
		protected const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 0x12345
		protected const val REQUEST_PERMISSION_CAMERA = 0x345678
	}
}
