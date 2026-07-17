package com.github.auties00.cobalt.wire.linked.call;

/**
 * Enumerates the user-visible phases of a call's lifecycle.
 *
 * <p>Internal protocol-level events (ICE connectivity checks, the DTLS handshake, SCTP association
 * setup) are folded into {@link #CONNECTING}. A caller only needs to observe {@link #ACTIVE} to know
 * media is flowing and {@link #ENDED} to know the call may be cleaned up. The constants are ordered
 * chronologically, so {@link Enum#ordinal()} comparison reflects lifecycle progression. The current
 * phase of a call is read through {@link Call#state()}.
 */
public enum CallState {
    /**
     * Indicates the call has been offered and is awaiting acceptance.
     *
     * <p>For an outbound call the local user is the caller and the peer is being notified; for an
     * inbound call the local user is the callee and must accept or reject the offer.
     */
    RINGING,

    /**
     * Indicates the peer has accepted and transport setup is underway.
     *
     * <p>The ICE, DTLS, and SRTP layers are negotiating; no application-layer media is flowing yet.
     */
    CONNECTING,

    /**
     * Indicates media is flowing and the call's media streams are live.
     */
    ACTIVE,

    /**
     * Indicates the transport layer detected a network change and is re-establishing the media path.
     *
     * <p>A Wi-Fi to cellular IP swap, a relay outage, or a prolonged round-trip-time spike drives the
     * call here while a fresh DTLS and SRTP path is negotiated against a new relay or candidate pair.
     * Frames written during this window are buffered or dropped at the call's discretion. The call
     * returns to {@link #ACTIVE} once the new handshake completes, or moves to {@link #ENDED} if
     * recovery fails.
     */
    RECONNECTING,

    /**
     * Indicates the call is over for any reason: hangup, reject, timeout, or network failure.
     *
     * <p>Once a call reaches this state its {@link Call#endReason()} is populated and no further
     * frames are exchanged.
     */
    ENDED
}
