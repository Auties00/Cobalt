package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregates the senders of a single reaction code on a newsletter
 * message.
 *
 * <p>When a viewer taps a reaction code on a newsletter post they are
 * shown the list of accounts that reacted with the same emoji. This
 * type binds the emoji code to the senders' identifiers so that the UI
 * can render the breakdown row by row, and provides the optional
 * profile-picture direct path for displaying their avatar without an
 * additional contact lookup.
 */
@ProtobufMessage
public final class NewsletterReactor {
    /**
     * The reaction emoji code these senders reacted with.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String reactionCode;

    /**
     * The senders that reacted with the given emoji code.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    List<Sender> senders;

    /**
     * Constructs a new {@code NewsletterReactor}. Invoked by the
     * generated protobuf deserializer and by the converters that adapt
     * wire responses into the domain model.
     *
     * @param reactionCode the reaction emoji code, may be {@code null}
     * @param senders      the senders; defaulted to an empty list when
     *                     {@code null}
     */
    NewsletterReactor(String reactionCode, List<Sender> senders) {
        this.reactionCode = reactionCode;
        this.senders = senders == null ? List.of() : List.copyOf(senders);
    }

    /**
     * Returns the reaction emoji code.
     *
     * @return an {@link Optional} carrying the code, or empty when the
     *         relay has not reported one
     */
    public Optional<String> reactionCode() {
        return Optional.ofNullable(reactionCode);
    }

    /**
     * Returns the senders that reacted with the emoji code.
     *
     * @return an unmodifiable list of senders, never {@code null}
     */
    public List<Sender> senders() {
        return Collections.unmodifiableList(senders);
    }

    /**
     * Returns whether this reactor entry equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterReactor}
     *         carrying equal fields
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterReactor that
                && Objects.equals(reactionCode, that.reactionCode)
                && Objects.equals(senders, that.senders);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(reactionCode, senders);
    }

    /**
     * Returns a debug-oriented string representation.
     *
     * @return a human-readable string
     */
    @Override
    public String toString() {
        return "NewsletterReactor[reactionCode=" + reactionCode +
                ", senders=" + senders.size() + ']';
    }

    /**
     * Represents a single account that reacted with a particular emoji
     * code on a newsletter post.
     */
    @ProtobufMessage
    public static final class Sender {
        /**
         * The stable WhatsApp identifier of the reactor.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Jid jid;

        /**
         * The direct path of the reactor's profile picture on the media
         * server.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String profilePictureDirectPath;

        /**
         * Constructs a new {@code Sender}. Invoked by the generated
         * protobuf deserializer and by the converters that adapt wire
         * responses into the domain model.
         *
         * @param jid                      the reactor JID; must not be
         *                                 {@code null}
         * @param profilePictureDirectPath the profile picture direct path,
         *                                 may be {@code null}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        Sender(Jid jid, String profilePictureDirectPath) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            this.profilePictureDirectPath = profilePictureDirectPath;
        }

        /**
         * Returns the stable WhatsApp identifier of this reactor.
         *
         * @return the reactor JID, never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the direct path of the reactor's profile picture on the
         * media server.
         *
         * @return an {@link Optional} carrying the direct path, or empty
         *         when not reported
         */
        public Optional<String> profilePictureDirectPath() {
            return Optional.ofNullable(profilePictureDirectPath);
        }

        /**
         * Returns whether this sender equals the supplied object.
         *
         * @param o the object to compare against
         * @return {@code true} if {@code o} is a {@code Sender} carrying
         *         equal fields
         */
        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Sender that
                    && Objects.equals(jid, that.jid)
                    && Objects.equals(profilePictureDirectPath, that.profilePictureDirectPath);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, profilePictureDirectPath);
        }

        /**
         * Returns a debug-oriented string representation.
         *
         * @return a human-readable string
         */
        @Override
        public String toString() {
            return "Sender[jid=" + jid + ']';
        }
    }
}
