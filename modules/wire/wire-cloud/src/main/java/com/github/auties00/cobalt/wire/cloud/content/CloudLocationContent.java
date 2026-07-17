package com.github.auties00.cobalt.wire.cloud.content;

import com.github.auties00.cobalt.wire.core.message.LocationContent;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The Cloud transport's static-location content body.
 */
public final class CloudLocationContent implements LocationContent {
    /**
     * The latitude in degrees, or {@code null} when unset.
     */
    private final Double latitude;

    /**
     * The longitude in degrees, or {@code null} when unset.
     */
    private final Double longitude;

    /**
     * The location name, or {@code null} when unset.
     */
    private final String name;

    /**
     * The location address, or {@code null} when unset.
     */
    private final String address;

    /**
     * Constructs a Cloud location body.
     *
     * @param latitude  the latitude in degrees, or {@code null} when unset
     * @param longitude the longitude in degrees, or {@code null} when unset
     * @param name      the location name, or {@code null} when unset
     * @param address   the location address, or {@code null} when unset
     */
    public CloudLocationContent(Double latitude, Double longitude, String name, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.address = address;
    }

    @Override
    public OptionalDouble latitude() {
        return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
    }

    @Override
    public OptionalDouble longitude() {
        return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
    }

    @Override
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    @Override
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }
}
