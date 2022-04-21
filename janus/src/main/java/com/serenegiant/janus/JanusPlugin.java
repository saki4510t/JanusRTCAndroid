package com.serenegiant.janus;

import com.serenegiant.janus.response.PluginInfo;
import com.serenegiant.janus.response.Session;

import java.math.BigInteger;

import androidx.annotation.NonNull;

/*package*/ abstract class JanusPlugin {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = JanusPlugin.class.getSimpleName();

	public interface PluginCallback {
		/**
		 * callback when attached to plugin
		 * @param plugin
		 */
		public void onAttach(@NonNull final JanusPlugin plugin);
		/**
		 * callback when detached from plugin
		 * @param plugin
		 */
		public void onDetach(@NonNull final JanusPlugin plugin);
	}

	@NonNull
	protected final Session mSession;
	protected PluginInfo mPlugin;

	protected JanusPlugin(@NonNull final Session session) {
		mSession = session;
	}

	/**
	 * セッションIDを取得
	 * @return
	 */
	BigInteger sessionId() {
		return mSession.id();
	}

	/**
	 * プラグインIDを取得
	 * @return
	 */
	BigInteger id() {
		return mPlugin != null ? mPlugin.id() : null;
	}

	/**
	 * プラグインへ接続
	 */
	public abstract void attach();

	/**
	 * プラグインから切断
	 */
	public abstract void detach();
}
