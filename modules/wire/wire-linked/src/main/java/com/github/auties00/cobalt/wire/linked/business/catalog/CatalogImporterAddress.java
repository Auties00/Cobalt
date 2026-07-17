package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Importer address of a WhatsApp Business catalog product's compliance information.
 *
 * <p>Where a product must declare an importer for compliance, this model carries the importer's postal
 * address: the {@link #street1() first} and {@link #street2() second} street lines, the
 * {@link #postalCode() postal code}, the {@link #city() city}, the {@link #region() region}, and the
 * {@link #countryCode() country code}.
 */
@ProtobufMessage(name = "CatalogImporterAddress")
public final class CatalogImporterAddress {
    /**
     * First street line of the address. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String street1;

    /**
     * Second street line of the address. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String street2;

    /**
     * Postal code of the address. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String postalCode;

    /**
     * City of the address. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String city;

    /**
     * Region of the address. Empty when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String region;

    /**
     * ISO country code of the address. Empty when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String countryCode;

    /**
     * Constructs a new {@code CatalogImporterAddress}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param street1     the first street line, or {@code null}
     * @param street2     the second street line, or {@code null}
     * @param postalCode  the postal code, or {@code null}
     * @param city        the city, or {@code null}
     * @param region      the region, or {@code null}
     * @param countryCode the ISO country code, or {@code null}
     */
    CatalogImporterAddress(String street1, String street2, String postalCode, String city, String region,
                           String countryCode) {
        this.street1 = street1;
        this.street2 = street2;
        this.postalCode = postalCode;
        this.city = city;
        this.region = region;
        this.countryCode = countryCode;
    }

    /**
     * Returns the first street line of the address.
     *
     * @return an {@link Optional} carrying the first street line, or empty when unset
     */
    public Optional<String> street1() {
        return Optional.ofNullable(street1);
    }

    /**
     * Returns the second street line of the address.
     *
     * @return an {@link Optional} carrying the second street line, or empty when unset
     */
    public Optional<String> street2() {
        return Optional.ofNullable(street2);
    }

    /**
     * Returns the postal code of the address.
     *
     * @return an {@link Optional} carrying the postal code, or empty when unset
     */
    public Optional<String> postalCode() {
        return Optional.ofNullable(postalCode);
    }

    /**
     * Returns the city of the address.
     *
     * @return an {@link Optional} carrying the city, or empty when unset
     */
    public Optional<String> city() {
        return Optional.ofNullable(city);
    }

    /**
     * Returns the region of the address.
     *
     * @return an {@link Optional} carrying the region, or empty when unset
     */
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    /**
     * Returns the ISO country code of the address.
     *
     * @return an {@link Optional} carrying the country code, or empty when unset
     */
    public Optional<String> countryCode() {
        return Optional.ofNullable(countryCode);
    }
}
