package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * One geographic entry of a Click-to-WhatsApp ad targeting spec.
 *
 * <p>The geographic constraint of a targeting spec groups locations by kind (cities, regions, custom
 * radius pins, and so on). This model is the shared shape of one entry across all those kinds, carrying
 * the union of fields the location builders read: a {@link #key() key} and {@link #name() display
 * name}, a {@link #radius() radius} with its {@link #distanceUnit() unit}, a
 * {@link #latitude() latitude}/{@link #longitude() longitude} centre, and the
 * {@link #country() country}, {@link #countryCode() country code}, {@link #countryName() country
 * name}, {@link #region() region}, {@link #primaryCity() primary city}, and
 * {@link #addressString() address string} that place it. Every field is optional; only the ones
 * relevant to the entry kind are set.
 */
@ProtobufMessage(name = "TargetingGeoLocationEntry")
public final class TargetingGeoLocationEntry {
    /**
     * Server-issued key identifying the location. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String key;

    /**
     * Display name of the location. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Radius of the served area around the centre point. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    final Double radius;

    /**
     * Unit the {@link #radius() radius} is expressed in ({@code "mile"} or {@code "km"}). Empty when
     * unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String distanceUnit;

    /**
     * Latitude of the location's centre. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
    final Double latitude;

    /**
     * Longitude of the location's centre. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.DOUBLE)
    final Double longitude;

    /**
     * Country the location belongs to. Empty when unset.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String country;

    /**
     * ISO country code of the location. Empty when unset.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String countryCode;

    /**
     * Country name of the location. Empty when unset.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String countryName;

    /**
     * Region the location belongs to. Empty when unset.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String region;

    /**
     * Primary city of the location. Empty when unset.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String primaryCity;

    /**
     * Full address string of the location. Empty when unset.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    final String addressString;

    /**
     * Constructs a new {@code TargetingGeoLocationEntry}. Every argument may be {@code null} to leave
     * the corresponding field unset.
     *
     * @param key           the location key, or {@code null}
     * @param name          the display name, or {@code null}
     * @param radius        the served-area radius, or {@code null}
     * @param distanceUnit  the radius unit, or {@code null}
     * @param latitude      the centre latitude, or {@code null}
     * @param longitude     the centre longitude, or {@code null}
     * @param country       the country, or {@code null}
     * @param countryCode   the ISO country code, or {@code null}
     * @param countryName   the country name, or {@code null}
     * @param region        the region, or {@code null}
     * @param primaryCity   the primary city, or {@code null}
     * @param addressString the full address string, or {@code null}
     */
    TargetingGeoLocationEntry(String key, String name, Double radius, String distanceUnit, Double latitude,
                              Double longitude, String country, String countryCode, String countryName,
                              String region, String primaryCity, String addressString) {
        this.key = key;
        this.name = name;
        this.radius = radius;
        this.distanceUnit = distanceUnit;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.region = region;
        this.primaryCity = primaryCity;
        this.addressString = addressString;
    }

    /**
     * Returns the server-issued key identifying the location.
     *
     * @return an {@link Optional} carrying the key, or empty when unset
     */
    public Optional<String> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the display name of the location.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the radius of the served area around the centre point.
     *
     * @return an {@link OptionalDouble} carrying the radius, or empty when unset
     */
    public OptionalDouble radius() {
        return radius == null ? OptionalDouble.empty() : OptionalDouble.of(radius);
    }

    /**
     * Returns the unit the radius is expressed in.
     *
     * @return an {@link Optional} carrying the distance unit, or empty when unset
     */
    public Optional<String> distanceUnit() {
        return Optional.ofNullable(distanceUnit);
    }

    /**
     * Returns the latitude of the location's centre.
     *
     * @return an {@link OptionalDouble} carrying the latitude, or empty when unset
     */
    public OptionalDouble latitude() {
        return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
    }

    /**
     * Returns the longitude of the location's centre.
     *
     * @return an {@link OptionalDouble} carrying the longitude, or empty when unset
     */
    public OptionalDouble longitude() {
        return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
    }

    /**
     * Returns the country the location belongs to.
     *
     * @return an {@link Optional} carrying the country, or empty when unset
     */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }

    /**
     * Returns the ISO country code of the location.
     *
     * @return an {@link Optional} carrying the country code, or empty when unset
     */
    public Optional<String> countryCode() {
        return Optional.ofNullable(countryCode);
    }

    /**
     * Returns the country name of the location.
     *
     * @return an {@link Optional} carrying the country name, or empty when unset
     */
    public Optional<String> countryName() {
        return Optional.ofNullable(countryName);
    }

    /**
     * Returns the region the location belongs to.
     *
     * @return an {@link Optional} carrying the region, or empty when unset
     */
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    /**
     * Returns the primary city of the location.
     *
     * @return an {@link Optional} carrying the primary city, or empty when unset
     */
    public Optional<String> primaryCity() {
        return Optional.ofNullable(primaryCity);
    }

    /**
     * Returns the full address string of the location.
     *
     * @return an {@link Optional} carrying the address string, or empty when unset
     */
    public Optional<String> addressString() {
        return Optional.ofNullable(addressString);
    }
}
