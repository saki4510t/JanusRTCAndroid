package com.serenegiant.janus
/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 - 2025 saki t_saki@serenegiant.com
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import com.serenegiant.janus.JanusClient.ListCallback
import com.serenegiant.janus.request.videoroom.ConfigPublisher
import com.serenegiant.janus.request.videoroom.ConfigSubscriber
import com.serenegiant.janus.response.videoroom.RoomInfo
import com.serenegiant.webrtc.RoomConnectionParameters

interface VideoRoomClient : JanusClient {
	/**
	 * request list of available room
	 */
	fun requestRoomList(callback: ListCallback<List<RoomInfo?>?>)

	/**
	 * Asynchronously connect to an Janus-gateway room URL using supplied connection
	 * parameters. Once connection is established onConnectedToRoom()
	 * callback with room parameters is invoked.
	 */
	fun connectToRoom(connectionParameters: RoomConnectionParameters)

	/**
	 * Disconnect from room.
	 */
	fun disconnectFromRoom()

	/**
	 * PublisherのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分のパブリッシャーのプラグインIDのはず
	 * @return
	 */
	val publishers: Collection<Long?>

	/**
	 * SubscriberのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分がサブスクライブしているリモートに対応するプラグインIDのはず
	 * @return
	 */
	val subscribers: Collection<Long?>

	/**
	 * 全てのPublisherを設定する
	 * @param config
	 * @return
	 */
	suspend fun configure(config: ConfigPublisher): Boolean

	/**
	 * 指定したプラグインIDが一致する最初のPublisherを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	suspend fun configure(pluginId: Long, config: ConfigPublisher): Boolean

	/**
	 * 全てのSubscriberを設定する
	 * @param config
	 * @return
	 */
	suspend fun configure(config: ConfigSubscriber): Boolean

	/**
	 * 指定したプラグインIDが一致する最初のSubscriberを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	suspend fun configure(pluginId: Long, config: ConfigSubscriber): Boolean
}
