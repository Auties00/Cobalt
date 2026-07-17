package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;
import java.util.OptionalDouble;

/**
 * A geographic place a merchant can target with a WhatsApp Business
 * advertisement.
 *
 * <p>When a merchant restricts where a "Click-to-WhatsApp" ad (a paid promotion
 * that opens a chat with the business when tapped) is shown, they pick places
 * from a location search: nearby spots when targeting a local radius, or wider
 * regions, countries, and cities when targeting broader areas. This model is one
 * matched place, covering both kinds of search; fields the server does not
 * report for a given place are simply absent.
 *
 * <p>{@link #key()} is the handle added to a targeting specification;
 * {@link #name()} is the label shown to the merchant; {@link #type()} is the
 * server-defined kind of place; {@link #countryCode()} and {@link #countryName()}
 * identify the country; {@link #region()} and {@link #regionId()} identify the
 * region; {@link #primaryCityId()} identifies the place's primary city for a
 * local match; {@link #latitude()} and {@link #longitude()} give the map point
 * of a local match; and {@link #worldwide()} reports whether the place is the
 * everywhere target.
 */
@ProtobufMessage(name = "BusinessAdLocation")
public final class BusinessAdLocation {
    /**
     * Server-issued targeting key of the place. This is the handle added to a
     * targeting specification. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String key;

    /**
     * Label of the place shown to the merchant. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Server-defined kind of place (for example a city, a region, or a country).
     * The full marker set is not recoverable from the WhatsApp client, so the
     * raw marker is exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String type;

    /**
     * Country code of the place. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String countryCode;

    /**
     * Country name of the place. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String countryName;

    /**
     * Region name of the place. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String region;

    /**
     * Region identifier of the place. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String regionId;

    /**
     * Identifier of the primary city of the place, reported for a local match.
     * Empty when the server omitted it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String primaryCityId;

    /**
     * Latitude of the place in decimal degrees, reported for a local match, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.DOUBLE)
    final Double latitude;

    /**
     * Longitude of the place in decimal degrees, reported for a local match, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.DOUBLE)
    final Double longitude;

    /**
     * Whether the place represents the everywhere (worldwide) target. Reported
     * by the server; {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BOOL)
    final boolean worldwide;

    /**
     * Constructs a new {@code BusinessAdLocation}. The reference arguments may be
     * {@code null} when the server omitted them.
     *
     * @param key           the targeting key, or {@code null}
     * @param name          the display label, or {@code null}
     * @param type          the place kind marker, or {@code null}
     * @param countryCode   the country code, or {@code null}
     * @param countryName   the country name, or {@code null}
     * @param region        the region name, or {@code null}
     * @param regionId      the region identifier, or {@code null}
     * @param primaryCityId the primary city identifier, or {@code null}
     * @param latitude      the latitude in decimal degrees, or {@code null}
     * @param longitude     the longitude in decimal degrees, or {@code null}
     * @param worldwide     whether the place is the everywhere target
     */
    BusinessAdLocation(String key,
                       String name,
                       String type,
                       String countryCode,
                       String countryName,
                       String region,
                       String regionId,
                       String primaryCityId,
                       Double latitude,
                       Double longitude,
                       boolean worldwide) {
        this.key = key;
        this.name = name;
        this.type = type;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.region = region;
        this.regionId = regionId;
        this.primaryCityId = primaryCityId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.worldwide = worldwide;
    }

    /**
     * Returns the server-issued targeting key of the place.
     *
     * @return the targeting key, or empty when the server omitted it
     */
    public Optional<String> key() {
        return Optional.ofNullable(key);
    }

    /**
     * Returns the label of the place shown to the merchant.
     *
     * @return the display label, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the server-defined kind of place.
     *
     * @return the place kind marker, or empty when the server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the country code of the place.
     *
     * @return the country code, or empty when the server omitted it
     */
    public Optional<String> countryCode() {
        return Optional.ofNullable(countryCode);
    }

    /**
     * Returns the country name of the place.
     *
     * @return the country name, or empty when the server omitted it
     */
    public Optional<String> countryName() {
        return Optional.ofNullable(countryName);
    }

    /**
     * Returns the region name of the place.
     *
     * @return the region name, or empty when the server omitted it
     */
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    /**
     * Returns the region identifier of the place.
     *
     * @return the region identifier, or empty when the server omitted it
     */
    public Optional<String> regionId() {
        return Optional.ofNullable(regionId);
    }

    /**
     * Returns the identifier of the primary city of the place.
     *
     * @return the primary city identifier, or empty when the server omitted it
     */
    public Optional<String> primaryCityId() {
        return Optional.ofNullable(primaryCityId);
    }

    /**
     * Returns the latitude of the place in decimal degrees.
     *
     * @return the latitude, or empty when the server omitted it
     */
    public OptionalDouble latitude() {
        return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
    }

    /**
     * Returns the longitude of the place in decimal degrees.
     *
     * @return the longitude, or empty when the server omitted it
     */
    public OptionalDouble longitude() {
        return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
    }

    /**
     * Returns whether the place represents the everywhere (worldwide) target.
     *
     * @return {@code true} when the place is the everywhere target, {@code false}
     *         otherwise
     */
    public boolean worldwide() {
        return worldwide;
    }
}
