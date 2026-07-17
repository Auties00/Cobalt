package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalDouble;

/**
 * A geographic point given by its latitude and longitude.
 *
 * <p>Some WhatsApp Business profile lookups bias their results around a map centre expressed as a bare
 * coordinate pair. This model holds that pair: the {@link #latitude() latitude} and
 * {@link #longitude() longitude} of the point.
 */
@ProtobufMessage(name = "BusinessGeoPoint")
public final class BusinessGeoPoint {
    /**
     * Latitude of the point. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    final Double latitude;

    /**
     * Longitude of the point. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    final Double longitude;

    /**
     * Constructs a new {@code BusinessGeoPoint}. Every argument may be {@code null} to leave the
     * corresponding coordinate unset.
     *
     * @param latitude  the latitude, or {@code null}
     * @param longitude the longitude, or {@code null}
     */
    BusinessGeoPoint(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Returns the latitude of the point.
     *
     * @return an {@link OptionalDouble} carrying the latitude, or empty when unset
     */
    public OptionalDouble latitude() {
        return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
    }

    /**
     * Returns the longitude of the point.
     *
     * @return an {@link OptionalDouble} carrying the longitude, or empty when unset
     */
    public OptionalDouble longitude() {
        return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
    }
}
