package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * One geographic area a WhatsApp Business profile serves.
 *
 * <p>A business may deliver goods or provide services within a circular area rather than at a fixed
 * storefront. Each such area is a circle: the {@link #radiusMeters() radius} around a centre point
 * given by its {@link #centerLatitude() latitude} and {@link #centerLongitude() longitude}, plus an
 * optional {@link #description() description} naming it.
 */
@ProtobufMessage(name = "BusinessProfileServiceArea")
public final class BusinessProfileServiceArea {
    /**
     * Radius of the served circle in metres. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer radiusMeters;

    /**
     * Latitude of the served circle's centre. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    final Double centerLatitude;

    /**
     * Longitude of the served circle's centre. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    final Double centerLongitude;

    /**
     * Human-readable description naming the served area. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String description;

    /**
     * Constructs a new {@code BusinessProfileServiceArea}. Every argument may be {@code null} to leave
     * the corresponding field unset.
     *
     * @param radiusMeters    the served-circle radius in metres, or {@code null}
     * @param centerLatitude  the centre latitude, or {@code null}
     * @param centerLongitude the centre longitude, or {@code null}
     * @param description     the area description, or {@code null}
     */
    BusinessProfileServiceArea(Integer radiusMeters, Double centerLatitude, Double centerLongitude, String description) {
        this.radiusMeters = radiusMeters;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.description = description;
    }

    /**
     * Returns the radius of the served circle in metres.
     *
     * @return an {@link OptionalInt} carrying the radius, or empty when unset
     */
    public OptionalInt radiusMeters() {
        return radiusMeters == null ? OptionalInt.empty() : OptionalInt.of(radiusMeters);
    }

    /**
     * Returns the latitude of the served circle's centre.
     *
     * @return an {@link OptionalDouble} carrying the latitude, or empty when unset
     */
    public OptionalDouble centerLatitude() {
        return centerLatitude == null ? OptionalDouble.empty() : OptionalDouble.of(centerLatitude);
    }

    /**
     * Returns the longitude of the served circle's centre.
     *
     * @return an {@link OptionalDouble} carrying the longitude, or empty when unset
     */
    public OptionalDouble centerLongitude() {
        return centerLongitude == null ? OptionalDouble.empty() : OptionalDouble.of(centerLongitude);
    }

    /**
     * Returns the human-readable description naming the served area.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }
}
