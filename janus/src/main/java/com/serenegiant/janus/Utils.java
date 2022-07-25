package com.serenegiant.janus;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.serenegiant.janus.Const.HTTP_CONNECT_TIMEOUT_MS;

class Utils {
	private static final boolean DEBUG = false;
	private static final String TAG = Utils.class.getSimpleName();

	public interface BuilderCallback {
		@NonNull
		public OkHttpClient.Builder setupOkHttp(@NonNull final OkHttpClient.Builder builder,
			final boolean isLongPoll,
			final long connectionTimeout, final long readTimeoutMs, final long writeTimeoutMs);

		@NonNull
		public Retrofit.Builder setupRetrofit(@NonNull final Retrofit.Builder builder);
	}

	private Utils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * Executor thread is started once in private ctor and is used for all
	 * peer connection API calls to ensure new peer connection factory is
	 * created on the same thread as previously destroyed factory.
	 */
	static final ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * keep first OkHttpClient as singleton
	 */
	private static OkHttpClient sOkHttpClient;
	/**
	 * Janus-gatewayサーバーとの通信用のOkHttpClientインスタンスの初期化処理
	 * @return
	 */
	public static synchronized OkHttpClient setupHttpClient(
		final boolean isLongPoll,
		final long readTimeoutMs, final long writeTimeoutMs,
		@NonNull final BuilderCallback callback) {

		if (DEBUG) Log.v(TAG, "setupHttpClient:");

		OkHttpClient.Builder builder;
		if (sOkHttpClient == null) {
		 	builder = new OkHttpClient.Builder();
		} else {
			builder = sOkHttpClient.newBuilder();
		}
		builder
			.connectTimeout(HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)	// 接続タイムアウト
			.readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)		// 読み込みタイムアウト
			.writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS);	// 書き込みタイムアウト
		builder = callback.setupOkHttp(builder, isLongPoll,
			HTTP_CONNECT_TIMEOUT_MS, readTimeoutMs, writeTimeoutMs);
		final List<Interceptor> interceptors = builder.interceptors();
		builder
			.addInterceptor(new Interceptor() {
				@NonNull
				@Override
				public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {

					final Request original = chain.request();
					// header設定
					final Request request = original.newBuilder()
						.header("Accept", "application/json")
						.method(original.method(), original.body())
						.build();

					okhttp3.Response response = chain.proceed(request);
					return response;
				}
			});
		// ログ出力設定
		if (DEBUG) {
			boolean hasLogging = false;
			for (final Interceptor interceptor: interceptors) {
				if (interceptor instanceof HttpLoggingInterceptor) {
					hasLogging = true;
					break;
				}
			}
			if (!hasLogging) {
				final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
				logging.setLevel(HttpLoggingInterceptor.Level.BODY);
				builder.addInterceptor(logging);
			}
		}

		final OkHttpClient result = builder.build();
		if (sOkHttpClient == null) {
			sOkHttpClient = result;
		}
		return result;
	}

	/**
	 * Janus-gatewayサーバーとの通信用のRetrofitインスタンスの初期化処理
	 * @param client
	 * @param baseUrl
	 * @return
	 */
	public static Retrofit setupRetrofit(
		@NonNull final OkHttpClient client,
		@NonNull final String baseUrl,
		@NonNull final BuilderCallback callback) {

		if (DEBUG) Log.v(TAG, "setupRetrofit:" + baseUrl);
		// JSONのパーサーとしてGsonを使う
		final Gson gson = new GsonBuilder()
//			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)	// IDENTITY
			.registerTypeAdapter(Date.class, new DateTypeAdapter())
			.create();
		return callback.setupRetrofit(
			new Retrofit.Builder()
				.baseUrl(baseUrl)
				.addConverterFactory(GsonConverterFactory.create(gson))
				.client(client)
			).build();
	}

}
