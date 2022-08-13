package com.serenegiant.janus;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * manage relation ship between request and response over network connection
 */
public class TransactionManager {
	/**
	 * helper class to generate random strings for transaction id
	 */
	private static class RandomString {
		final String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final Random rnd = new Random();

		/**
		 * generate random string
		 * @param length length of random string
		 * @return
		 */
		@NonNull
		public String get(final int length) {
			final StringBuilder sb = new StringBuilder(length);
			for (int i = 0; i < length; i++) {
				sb.append(str.charAt(rnd.nextInt(str.length())));
			}
			return sb.toString();
		}
	}

	/**
	 * statically hold RandomString instance to generate random strings for transaction id
	 */
	@NonNull
	private static final RandomString mRandomString = new RandomString();
	
	/**
	 * hold transaction id - TransactionCallback pair(s)
	 */
	@NonNull
	private static final Map<String, TransactionCallback>
		sTransactions = new HashMap<>();
	
	/**
	 * get transaction and assign it to specific callback
	 * @param length
	 * @param callback
	 * @return
	 */
	@NonNull
	public static String get(
		final int length, @Nullable final TransactionCallback callback) {

		final String transaction = mRandomString.get(length);
		if (callback != null) {
			synchronized (sTransactions) {
				sTransactions.put(transaction, callback);
			}
		}
		return transaction;
	}
	
	/**
	 * callback listener when app receives transaction message
	 */
	public interface TransactionCallback {
		/**
		 * usually this is called from from long poll
		 * @param transaction
		 * @param body
		 * @return true: handled, if return true, assignment will be removed.
		 */
		public boolean onReceived(
			@NonNull final String transaction,
			@NonNull final JSONObject body);
	}
	
	/**
	 * call callback related to the specific transaction
	 * @param transaction
	 * @param body
	 * @return true: handled
	 */
	public static boolean handleTransaction(
		@NonNull final String transaction,
		@NonNull final JSONObject body) {
		
		TransactionCallback callback = null;
		final boolean result;
		synchronized (sTransactions) {
			if (sTransactions.containsKey(transaction)) {
				callback = sTransactions.get(transaction);
			}
			result = callback != null && callback.onReceived(transaction, body);
		}
		return result;
	}

	/**
	 * remove specific transaction
 	 * @param transaction
	 */
	public static void removeTransaction(@NonNull final String transaction) {
		synchronized (sTransactions) {
			sTransactions.remove(transaction);
		}
	}

	/**
	 * clear transaction - callback mapping
	 */
	public static void clearTransactions() {
		synchronized (sTransactions) {
			sTransactions.clear();
		}
	}
}
