package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enumerates the roles that the current viewer can hold within a newsletter.
 *
 * <p>The role determines which actions the viewer can perform. Owners and
 * admins can edit metadata and publish messages; subscribers can read,
 * react, and forward; guests have a limited preview of the newsletter
 * without a proper subscription.
 */
@ProtobufEnum
public enum NewsletterViewerRole {
    /**
     * The role is not known or was not reported by the server.
     */
    UNKNOWN(0),

    /**
     * The viewer is the owner of the newsletter and can perform every
     * administrative action.
     */
    OWNER(1),

    /**
     * The viewer is a regular subscriber and can read, react to, and
     * forward messages.
     */
    SUBSCRIBER(2),

    /**
     * The viewer is an administrator and can publish messages and manage
     * newsletter settings.
     */
    ADMIN(3),

    /**
     * The viewer is a guest with limited read-only access, typically when
     * previewing a newsletter before subscribing.
     */
    GUEST(4);

    /**
     * The protobuf wire index associated with this constant.
     */
    final int index;

    /**
     * Lookup table from the lowercase enum name to the constant, used by
     * {@link #of(String)} for case-insensitive parsing.
     */
    private static final Map<String, NewsletterViewerRole> BY_NAME = Arrays.stream(NewsletterViewerRole.values())
            .collect(Collectors.toMap(entry -> entry.name().toLowerCase(), role -> role));

    /**
     * Lookup table from the protobuf wire index to the constant, used by
     * {@link #of(Integer)}.
     */
    private static final Map<Integer, NewsletterViewerRole> BY_INDEX = Arrays.stream(NewsletterViewerRole.values())
            .collect(Collectors.toMap(entry -> entry.index, role -> role));

    /**
     * Returns the constant whose name matches the supplied string,
     * case-insensitively.
     *
     * @param name the role name as reported by the server, may be
     *             {@code null}
     * @return the matching role constant, or {@link #UNKNOWN} when
     *         {@code name} is {@code null} or does not match any constant
     */
    public static NewsletterViewerRole of(String name) {
        return name == null ? UNKNOWN : BY_NAME.getOrDefault(name.toLowerCase(), UNKNOWN);
    }

    /**
     * Returns the constant whose protobuf wire index matches the supplied
     * value.
     *
     * @param index the protobuf wire index, may be {@code null}
     * @return the matching role constant, or {@link #UNKNOWN} when
     *         {@code index} is {@code null} or does not match any constant
     */
    public static NewsletterViewerRole of(Integer index) {
        return index == null ? UNKNOWN : BY_INDEX.getOrDefault(index, UNKNOWN);
    }

    /**
     * Constructs a new enum constant bound to the supplied protobuf wire
     * index.
     *
     * @param index the protobuf wire index
     */
    NewsletterViewerRole(@ProtobufEnumIndex int index) {
        this.index = index;
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
