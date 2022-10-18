package com.serenegiant.janus;
/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 - 2022 by saki t_saki@serenegiant.com
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
*/

import com.serenegiant.janus.request.videoroom.ConfigPublisher;
import com.serenegiant.janus.request.videoroom.ConfigSubscriber;
import com.serenegiant.janus.response.videoroom.RoomInfo;

import org.appspot.apprtc.RoomConnectionParameters;

import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;

public interface VideoRoomClient extends JanusClient {
	/**
	 * request list of available room
	 */
	public void requestRoomList(@NonNull final ListCallback<List<RoomInfo>> callback);

	/**
	 * Asynchronously connect to an Janus-gateway room URL using supplied connection
	 * parameters. Once connection is established onConnectedToRoom()
	 * callback with room parameters is invoked.
	 */
	public void connectToRoom(@NonNull final RoomConnectionParameters connectionParameters);

	/**
	 * Disconnect from room.
	 */
	public void disconnectFromRoom();

	/**
	 * PublisherのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分のパブリッシャーのプラグインIDのはず
	 * @return
	 */
	@NonNull
	public Collection<Long> getPublishers();

	/**
	 * SubscriberのプラグインID一覧を取得
	 * 基本的にこれに入っているのは自分がサブスクライブしているリモートに対応するプラグインIDのはず
	 * @return
	 */
	@NonNull
	public Collection<Long> getSubscribers();

	/**
	 * 全てのPublisherを設定する
	 * @param config
	 * @return
	 */
	public boolean configure(@NonNull final ConfigPublisher config);

	/**
	 * 指定したプラグインIDが一致する最初のPublisherを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	public boolean configure(final long pluginId, @NonNull final ConfigPublisher config);

	/**
	 * 全てのSubscriberを設定する
	 * @param config
	 * @return
	 */
	public boolean configure(@NonNull final ConfigSubscriber config);

	/**
	 * 指定したプラグインIDが一致する最初のSubscriberを設定する
	 * @param pluginId
	 * @param config
	 * @return
	 */
	public boolean configure(final long pluginId, @NonNull final ConfigSubscriber config);
}
