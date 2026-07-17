package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;

/**
 * Geographic component of a Click-to-WhatsApp ad targeting spec.
 *
 * <p>An ad's targeting spec constrains the audience by location, grouped by kind. This model carries
 * the location groups WhatsApp Web's targeting reader and builder use: the {@link #countries()
 * countries} (as bare country codes) and the {@link #cities() cities}, {@link #regions() regions},
 * {@link #countryGroups() country groups}, {@link #customLocations() custom radius locations},
 * {@link #zips() postal areas}, {@link #neighborhoods() neighbourhoods}, {@link #geoMarkets() geo
 * markets}, and {@link #places() places}, each a list of {@link TargetingGeoLocationEntry} entries.
 */
@ProtobufMessage(name = "TargetingGeoLocations")
public final class TargetingGeoLocations {
    /**
     * ISO 3166-1 alpha-2 country codes the ad is eligible in, in the order they are sent. Never
     * {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> countries;

    /**
     * Targeted cities, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> cities;

    /**
     * Targeted regions, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> regions;

    /**
     * Targeted country groups, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> countryGroups;

    /**
     * Targeted custom radius locations, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> customLocations;

    /**
     * Targeted postal areas, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> zips;

    /**
     * Targeted neighbourhoods, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> neighborhoods;

    /**
     * Targeted geo markets, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> geoMarkets;

    /**
     * Targeted places, in the order they are sent. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    final List<TargetingGeoLocationEntry> places;

    /**
     * Constructs a new {@code TargetingGeoLocations}. Every {@code null} list argument is coerced to an
     * empty list.
     *
     * @param countries       the country codes; {@code null} treated as empty
     * @param cities          the targeted cities; {@code null} treated as empty
     * @param regions         the targeted regions; {@code null} treated as empty
     * @param countryGroups   the targeted country groups; {@code null} treated as empty
     * @param customLocations the targeted custom radius locations; {@code null} treated as empty
     * @param zips            the targeted postal areas; {@code null} treated as empty
     * @param neighborhoods   the targeted neighbourhoods; {@code null} treated as empty
     * @param geoMarkets      the targeted geo markets; {@code null} treated as empty
     * @param places          the targeted places; {@code null} treated as empty
     */
    TargetingGeoLocations(List<String> countries, List<TargetingGeoLocationEntry> cities,
                          List<TargetingGeoLocationEntry> regions, List<TargetingGeoLocationEntry> countryGroups,
                          List<TargetingGeoLocationEntry> customLocations, List<TargetingGeoLocationEntry> zips,
                          List<TargetingGeoLocationEntry> neighborhoods, List<TargetingGeoLocationEntry> geoMarkets,
                          List<TargetingGeoLocationEntry> places) {
        this.countries = countries == null ? List.of() : List.copyOf(countries);
        this.cities = cities == null ? List.of() : List.copyOf(cities);
        this.regions = regions == null ? List.of() : List.copyOf(regions);
        this.countryGroups = countryGroups == null ? List.of() : List.copyOf(countryGroups);
        this.customLocations = customLocations == null ? List.of() : List.copyOf(customLocations);
        this.zips = zips == null ? List.of() : List.copyOf(zips);
        this.neighborhoods = neighborhoods == null ? List.of() : List.copyOf(neighborhoods);
        this.geoMarkets = geoMarkets == null ? List.of() : List.copyOf(geoMarkets);
        this.places = places == null ? List.of() : List.copyOf(places);
    }

    /**
     * Returns the country codes the ad is eligible in.
     *
     * @return an unmodifiable view of the country codes; never {@code null}, possibly empty
     */
    public List<String> countries() {
        return countries;
    }

    /**
     * Returns the targeted cities.
     *
     * @return an unmodifiable view of the targeted cities; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> cities() {
        return cities;
    }

    /**
     * Returns the targeted regions.
     *
     * @return an unmodifiable view of the targeted regions; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> regions() {
        return regions;
    }

    /**
     * Returns the targeted country groups.
     *
     * @return an unmodifiable view of the targeted country groups; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> countryGroups() {
        return countryGroups;
    }

    /**
     * Returns the targeted custom radius locations.
     *
     * @return an unmodifiable view of the targeted custom locations; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> customLocations() {
        return customLocations;
    }

    /**
     * Returns the targeted postal areas.
     *
     * @return an unmodifiable view of the targeted postal areas; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> zips() {
        return zips;
    }

    /**
     * Returns the targeted neighbourhoods.
     *
     * @return an unmodifiable view of the targeted neighbourhoods; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> neighborhoods() {
        return neighborhoods;
    }

    /**
     * Returns the targeted geo markets.
     *
     * @return an unmodifiable view of the targeted geo markets; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> geoMarkets() {
        return geoMarkets;
    }

    /**
     * Returns the targeted places.
     *
     * @return an unmodifiable view of the targeted places; never {@code null}, possibly empty
     */
    public List<TargetingGeoLocationEntry> places() {
        return places;
    }
}
