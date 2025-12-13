package com.serenegiant.janus
/*
 * JanusRTCAndroid
 * Video chat sample app using videoroom plugin on janus-gateway server and WebRTC.
 *
 * Copyright (c) 2018 - 2025 saki t_saki@serenegiant.com
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

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.internal.bind.DateTypeAdapter
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

internal object Utils {
	private const val DEBUG = false
	private val TAG: String = Utils::class.java.simpleName

	/**
	 * default implementation of BuilderCallback
	 * do nothing additional
	 */
	val DEFAULT_BUILDER_CALLBACK = object : BuilderCallback {
		override fun setupOkHttp(
			builder: OkHttpClient.Builder,
			isLongPoll: Boolean,
			connectionTimeout: Long,
			readTimeoutMs: Long, writeTimeoutMs: Long
		): OkHttpClient.Builder {
			if (DEBUG) Log.v(TAG, "setupOkHttp:")
			return builder
		}

		override fun setupRetrofit(builder: Retrofit.Builder): Retrofit.Builder {
			if (DEBUG) Log.v(TAG, "setupRetrofit:")
			return builder
		}
	}

	/**
	 * Executor thread is started once in private ctor and is used for all
	 * peer connection API calls to ensure new peer connection factory is
	 * created on the same thread as previously destroyed factory.
	 */
	@JvmField
	val executor = Executors.newSingleThreadExecutor()

	/**
	 * keep first OkHttpClient as singleton
	 */
	private var sOkHttpClient: OkHttpClient? = null
	private val sLock = ReentrantLock()
	/**
	 * Janus-gatewayサーバーとの通信用のOkHttpClientインスタンスの初期化処理
	 * @return
	 */
	fun setupHttpClient(
		isLongPoll: Boolean,
		readTimeoutMs: Long, writeTimeoutMs: Long,
		callback: BuilderCallback
	): OkHttpClient {
		if (DEBUG) Log.v(TAG, "setupHttpClient:")
		sLock.withLock {
			var builder = if (sOkHttpClient == null) {
				OkHttpClient.Builder()
			} else {
				sOkHttpClient!!.newBuilder()
			}
			builder
				.connectTimeout(Const.HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS) // 接続タイムアウト
				.readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS) // 読み込みタイムアウト
				.writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS) // 書き込みタイムアウト
			builder = callback.setupOkHttp(
				builder, isLongPoll,
				Const.HTTP_CONNECT_TIMEOUT_MS, readTimeoutMs, writeTimeoutMs
			)
			val interceptors = builder.interceptors()
			builder.addInterceptor(Interceptor { chain ->
				val original = chain.request()
				// header設定
				val request = original.newBuilder()
					.header("Accept", "application/json")
					.method(original.method, original.body)
					.build()

				val response = chain.proceed(request)
				response
			})
			// ログ出力設定
			if (DEBUG) {
				var hasLogging = false
				for (interceptor in interceptors) {
					if (interceptor is HttpLoggingInterceptor) {
						hasLogging = true
						break
					}
				}
				if (!hasLogging) {
					val logging = HttpLoggingInterceptor()
					logging.setLevel(HttpLoggingInterceptor.Level.BODY)
					builder.addInterceptor(logging)
				}
			}

			val result = builder.build()
			if (sOkHttpClient == null) {
				sOkHttpClient = result
			}
			return result
		}
	}

	/**
	 * Janus-gatewayサーバーとの通信用のRetrofitインスタンスの初期化処理
	 * @param client
	 * @param baseUrl
	 * @return
	 */
	fun setupRetrofit(
		client: OkHttpClient,
		baseUrl: String,
		callback: BuilderCallback
	): Retrofit {
		if (DEBUG) Log.v(TAG, "setupRetrofit:$baseUrl")
		// JSONのパーサーとしてGsonを使う
		val gson = GsonBuilder()
//			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)	// IDENTITY
			.registerTypeAdapter(Date::class.java, DateTypeAdapter())
			.create()
		return callback.setupRetrofit(
			Retrofit.Builder()
				.baseUrl(baseUrl)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.client(client)
		).build()
	}
}
