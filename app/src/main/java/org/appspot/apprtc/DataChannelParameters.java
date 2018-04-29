package org.appspot.apprtc;

/**
 * Peer connection parameters.
 */
public class DataChannelParameters {
	public final boolean ordered;
	public final int maxRetransmitTimeMs;
	public final int maxRetransmits;
	public final String protocol;
	public final boolean negotiated;
	public final int id;
	
	public DataChannelParameters(final boolean ordered,
		final int maxRetransmitTimeMs, final int maxRetransmits,
		final String protocol, final boolean negotiated, final int id) {

		this.ordered = ordered;
		this.maxRetransmitTimeMs = maxRetransmitTimeMs;
		this.maxRetransmits = maxRetransmits;
		this.protocol = protocol;
		this.negotiated = negotiated;
		this.id = id;
	}
}
