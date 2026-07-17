package com.github.auties00.cobalt.wam.model;

import com.github.auties00.cobalt.wam.annotation.WamEvent;

/**
 * A transport channel for WhatsApp Metrics (WAM) events, such as
 * {@code REGULAR}.
 *
 * <p>Each channel determines how the event buffer is transmitted to
 * WhatsApp servers and is encoded as a single byte in the WAM binary
 * buffer header.
 *
 * <p>The three channels serve different purposes:
 * <ul>
 * <li>{@link #REGULAR} is the standard telemetry path, sent as an XMPP
 *     {@code <iq>} stanza over the existing Noise-encrypted WebSocket
 *     connection.
 * <li>{@link #REALTIME} uses the same transport as {@code REGULAR} but
 *     triggers an immediate flush with near-zero delay, suitable for
 *     latency-sensitive events.
 * <li>{@link #PRIVATE} uses a separate HTTP upload path with blinded
 *     authentication tokens so the server cannot correlate the upload
 *     to the authenticated session.
 * </ul>
 *
 * @see WamEvent#channel()
 */
public enum WamChannel {
    /**
     * The standard telemetry channel.
     * This has the wire value of {@code 0x00} in the buffer header.
     */
    REGULAR(0),

    /**
     * The real-time telemetry channel for latency-sensitive events.
     * This has the wire value of {@code 0x01} in the buffer header.
     */
    REALTIME(1),

    /**
     * The privacy-sensitive channel with anonymized identifiers and
     * blinded-token authentication.
     * This has the wire value of {@code 0x02} in the buffer header.
     */
    PRIVATE(2);

    /**
     * Constructs a new {@code WamChannel} with the given wire value.
     *
     * @param id the channel byte value in the WAM buffer header
     */
    WamChannel(int id) {
        this.id = id;
    }

    /**
     * The numeric channel identifier written to the WAM buffer header.
     */
    final int id;

    /**
     * Returns the numeric channel identifier used in the WAM buffer header.
     *
     * @return the channel byte value
     */
    public int id() {
        return id;
    }
}
