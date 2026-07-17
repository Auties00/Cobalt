package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerates the discoverability policies that may be configured for a
 * newsletter.
 *
 * <p>This flag determines whether the newsletter appears in directory
 * searches or can only be reached via an invite link.
 */
@ProtobufEnum
public enum NewsletterPrivacy {
    /**
     * The privacy policy was not reported by the server or is not
     * recognized by this version of the client.
     */
    UNKNOWN(0),

    /**
     * The newsletter is publicly discoverable through the directory search.
     */
    PUBLIC(1),

    /**
     * The newsletter is private and can only be joined through an invite
     * link shared by an admin.
     */
    PRIVATE(2);

    /**
     * Lookup table from the lowercase enum name to the constant, used by
     * {@link #of(String)} for case-insensitive parsing.
     */
    private static final Map<String, NewsletterPrivacy> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(key -> key.name().toLowerCase(), Function.identity()));

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * Constructs a new enum constant bound to the supplied protobuf wire
     * index.
     *
     * @param index the protobuf wire index
     */
    NewsletterPrivacy(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * Returns the constant whose name matches the supplied string,
     * case-insensitively.
     *
     * @param name the privacy name as reported by the server, may be
     *             {@code null}
     * @return the matching privacy constant, or {@link #UNKNOWN} when
     *         {@code name} is {@code null} or does not match any constant
     */
    static NewsletterPrivacy of(String name) {
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
