package com.github.auties00.cobalt.wire.linked.newsletter;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a caller-friendly description of a newsletter message
 * publish.
 *
 * <p>Two distinct publish flows share this entry point: a brand-new
 * message (carrying just a client-side stanza id and the message
 * payload bytes) and a contribution to a previously-published
 * message (carrying the target message's server id alongside the
 * contribution payload bytes). Question-response, reaction,
 * reaction-revoke and poll-vote contributions all use the
 * server-id-bound flow, while a brand-new message uses the
 * client-id-only flow.
 *
 * <p>The {@link #payloadBytes()} accessor returns the publish content
 * as protobuf-serialised message bytes; the caller is responsible for
 * encoding the appropriate variant. The
 * {@link #targetMessageServerId()} accessor identifies the
 * previously-published message a contribution targets, and is empty
 * for a brand-new message publish.
 *
 * <p>The optional {@link #originTag()} field tags forwarded broadcasts
 * with their original-source identifier; the optional
 * {@link #mediaContentId()} field carries the opaque media-content id
 * that pairs the publish with a previously-uploaded media object.
 */
public final class NewsletterPublishMessageRequest {
    /**
     * The locally-generated stanza id assigned to the publish.
     */
    private final String stanzaId;

    /**
     * The optional server id of the previously-published message
     * this publish targets.
     */
    private final Long targetMessageServerId;

    /**
     * The protobuf-serialised payload bytes carried by the publish.
     */
    private final byte[] payloadBytes;

    /**
     * The optional msg-meta-origin tag for forwarded broadcasts.
     */
    private final String originTag;

    /**
     * The optional media content id used to pair the publish with a
     * previously-uploaded media object.
     */
    private final String mediaContentId;

    /**
     * Constructs a new request.
     *
     * @param stanzaId              the publish stanza id; must not be
     *                              {@code null}
     * @param targetMessageServerId the optional target message server
     *                              id; supply {@code null} for a
     *                              brand-new message publish
     * @param payloadBytes          the publish payload bytes; must
     *                              not be {@code null}
     * @param originTag             the optional origin tag; may be
     *                              {@code null}
     * @param mediaContentId        the optional media content id;
     *                              may be {@code null}
     * @throws NullPointerException if {@code stanzaId} or
     *                              {@code payloadBytes} is
     *                              {@code null}
     */
    public NewsletterPublishMessageRequest(String stanzaId,
                                           Long targetMessageServerId,
                                           byte[] payloadBytes,
                                           String originTag,
                                           String mediaContentId) {
        this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
        this.targetMessageServerId = targetMessageServerId;
        this.payloadBytes = Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null").clone();
        this.originTag = originTag;
        this.mediaContentId = mediaContentId;
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
     * Returns the server id of the previously-published message this
     * publish targets.
     *
     * @return an {@link Optional} carrying the server id, or empty
     *         for a brand-new message publish
     */
    public Optional<Long> targetMessageServerId() {
        return Optional.ofNullable(targetMessageServerId);
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

    /**
     * Returns the optional msg-meta-origin tag for forwarded
     * broadcasts.
     *
     * @return an {@link Optional} carrying the tag, or empty when
     *         the publish is not a forwarded broadcast
     */
    public Optional<String> originTag() {
        return Optional.ofNullable(originTag);
    }

    /**
     * Returns the optional media content id.
     *
     * @return an {@link Optional} carrying the content id, or empty
     *         when the publish does not carry media
     */
    public Optional<String> mediaContentId() {
        return Optional.ofNullable(mediaContentId);
    }
}
