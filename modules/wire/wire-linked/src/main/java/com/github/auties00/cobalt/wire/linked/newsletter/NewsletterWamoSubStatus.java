package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerates the possible WAMO (WhatsApp Marketing Offers) subscription
 * statuses associated with a newsletter or with the current viewer's
 * subscription to a newsletter.
 *
 * <p>WAMO is WhatsApp's paid subscription surface on top of newsletters,
 * allowing admins to gate content behind a monthly plan. This enum reports
 * whether the subscription is currently billable.
 */
@ProtobufEnum
public enum NewsletterWamoSubStatus {
    /**
     * The subscription status was not reported by the server or is not
     * recognized by this version of the client.
     */
    UNKNOWN(0),

    /**
     * The subscription is currently active and the subscriber can access
     * paid content.
     */
    ACTIVE(1),

    /**
     * The subscription is currently inactive, for example because a payment
     * failed or the user cancelled their plan.
     */
    INACTIVE(2);

    /**
     * Lookup table from the lowercase enum name to the constant, used by
     * {@link #of(String)} for case-insensitive parsing.
     */
    private static final Map<String, NewsletterWamoSubStatus> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(key -> key.name().toLowerCase(), Function.identity()));

    /**
     * The protobuf wire index that identifies this constant on the wire.
     */
    final int index;

    /**
     * Constructs a new enum constant bound to the supplied protobuf wire
     * index.
     *
     * @param index the protobuf wire index
     */
    NewsletterWamoSubStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the enum constant whose name matches the supplied string,
     * case-insensitively.
     *
     * @param name the status name as reported by the server, may be
     *             {@code null}
     * @return the matching status constant, or {@link #UNKNOWN} when
     *         {@code name} is {@code null} or does not match any constant
     */
    public static NewsletterWamoSubStatus of(String name) {
        return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
    }

    /**
     * Returns the name of this enum constant.
     *
     * @return the constant name as written in source code
     */
    @Override
    public String toString() {
        return name();
    }
}
