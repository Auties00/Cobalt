package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Country-of-origin and importer disclosure attached to a
 * {@link BusinessProduct}.
 *
 * <p>Several jurisdictions require imported goods to declare their
 * country of origin and, for cross-border shipments, the local importer
 * responsible for the goods. WhatsApp surfaces these fields under the
 * catalog product's {@code <compliance_info>} child so the consumer-side
 * product detail view can render the disclosure inline.
 *
 * <p>Only the country code of origin is required. The importer name and
 * importer address are independently optional and only published when
 * the merchant configured them for the destination market.
 */
@ProtobufMessage(name = "BusinessProductCompliance")
public final class BusinessProductCompliance {
    /**
     * ISO 3166-1 alpha-2 code of the product's country of origin.
     * Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String countryCodeOrigin;

    /**
     * Name of the entity responsible for importing the product into the
     * consumer's market. Empty when the merchant did not publish an
     * importer.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String importerName;

    /**
     * Postal address of the importer. Empty when the merchant did not
     * publish an importer address.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final BusinessProductImporterAddress importerAddress;

    /**
     * Constructs a new compliance block. The country code is required;
     * the importer name and address may be {@code null} when the
     * merchant omitted them.
     *
     * @param countryCodeOrigin the ISO 3166-1 alpha-2 country code; never {@code null}
     * @param importerName      the optional importer name, or {@code null}
     * @param importerAddress   the optional importer address, or {@code null}
     * @throws NullPointerException if {@code countryCodeOrigin} is {@code null}
     */
    BusinessProductCompliance(String countryCodeOrigin, String importerName,
                              BusinessProductImporterAddress importerAddress) {
        this.countryCodeOrigin = Objects.requireNonNull(countryCodeOrigin, "countryCodeOrigin cannot be null");
        this.importerName = importerName;
        this.importerAddress = importerAddress;
    }

    /**
     * Returns the ISO 3166-1 alpha-2 code of the product's country of
     * origin.
     *
     * @return the country code; never {@code null}
     */
    public String countryCodeOrigin() {
        return countryCodeOrigin;
    }

    /**
     * Returns the importer name.
     *
     * @return an {@code Optional} containing the importer name, or
     *         empty when the merchant did not publish one
     */
    public Optional<String> importerName() {
        return Optional.ofNullable(importerName);
    }

    /**
     * Returns the importer postal address.
     *
     * @return an {@code Optional} containing the importer address, or
     *         empty when the merchant did not publish one
     */
    public Optional<BusinessProductImporterAddress> importerAddress() {
        return Optional.ofNullable(importerAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessProductCompliance) obj;
        return Objects.equals(this.countryCodeOrigin, that.countryCodeOrigin)
                && Objects.equals(this.importerName, that.importerName)
                && Objects.equals(this.importerAddress, that.importerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(countryCodeOrigin, importerName, importerAddress);
    }

    @Override
    public String toString() {
        return "BusinessProductCompliance[" +
                "countryCodeOrigin=" + countryCodeOrigin + ", " +
                "importerName=" + importerName + ", " +
                "importerAddress=" + importerAddress + ']';
    }
}
