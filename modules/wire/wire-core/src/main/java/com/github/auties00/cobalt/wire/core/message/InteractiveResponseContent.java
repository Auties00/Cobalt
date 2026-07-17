package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;

/**
 * Transport-agnostic contract for an inbound native-flow (interactive) reply.
 *
 * <p>Carries the native-flow response name, its raw JSON parameters, and an optional body text.
 */
public interface InteractiveResponseContent extends MessageContent {
    /**
     * Returns the name of the native-flow response.
     *
     * @return an {@link Optional} holding the response name, or empty when unset
     */
    Optional<String> name();

    /**
     * Returns the raw JSON parameters of the native-flow response.
     *
     * @return an {@link Optional} holding the response JSON, or empty when unset
     */
    Optional<String> responseJson();

    /**
     * Returns the optional body text accompanying the response.
     *
     * @return an {@link Optional} holding the body text, or empty when none
     */
    Optional<String> body();
}
