package com.github.auties00.cobalt.wire.linked.newsletter;

import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Per-channel configuration that controls which emoji reactions
 * followers may post on messages published in a channel.
 *
 * <p>Channel admins choose between four policies exposed by {@link Type}:
 * accept every emoji ({@link Type#ALL}), accept only a curated set of
 * basic emojis ({@link Type#BASIC}), disable reactions entirely
 * ({@link Type#NONE}), or admit every emoji except those listed in
 * {@link #blockedCodes()} ({@link Type#BLOCKLIST}). The blocked list is
 * authored from the admin settings sheet and is meaningful only for the
 * blocklist policy; for any other policy the field is ignored.
 *
 * <p>The activation timestamp records when the current policy was
 * applied so that clients can invalidate locally cached reaction
 * aggregates that predate the change. The timestamp is wire-encoded as a
 * UNIX seconds value via {@link InstantSecondsMixin} but exposed
 * client-side as an {@link Instant}.
 */
@ProtobufMessage
public final class NewsletterReactionSettings {
    /**
     * The active reaction policy. Defaults to {@link Type#UNKNOWN} when
     * the wire value is missing or unrecognised.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    Type value;

    /**
     * The emoji codes explicitly blocked when the policy is
     * {@link Type#BLOCKLIST}. Ignored for every other policy.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    List<String> blockedCodes;

    /**
     * The moment at which the current policy was last activated, used to
     * invalidate stale client-side reaction caches. Wire-encoded as
     * UNIX seconds.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    Instant enabledTimestamp;

    /**
     * Constructs a new {@code NewsletterReactionSettings}. Invoked by the
     * generated protobuf deserializer.
     *
     * @param value            the reaction policy, defaulted to
     *                         {@link Type#UNKNOWN} when {@code null}
     * @param blockedCodes     the blocked emoji codes, defaulted to a
     *                         fresh mutable list when {@code null}
     * @param enabledTimestamp the moment the current policy was activated,
     *                         may be {@code null}
     */
    NewsletterReactionSettings(Type value, List<String> blockedCodes, Instant enabledTimestamp) {
        this.value = Objects.requireNonNullElse(value, Type.UNKNOWN);
        this.blockedCodes = Objects.requireNonNullElseGet(blockedCodes, ArrayList::new);
        this.enabledTimestamp = enabledTimestamp;
    }

    /**
     * Returns the active reaction policy.
     *
     * @return the current policy, never {@code null}
     */
    public Type value() {
        return value;
    }

    /**
     * Sets the active reaction policy. {@code null} is normalised to
     * {@link Type#UNKNOWN}.
     *
     * @param value the new policy, or {@code null} to fall back to
     *              {@link Type#UNKNOWN}
     */
    public void setValue(Type value) {
        this.value = Objects.requireNonNullElse(value, Type.UNKNOWN);
    }

    /**
     * Returns the emoji codes currently blocked.
     *
     * <p>The returned list is meaningful only when the policy is
     * {@link Type#BLOCKLIST}; for every other policy clients should
     * ignore it.
     *
     * @return an unmodifiable list of blocked emoji codes, never
     *         {@code null}
     */
    public List<String> blockedCodes() {
        return blockedCodes == null ? List.of() : Collections.unmodifiableList(blockedCodes);
    }

    /**
     * Sets the emoji codes blocked when the policy is
     * {@link Type#BLOCKLIST}.
     *
     * @param blockedCodes the new blocked codes list, or {@code null} to
     *                     clear
     */
    public void setBlockedCodes(List<String> blockedCodes) {
        this.blockedCodes = blockedCodes;
    }

    /**
     * Returns the moment at which the current policy was last activated.
     *
     * @return an {@link Optional} holding the activation instant, or
     *         empty when not set
     */
    public Optional<Instant> enabledTimestamp() {
        return Optional.ofNullable(enabledTimestamp);
    }

    /**
     * Sets the moment at which the current policy was activated.
     *
     * @param enabledTimestamp the new activation instant, or {@code null}
     *                         to clear
     */
    public void setEnabledTimestamp(Instant enabledTimestamp) {
        this.enabledTimestamp = enabledTimestamp;
    }

    /**
     * Compares these settings with another object for equality.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a
     *         {@code NewsletterReactionSettings} whose fields all match
     *         this one's
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof NewsletterReactionSettings that
                            && value == that.value
                            && Objects.equals(blockedCodes, that.blockedCodes)
                            && Objects.equals(enabledTimestamp, that.enabledTimestamp);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code for these settings
     */
    @Override
    public int hashCode() {
        return Objects.hash(value, blockedCodes, enabledTimestamp);
    }

    /**
     * Enumerates the four reaction policies that may be configured for a
     * channel, plus an {@link #UNKNOWN} fallback for unrecognised wire
     * values.
     */
    @ProtobufEnum
    public enum Type {
        /**
         * Every emoji is accepted as a reaction.
         */
        ALL(0),

        /**
         * Only a curated set of basic emoji is accepted as a reaction.
         */
        BASIC(1),

        /**
         * Every emoji is accepted except those listed in
         * {@link NewsletterReactionSettings#blockedCodes()}.
         */
        BLOCKLIST(2),

        /**
         * No reactions are accepted.
         */
        NONE(3),

        /**
         * The policy was not reported by the server or is unrecognized
         * by this version of the client.
         */
        UNKNOWN(4);

        /**
         * Lookup table from the lowercase enum name to the constant,
         * used by {@link #of(String)} for case-insensitive parsing.
         */
        private static final Map<String, Type> BY_NAME = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(key -> key.name().toLowerCase(), Function.identity()));

        /**
         * Returns the constant whose name matches the supplied string,
         * case-insensitively.
         *
         * @param name the policy name as reported by the server, may be
         *             {@code null}
         * @return the matching policy constant, or {@link #UNKNOWN} when
         *         {@code name} is {@code null} or does not match any
         *         constant
         */
        public static Type of(String name) {
            return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
        }

        /**
         * The protobuf wire index associated with this constant.
         */
        final int index;

        /**
         * Constructs a new enum constant bound to the supplied protobuf
         * wire index.
         *
         * @param index the protobuf wire index
         */
        Type(@ProtobufEnumIndex int index) {
            this.index = index;
        }
    }
}
