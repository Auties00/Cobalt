package com.github.auties00.cobalt.wire.linked.location;

import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAction;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A geographic location pinned to a region of an image or video message.
 *
 * <p>Interactive annotations allow a media message to expose tappable
 * regions that perform a contextual action when the recipient taps them.
 * When the action is an instance of {@code Location}, tapping the region
 * opens the given latitude and longitude in the recipient's map
 * application. The tappable region itself is defined by a polygon of
 * {@link Point} vertices stored on the enclosing
 * {@link com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAnnotation InteractiveAnnotation}.
 *
 * <p>The {@link #name()} field provides an optional human-readable label
 * (for example, a venue or landmark name) that clients display alongside
 * the annotation. When the enclosing annotation also carries a postal
 * address, clients typically render the label as the name followed by a
 * newline and the address.
 *
 * <p>This class is distinct from the top-level location-sharing message
 * types ({@code LocationMessage} and {@code LiveLocationMessage}), which
 * represent standalone messages whose entire purpose is to share a
 * location. {@code Location} is a lightweight payload that appears only
 * as a nested action inside an {@code InteractiveAnnotation}.
 *
 * @see com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAnnotation
 * @see Point
 */
@ProtobufMessage(name = "Location")
public final class Location implements InteractiveAction {
    /**
     * The latitude of this location expressed in decimal degrees. Positive
     * values indicate positions north of the equator; negative values
     * indicate positions south of the equator.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    Double degreesLatitude;

    /**
     * The longitude of this location expressed in decimal degrees. Positive
     * values indicate positions east of the prime meridian; negative values
     * indicate positions west of the prime meridian.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    Double degreesLongitude;

    /**
     * The human-readable display name of this location, such as a venue or
     * landmark name, or {@code null} if no name was provided.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String name;

    /**
     * Constructs a new {@code Location} with the specified latitude,
     * longitude, and display name.
     *
     * @param degreesLatitude  the latitude in decimal degrees, or
     *                         {@code null} if not available
     * @param degreesLongitude the longitude in decimal degrees, or
     *                         {@code null} if not available
     * @param name             the display name of the location, or
     *                         {@code null} if not available
     */
    Location(Double degreesLatitude, Double degreesLongitude, String name) {
        this.degreesLatitude = degreesLatitude;
        this.degreesLongitude = degreesLongitude;
        this.name = name;
    }

    /**
     * Returns the latitude of this location in decimal degrees, if present.
     *
     * @return an {@code OptionalDouble} containing the latitude, or empty
     *         if the latitude was not set
     */
    public OptionalDouble degreesLatitude() {
        return degreesLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLatitude);
    }

    /**
     * Returns the longitude of this location in decimal degrees, if present.
     *
     * @return an {@code OptionalDouble} containing the longitude, or empty
     *         if the longitude was not set
     */
    public OptionalDouble degreesLongitude() {
        return degreesLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(degreesLongitude);
    }

    /**
     * Returns the human-readable display name of this location, if present.
     *
     * @return an {@code Optional} containing the location name, or empty if
     *         no name was provided
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets the latitude of this location in decimal degrees.
     *
     * @param degreesLatitude the latitude, or {@code null} to clear the
     *                        value
     */
    public void setDegreesLatitude(Double degreesLatitude) {
        this.degreesLatitude = degreesLatitude;
    }

    /**
     * Sets the longitude of this location in decimal degrees.
     *
     * @param degreesLongitude the longitude, or {@code null} to clear the
     *                         value
     */
    public void setDegreesLongitude(Double degreesLongitude) {
        this.degreesLongitude = degreesLongitude;
    }

    /**
     * Sets the human-readable display name of this location.
     *
     * @param name the location name, or {@code null} to clear the value
     */
    public void setName(String name) {
        this.name = name;
    }
}
