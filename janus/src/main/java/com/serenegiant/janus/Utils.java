package com.serenegiant.janus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Utils {
	private Utils() {
		// インスタンス化をエラーとするためにデフォルトコンストラクタをprivateにする
	}

	/**
	 * Executor thread is started once in private ctor and is used for all
	 * peer connection API calls to ensure new peer connection factory is
	 * created on the same thread as previously destroyed factory.
	 */
	static final ExecutorService executor = Executors.newSingleThreadExecutor();

}
