package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes a joinable call link: the shareable handle for a lobby-style call.
 *
 * <p>A link is identified by an opaque server-generated {@link #token()} that every link operation
 * uses as a lookup key. The link records its host, optional underlying call identifier, creation
 * time, media kind, and whether joiners must pass through a host-admitted lobby. The same value type
 * is surfaced to the application through the call-link lobby listener callbacks.
 *
 * @param token         the opaque server-generated link token, used as the lookup key by every link
 *                      operation
 * @param creator       the JID of the host who created the link
 * @param callId        the underlying call identifier the link refers to, populated once the host
 *                      starts the call and empty before then
 * @param createdAt     the wall-clock instant the link was created
 * @param videoEnabled  whether the link is configured for a video call rather than audio-only
 * @param requiresLobby whether joiners are queued in a lobby that the host must admit them from
 */
public record JoinableCallLink(
        String token,
        Jid creator,
        Optional<String> callId,
        Instant createdAt,
        boolean videoEnabled,
        boolean requiresLobby
) {
    /**
     * Constructs a joinable call link, rejecting {@code null} fields and an empty token.
     *
     * @throws NullPointerException     if {@code token}, {@code creator}, {@code callId}, or
     *                                  {@code createdAt} is {@code null}
     * @throws IllegalArgumentException if {@code token} is empty
     */
    public JoinableCallLink {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(creator, "creator cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        if (token.isEmpty()) {
            throw new IllegalArgumentException("token cannot be empty");
        }
    }
}
