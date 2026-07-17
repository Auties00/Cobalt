package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for an inbound list selection.
 *
 * <p>Carries the title and description of the chosen row and its id.
 */
public interface ListResponseContent extends MessageContent {
    /**
     * Returns the title of the selected row.
     *
     * @return an {@link Optional} holding the title, or empty when unset
     */
    Optional<String> title();

    /**
     * Returns the description of the selected row.
     *
     * @return an {@link Optional} holding the description, or empty when unset
     */
    Optional<String> description();

    /**
     * Returns the id of the selected row.
     *
     * @return an {@link Optional} holding the selected row id, or empty when unset
     */
    Optional<String> selectedRowId();
}
