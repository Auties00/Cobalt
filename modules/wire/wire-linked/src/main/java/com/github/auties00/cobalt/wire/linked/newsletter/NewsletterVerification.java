package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufDeserializer;
import it.auties.protobuf.annotation.ProtobufSerializer;

import java.util.Objects;

/**
 * Represents the verification status of a newsletter.
 *
 * <p>A verified newsletter shows a checkmark badge next to its name,
 * confirming that WhatsApp has reviewed and approved the channel's
 * identity. This class models the verification flag as a two-state value
 * serialised as the strings {@code "ON"} and {@code "OFF"}.
 *
 * <p>Instances are not constructed directly; use {@link #enabled()} and
 * {@link #disabled()} to obtain the two shared singletons.
 */
public class NewsletterVerification {
    /**
     * The wire representation of the verified state.
     */
    private static final String ENABLED_JSON_VALUE = "ON";

    /**
     * The wire representation of the unverified state.
     */
    private static final String DISABLED_JSON_VALUE = "OFF";

    /**
     * Shared singleton returned by {@link #enabled()}.
     */
    private static final NewsletterVerification ENABLED = new NewsletterVerification(true);

    /**
     * Shared singleton returned by {@link #disabled()}.
     */
    private static final NewsletterVerification DISABLED = new NewsletterVerification(false);

    /**
     * Whether this instance represents the verified state.
     */
    private final boolean verified;

    /**
     * Constructs one of the two singletons.
     *
     * @param verified {@code true} for the verified singleton,
     *                 {@code false} for the unverified one
     */
    private NewsletterVerification(boolean verified) {
        this.verified = verified;
    }

    /**
     * Returns the shared singleton representing a verified newsletter.
     *
     * @return the verified singleton, never {@code null}
     */
    public static NewsletterVerification enabled() {
        return ENABLED;
    }

    /**
     * Returns the shared singleton representing an unverified newsletter.
     *
     * @return the unverified singleton, never {@code null}
     */
    public static NewsletterVerification disabled() {
        return DISABLED;
    }

    /**
     * Deserialises a verification flag from its wire string form. Invoked
     * by the protobuf runtime.
     *
     * @param value the raw wire string, expected to be {@code "ON"} or
     *              {@code "OFF"}
     * @return {@link #enabled()} when {@code value} equals {@code "ON"},
     *         {@link #disabled()} otherwise
     */
    @ProtobufDeserializer
    static NewsletterVerification deserialize(String value) {
        return ENABLED_JSON_VALUE.equals(value) ? ENABLED : DISABLED;
    }

    /**
     * Serialises this verification flag to its wire string form. Invoked
     * by the protobuf runtime.
     *
     * @return {@code "ON"} when this is the verified singleton,
     *         {@code "OFF"} otherwise
     */
    @ProtobufSerializer
    String serialize() {
        return verified ? ENABLED_JSON_VALUE : DISABLED_JSON_VALUE;
    }

    /**
     * Returns whether this instance represents the verified state.
     *
     * @return {@code true} when the newsletter is verified, {@code false}
     *         otherwise
     */
    public boolean verified() {
        return verified;
    }

    /**
     * Returns whether this verification flag equals the supplied object.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code NewsletterVerification}
     *         with the same verified state
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterVerification that
                            && verified == that.verified;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for this verification flag
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(verified);
    }
}
