package com.github.auties00.cobalt.wire.core.message;

import java.util.List;
import java.util.Optional;

/**
 * Transport-agnostic contract for a shared array of contacts.
 *
 * <p>Carries a display name for the group and the list of individual {@link ContactContent contacts}.
 */
public interface ContactsArrayContent extends MessageContent {
    /**
     * Returns the display name for the contacts group.
     *
     * @return an {@link Optional} holding the display name, or empty when unset
     */
    Optional<String> displayName();

    /**
     * Returns the contacts carried by this message.
     *
     * @return an unmodifiable list of contacts, never {@code null}
     */
    List<ContactContent> contacts();
}
