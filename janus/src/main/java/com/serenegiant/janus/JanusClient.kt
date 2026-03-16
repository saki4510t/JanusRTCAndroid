package com.serenegiant.janus
/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *  Modified 2018 - 2026 saki t_saki@serenegiant.com
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoCapturer
import org.webrtc.VideoSink

/**
 * com.serenegiant.janus.JanusClient is the interface representing an AppRTC client.
 */
interface JanusClient {
	interface ErrorCallback {
		fun onError(t: Throwable)
	}

	interface ListCallback<T> : ErrorCallback {
		fun onSuccess(result: T)
	}

	/**
	 * request to create PeerConnectionFactory
	 * @param options
	 */
	fun createPeerConnectionFactory(
		options: PeerConnectionFactory.Options?
	)

	/**
	 * create PeerConnection
	 * @param localRender
	 * @param videoCapturer
	 */
	fun createPeerConnection(
		localRender: VideoSink,
		videoCapturer: VideoCapturer?
	)

	/**
	 * temporary disable video transmitting/receiving
	 */
	fun stopVideoSource()

	/**
	 * temporary enable video transmitting/receiving
	 */
	fun startVideoSource()

	/**
	 * switch camera to transmit camera images
	 */
	fun switchCamera()

	/**
	 * request change video size and frame rate
	 * @param width
	 * @param height
	 * @param framerate
	 */
	fun changeCaptureFormat(width: Int, height: Int, framerate: Int)

	/**
	 * temporary enable/disable voice transmitting/receiving
	 * @param enable
	 */
	fun setAudioEnabled(enable: Boolean)

	/**
	 * temporary enable/disable video transmitting/receiving
	 */
	fun setVideoEnabled(enable: Boolean)

	/**
	 * request update stats
	 * @param enable
	 * @param periodMs
	 */
	fun enableStatsEvents(enable: Boolean, periodMs: Int)
}
