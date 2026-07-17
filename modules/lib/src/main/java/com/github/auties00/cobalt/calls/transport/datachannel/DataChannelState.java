package com.github.auties00.cobalt.calls.transport.datachannel;

import com.github.auties00.cobalt.calls.transport.CallTransportController;

/**
 * Enumerates the bring up states of the Web P2P application data channel.
 *
 * <p>On the WebRTC interoperable Web P2P path the application data channel comes up in stages: the
 * controller creates the data channel controller, runs the DTLS handshake, opens the SCTP association
 * and its DCEP channel, and only then becomes ready to carry application data. The states advance in a
 * fixed order and each step is gated on the previous one:
 *
 * {@snippet lang = "text":
 * UNINITIALIZED -> DTLS_HANDSHAKING -> SCTP_OPENING -> READY
 *                                                        |
 *                                          send failure  v
 *                                                     RELAY_FALLBACK
 *
 * any state -> CLOSED
 * }
 *
 * <p>Naming the stages lets the {@link CallTransportController} advance the channel one step at a time
 * and fall back to the relay application data stream if a send fails after the channel reached
 * {@link #READY}. The relay path does not run this machine; it carries application data over the relay
 * RTP stream directly.
 */
public enum DataChannelState {
    /**
     * No data channel exists yet; the data channel controller has not been created.
     */
    UNINITIALIZED,

    /**
     * The data channel controller is created and the DTLS handshake is in progress.
     *
     * <p>The SCTP association cannot open until the handshake completes.
     */
    DTLS_HANDSHAKING,

    /**
     * The DTLS handshake completed and the SCTP association with its DCEP channel is opening.
     */
    SCTP_OPENING,

    /**
     * The channel is open and ready to carry application data; buffered messages are flushed.
     */
    READY,

    /**
     * The Web P2P channel was deactivated after a send failure; application data now rides the relay RTP
     * stream.
     */
    RELAY_FALLBACK,

    /**
     * The channel is closed and carries no further application data.
     */
    CLOSED
}
