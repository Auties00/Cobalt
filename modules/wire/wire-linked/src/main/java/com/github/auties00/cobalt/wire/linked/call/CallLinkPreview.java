package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;

/**
 * Describes a call link the local user is considering joining.
 *
 * <p>A preview wraps the underlying {@link JoinableCallLink} with the metadata a UI needs to render a
 * join prompt without committing the user to a lobby admit: the host's best-effort display name, the
 * JIDs currently in the call, and the host-configured participant cap. The active participant list is
 * empty while the host has not yet started the call.
 *
 * @param link               the underlying {@link JoinableCallLink}
 * @param creatorName        the host's display name on a best-effort basis; empty when the host's
 *                           profile is hidden
 * @param activeParticipants the JIDs currently in the call; an empty list means the host has not
 *                           started yet
 * @param participantCount   the total participant cap configured by the host
 */
public record CallLinkPreview(
        JoinableCallLink link,
        String creatorName,
        List<Jid> activeParticipants,
        int participantCount
) {
    /**
     * Constructs a preview, rejecting {@code null} fields and a negative participant cap, and
     * defensively copying the participant list.
     *
     * @throws NullPointerException     if {@code link}, {@code creatorName}, or
     *                                  {@code activeParticipants} is {@code null}
     * @throws IllegalArgumentException if {@code participantCount} is negative
     */
    public CallLinkPreview {
        Objects.requireNonNull(link, "link cannot be null");
        Objects.requireNonNull(creatorName, "creatorName cannot be null");
        Objects.requireNonNull(activeParticipants, "activeParticipants cannot be null");
        if (participantCount < 0) {
            throw new IllegalArgumentException("participantCount must be >= 0");
        }
        activeParticipants = List.copyOf(activeParticipants);
    }
}
