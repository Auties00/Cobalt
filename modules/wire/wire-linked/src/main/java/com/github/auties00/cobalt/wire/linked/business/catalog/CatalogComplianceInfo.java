package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Compliance information of a WhatsApp Business catalog product write payload.
 *
 * <p>Some markets require a product to declare its country of origin and importer. This model carries
 * that compliance data: the {@link #countryCodeOrigin() country of origin}, the
 * {@link #importerName() importer name}, and the {@link #importerAddress() importer address}.
 */
@ProtobufMessage(name = "CatalogComplianceInfo")
public final class CatalogComplianceInfo {
    /**
     * ISO country code the product originates from. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String countryCodeOrigin;

    /**
     * Name of the product's importer. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String importerName;

    /**
     * Postal address of the product's importer. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final CatalogImporterAddress importerAddress;

    /**
     * Constructs a new {@code CatalogComplianceInfo}. Every argument may be {@code null} to leave the
     * corresponding field unset.
     *
     * @param countryCodeOrigin the country of origin, or {@code null}
     * @param importerName      the importer name, or {@code null}
     * @param importerAddress   the importer address, or {@code null}
     */
    CatalogComplianceInfo(String countryCodeOrigin, String importerName, CatalogImporterAddress importerAddress) {
        this.countryCodeOrigin = countryCodeOrigin;
        this.importerName = importerName;
        this.importerAddress = importerAddress;
    }

    /**
     * Returns the ISO country code the product originates from.
     *
     * @return an {@link Optional} carrying the country of origin, or empty when unset
     */
    public Optional<String> countryCodeOrigin() {
        return Optional.ofNullable(countryCodeOrigin);
    }

    /**
     * Returns the name of the product's importer.
     *
     * @return an {@link Optional} carrying the importer name, or empty when unset
     */
    public Optional<String> importerName() {
        return Optional.ofNullable(importerName);
    }

    /**
     * Returns the postal address of the product's importer.
     *
     * @return an {@link Optional} carrying the importer address, or empty when unset
     */
    public Optional<CatalogImporterAddress> importerAddress() {
        return Optional.ofNullable(importerAddress);
    }
}
