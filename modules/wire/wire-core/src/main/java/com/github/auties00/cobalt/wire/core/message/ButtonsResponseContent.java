package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for an inbound reply-button selection.
 *
 * <p>Carries the id and display text of the button the user tapped, from either an interactive
 * reply-button message or a legacy button tap.
 */
public interface ButtonsResponseContent extends MessageContent {
    /**
     * Returns the id of the selected button.
     *
     * @return an {@link Optional} holding the selected button id, or empty when unset
     */
    Optional<String> selectedButtonId();

    /**
     * Returns the display text of the selected button.
     *
     * @return an {@link Optional} holding the display text, or empty when unset
     */
    Optional<String> selectedDisplayText();
}
