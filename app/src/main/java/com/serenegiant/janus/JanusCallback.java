package com.serenegiant.janus;

import android.support.annotation.NonNull;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

public interface JanusCallback {
	/**
	 * callback when JanusRTCClient connects janus-gateway server
	 * @param client
	 */
	public void onConnectServer(@NonNull final JanusRTCClient client);
	
	/**
	 * get list of IceServer
	 * @param client
	 * @return
	 */
	public List<PeerConnection.IceServer> getIceServers(@NonNull final JanusRTCClient client);

	/**
	 * Callback fired once the room's signaling parameters
	 * SignalingParameters are extracted.
	 */
	public void onConnectedToRoom(final boolean initiator);
	
	/**
	 * Callback fired once remote SDP is received.
	 */
	public void onRemoteDescription(final SessionDescription sdp);
	
	/**
	 * Callback fired once remote Ice candidate is received.
	 */
	public void onRemoteIceCandidate(final IceCandidate candidate);
	
	/**
	 * Callback fired once remote Ice candidate removals are received.
	 */
	public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

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
	public void onChannelClose();
	
	/**
	 * Callback fired once peer connection is closed.
	 */
	public void onDisconnected();

	/**
	 * Callback fired once channel error happened.
	 */
	public void onChannelError(final String description);

}
