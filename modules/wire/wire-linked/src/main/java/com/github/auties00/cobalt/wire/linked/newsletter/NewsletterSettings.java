package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Groups together the server-configurable settings of a newsletter.
 *
 * <p>At present the only setting surfaced by the server is the reaction
 * policy, which determines which emoji subscribers may use to react to
 * published messages. Future server revisions are expected to extend this
 * structure with additional fields; clients should therefore treat this
 * type as a container rather than as a single flag.
 *
 * @see NewsletterReactionSettings
 */
@ProtobufMessage
public final class NewsletterSettings {
    /**
     * The reaction policy configured for the newsletter.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    NewsletterReactionSettings reactionCodes;

    /**
     * Constructs a new {@code NewsletterSettings}. Invoked by the generated
     * protobuf deserializer.
     *
     * @param reactionCodes the reaction policy, must not be {@code null}
     * @throws NullPointerException if {@code reactionCodes} is {@code null}
     */
    NewsletterSettings(NewsletterReactionSettings reactionCodes) {
        this.reactionCodes = Objects.requireNonNull(reactionCodes, "reactionCodes cannot be null");
    }

    /**
     * Returns the reaction policy configured for the newsletter.
     *
     * @return the reaction policy, never {@code null}
     */
    public NewsletterReactionSettings reactionCodes() {
        return reactionCodes;
    }

    /**
     * Sets the reaction policy configured for the newsletter.
     *
     * @param reactionCodes the new reaction policy
     */
    public void setReactionCodes(NewsletterReactionSettings reactionCodes) {
        this.reactionCodes = reactionCodes;
    }

    /**
     * Returns whether this settings object equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterSettings} with
     *         the same reaction policy
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterSettings that
               && Objects.equals(reactionCodes, that.reactionCodes);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this settings object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(reactionCodes);
    }
}
