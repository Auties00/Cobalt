package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for a single shared contact.
 *
 * <p>The contact carries a display name and a vCard rendering. The Cloud webhook delivers structured
 * contact fields which the decoder renders into the vCard string exposed here.
 */
public interface ContactContent extends MessageContent {
    /**
     * Returns the display name of the contact.
     *
     * @return an {@link Optional} holding the display name, or empty when unset
     */
    Optional<String> displayName();

    /**
     * Returns the vCard rendering of the contact.
     *
     * @return an {@link Optional} holding the vCard, or empty when unset
     */
    Optional<String> vcard();
}
