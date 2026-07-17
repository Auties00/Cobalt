package com.github.auties00.cobalt.wire.linked.newsletter;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a caller-friendly description of a newsletter status
 * publish.
 *
 * <p>Two distinct flows share this entry point: a brand-new status
 * post (carrying just a client-side stanza id and the status payload
 * bytes) and a contribution to a previously-published status
 * (carrying the target status's server id alongside the contribution
 * payload bytes — typically a status-newsletter-reaction or
 * status-newsletter-reaction-revoke).
 *
 * <p>The {@link #payloadBytes()} accessor returns the status content
 * as protobuf-serialised message bytes; the
 * {@link #targetStatusServerId()} accessor identifies the
 * previously-published status a contribution targets, and is empty
 * for a brand-new status publish.
 */
public final class NewsletterPublishStatusRequest {
    /**
     * The locally-generated stanza id assigned to the publish.
     */
    private final String stanzaId;

    /**
     * The optional server id of the previously-published status this
     * publish targets.
     */
    private final Long targetStatusServerId;

    /**
     * The protobuf-serialised payload bytes carried by the publish.
     */
    private final byte[] payloadBytes;

    /**
     * Constructs a new request.
     *
     * @param stanzaId             the publish stanza id; must not be
     *                             {@code null}
     * @param targetStatusServerId the optional target status server
     *                             id; supply {@code null} for a
     *                             brand-new status publish
     * @param payloadBytes         the publish payload bytes; must not
     *                             be {@code null}
     * @throws NullPointerException if {@code stanzaId} or
     *                              {@code payloadBytes} is
     *                              {@code null}
     */
    public NewsletterPublishStatusRequest(String stanzaId, Long targetStatusServerId, byte[] payloadBytes) {
        this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        this.targetStatusServerId = targetStatusServerId;
        this.payloadBytes = Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null").clone();
    }

    /**
     * Returns the locally-generated stanza id assigned to the
     * publish.
     *
     * @return the stanza id, never {@code null}
     */
    public String stanzaId() {
        return stanzaId;
    }

    /**
     * Returns the server id of the previously-published status this
     * publish targets.
     *
     * @return an {@link Optional} carrying the server id, or empty
     *         for a brand-new status publish
     */
    public Optional<Long> targetStatusServerId() {
        return Optional.ofNullable(targetStatusServerId);
    }

    /**
     * Returns the protobuf-serialised payload bytes carried by the
     * publish.
     *
     * @return a defensive copy of the payload bytes, never
     *         {@code null}
     */
    public byte[] payloadBytes() {
        return payloadBytes.clone();
    }
}
