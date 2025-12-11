package com.serenegiant.janus
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

import org.json.JSONObject
import java.util.Random

/**
 * manage relation ship between request and response over network connection
 */
object TransactionManager {
	/**
	 * statically hold RandomString instance to generate random strings for transaction id
	 */
	private val mRandomString = RandomString()

	/**
	 * hold transaction id - TransactionCallback pair(s)
	 */
	private val sTransactions = mutableMapOf<String, TransactionCallback>()

	/**
	 * get transaction and assign it to specific callback
	 * @param length
	 * @param callback
	 * @return
	 */
	fun get(length: Int, callback: TransactionCallback?): String {
		val transaction = mRandomString.get(length)
		if (callback != null) {
			synchronized(sTransactions) {
				sTransactions.put(transaction, callback)
			}
		}
		return transaction
	}

	/**
	 * call callback related to the specific transaction
	 * @param transaction
	 * @param body
	 * @return true: handled
	 */
	@JvmStatic
	fun handleTransaction(transaction: String, body: JSONObject): Boolean {
		var callback: TransactionCallback? = null
		val result: Boolean
		synchronized(sTransactions) {
			if (sTransactions.containsKey(transaction)) {
				callback = sTransactions[transaction]
			}
			result = callback != null && callback!!.onReceived(transaction, body)
		}
		return result
	}

	/**
	 * remove specific transaction
	 * @param transaction
	 */
	@JvmStatic
	fun removeTransaction(transaction: String) {
		synchronized(sTransactions) {
			sTransactions.remove(transaction)
		}
	}

	/**
	 * clear transaction - callback mapping
	 */
	@JvmStatic
	fun clearTransactions() {
		synchronized(sTransactions) {
			sTransactions.clear()
		}
	}

	/**
	 * helper class to generate random strings for transaction id
	 */
	private class RandomString {
		val rnd: Random = Random()

		/**
		 * generate random string
		 * @param length length of random string
		 * @return
		 */
		fun get(length: Int): String {
			val sb = StringBuilder(length)
			for (i in 0..<length) {
				sb.append(STR[rnd.nextInt(STR.length)])
			}
			return sb.toString()
		}

		companion object {
			private const val STR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
		}
	}

	/**
	 * callback listener when app receives transaction message
	 */
	interface TransactionCallback {
		/**
		 * usually this is called from from long poll
		 * @param transaction
		 * @param body
		 * @return true: handled, if return true, assignment will be removed.
		 */
		fun onReceived(transaction: String, body: JSONObject): Boolean
	}
}
