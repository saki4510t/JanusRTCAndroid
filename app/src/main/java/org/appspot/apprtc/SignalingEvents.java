package org.appspot.apprtc;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Callback interface for messages delivered on signaling channel.
 * <p>
 * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
 */
public interface SignalingEvents {
	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	void onConnectedToRoom(final SignalingParameters params);
	
	/**
	 * Callback fired once remote SDP is received.
	 */
	void onRemoteDescription(final SessionDescription sdp);
	
	/**
	 * Callback fired once remote Ice candidate is received.
	 */
	void onRemoteIceCandidate(final IceCandidate candidate);
	
	/**
	 * Callback fired once remote Ice candidate removals are received.
	 */
	void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);
	
	/**
	 * Callback fired once channel is closed.
	 */
	void onChannelClose();
	
	/**
	 * Callback fired once channel error happened.
	 */
	void onChannelError(final String description);
}
