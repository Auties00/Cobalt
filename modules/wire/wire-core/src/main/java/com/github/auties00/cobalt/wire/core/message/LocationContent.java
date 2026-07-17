package com.github.auties00.cobalt.wire.core.message;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Transport-agnostic contract for a static location message body.
 *
 * <p>Both transports carry the same four location fields; the Linked transport additionally carries
 * live-location, thumbnail, accuracy, and context-info fields that the Cloud wire never delivers.
 */
public interface LocationContent extends MessageContent {
    /**
     * Returns the latitude of the location in degrees.
     *
     * @return the latitude, or empty when unset
     */
    OptionalDouble latitude();

    /**
     * Returns the longitude of the location in degrees.
     *
     * @return the longitude, or empty when unset
     */
    OptionalDouble longitude();

    /**
     * Returns the human-readable name of the location, when present.
     *
     * @return an {@link Optional} holding the name, or empty when none
     */
    Optional<String> name();

    /**
     * Returns the street address of the location, when present.
     *
     * @return an {@link Optional} holding the address, or empty when none
     */
    Optional<String> address();
}
