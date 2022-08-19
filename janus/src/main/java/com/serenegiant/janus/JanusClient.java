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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSink;

/**
 * com.serenegiant.janus.JanusClient is the interface representing an AppRTC client.
 */
public interface JanusClient {

	public interface ErrorCallback {
		public void onError(@NonNull final Throwable t);
	}

	public interface ListCallback<T> extends ErrorCallback {
		public void onSuccess(@NonNull final T result);
	}

	/**
	 * request to create PeerConnectionFactory
	 * @param options
	 */
	public void createPeerConnectionFactory(
		@NonNull final PeerConnectionFactory.Options options);
	
	/**
	 * create PeerConnection
	 * @param localRender
	 * @param videoCapturer
	 */
	public void createPeerConnection(
		@NonNull final VideoSink localRender,
		@Nullable final VideoCapturer videoCapturer);
	
	/**
	 * temporary disable video transmitting/receiving
	 */
	public void stopVideoSource();
	
	/**
	 * temporary enable video transmitting/receiving
	 */
	public void startVideoSource();
	
	/**
	 * switch camera to transmit camera images
	 */
	public void switchCamera();
	
	/**
	 * request change video size and frame rate
	 * @param width
	 * @param height
	 * @param framerate
	 */
	public void changeCaptureFormat(final int width, final int height, final int framerate);
	
	/**
	 * temporary enable/disable voice transmitting/receiving
	 * @param enable
	 */
	public void setAudioEnabled(final boolean enable);
	
	/**
	 * temporary enable/disable video transmitting/receiving
	 */
	public void setVideoEnabled(final boolean enable);
	
	/**
	 * request update stats
	 * @param enable
	 * @param periodMs
	 */
	public void enableStatsEvents(final boolean enable, final int periodMs);


}
