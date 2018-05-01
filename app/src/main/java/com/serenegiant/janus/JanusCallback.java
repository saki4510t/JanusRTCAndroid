package com.serenegiant.janus;

import android.support.annotation.NonNull;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

public interface JanusCallback {
	public void onConnectServer(@NonNull final JanusRTCClient client);
	public List<PeerConnection.IceServer> getIceServers(@NonNull final JanusRTCClient client);

	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	void onConnectedToRoom(final boolean initiator);
	
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
	 * Callback fired once connection is established (IceConnectionState is
	 * CONNECTED).
	 */
	public void onIceConnected();
	
	/**
	 * Callback fired once connection is closed (IceConnectionState is
	 * DISCONNECTED).
	 */
	public void onIceDisconnected();
	
	/**
	 * Callback fired once channel is closed.
	 */
	void onChannelClose();
	
	/**
	 * Callback fired once peer connection is closed.
	 */
	public void onPeerConnectionClosed();

	/**
	 * Callback fired once channel error happened.
	 */
	void onChannelError(final String description);

}
