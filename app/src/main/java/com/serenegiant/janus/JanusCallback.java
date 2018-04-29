package com.serenegiant.janus;

import android.support.annotation.NonNull;

import org.appspot.apprtc.SignalingEvents;
import org.webrtc.PeerConnection;

import java.util.List;

public interface JanusCallback extends SignalingEvents {
	public void onConnectServer(@NonNull final JanusRTCClient client);
	public List<PeerConnection.IceServer> getIceServers(@NonNull final JanusRTCClient client);
}
