package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Importer postal address attached to a {@link BusinessProductCompliance}
 * block.
 *
 * <p>When a WhatsApp Business catalog product is sold across borders,
 * the regulatory disclosure carries the importer's postal address so
 * the destination market can verify accountability for imported goods.
 * The first street line, city and country code are required; every
 * other component (street line 2, postal code, region) is independently
 * optional and absent when the merchant did not configure it.
 */
@ProtobufMessage(name = "BusinessProductImporterAddress")
public final class BusinessProductImporterAddress {
    /**
     * First line of the importer's street address. Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String street1;

    /**
     * Second line of the importer's street address. Empty when the
     * merchant did not configure a second line.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String street2;

    /**
     * Postal code of the importer's address. Empty when the merchant
     * did not configure a postal code.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String postalCode;

    /**
     * City of the importer's address. Always populated.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String city;

    /**
     * Region (state, province, ...) of the importer's address. Empty
     * when the merchant did not configure a region.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String region;

    /**
     * ISO 3166-1 alpha-2 country code of the importer's address.
     * Always populated.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String countryCode;

    /**
     * Constructs a new importer-address block. The {@code street1},
     * {@code city} and {@code countryCode} arguments are required; the
     * remaining components may be {@code null} when the merchant
     * omitted them.
     *
     * @param street1     the first street line; never {@code null}
     * @param street2     the optional second street line, or {@code null}
     * @param postalCode  the optional postal code, or {@code null}
     * @param city        the city; never {@code null}
     * @param region      the optional region, or {@code null}
     * @param countryCode the ISO 3166-1 alpha-2 country code; never {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    BusinessProductImporterAddress(String street1, String street2, String postalCode,
                                   String city, String region, String countryCode) {
        this.street1 = Objects.requireNonNull(street1, "street1 cannot be null");
        this.street2 = street2;
        this.postalCode = postalCode;
        this.city = Objects.requireNonNull(city, "city cannot be null");
        this.region = region;
        this.countryCode = Objects.requireNonNull(countryCode, "countryCode cannot be null");
    }

    /**
     * Returns the first line of the importer's street address.
     *
     * @return the first street line; never {@code null}
     */
    public String street1() {
        return street1;
    }

    /**
     * Returns the second line of the importer's street address.
     *
     * @return an {@code Optional} containing the second street line,
     *         or empty
     */
    public Optional<String> street2() {
        return Optional.ofNullable(street2);
    }

    /**
     * Returns the postal code of the importer's address.
     *
     * @return an {@code Optional} containing the postal code, or empty
     */
    public Optional<String> postalCode() {
        return Optional.ofNullable(postalCode);
    }

    /**
     * Returns the city of the importer's address.
     *
     * @return the city; never {@code null}
     */
    public String city() {
        return city;
    }

    /**
     * Returns the region of the importer's address.
     *
     * @return an {@code Optional} containing the region, or empty
     */
    public Optional<String> region() {
        return Optional.ofNullable(region);
    }

    /**
     * Returns the ISO 3166-1 alpha-2 country code of the importer's
     * address.
     *
     * @return the country code; never {@code null}
     */
    public String countryCode() {
        return countryCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProductImporterAddress) obj;
        return Objects.equals(this.street1, that.street1)
                && Objects.equals(this.street2, that.street2)
                && Objects.equals(this.postalCode, that.postalCode)
                && Objects.equals(this.city, that.city)
                && Objects.equals(this.region, that.region)
                && Objects.equals(this.countryCode, that.countryCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street1, street2, postalCode, city, region, countryCode);
    }

    @Override
    public String toString() {
        return "BusinessProductImporterAddress[" +
                "street1=" + street1 + ", " +
                "city=" + city + ", " +
                "countryCode=" + countryCode + ']';
    }
}
