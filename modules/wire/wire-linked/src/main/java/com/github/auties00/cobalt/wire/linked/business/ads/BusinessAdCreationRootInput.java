package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Creation-context input identifying the boost the Click-to-WhatsApp ad-creation opening screen
 * resolves.
 *
 * <p>When the merchant enters the ad-creation flow, the opening-screen query passes this context to the
 * server-side resolver: the funding {@link #adAccountId() ad account}, the {@link #boostId() boost}
 * and {@link #flowId() flow} the run belongs to, the promoted {@link #pageId() page}, and the
 * {@link #product() product} kind being boosted.
 */
@ProtobufMessage(name = "BusinessAdCreationRootInput")
public final class BusinessAdCreationRootInput {
    /**
     * Legacy Facebook ad-account identifier the boost is funded from. A numeric string, not a WhatsApp
     * address. Empty when unset.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Identifier of the boost being created. Empty when unset.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String boostId;

    /**
     * Opaque correlator grouping the ad-creation funnel run. Empty when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String flowId;

    /**
     * Identifier of the page being promoted. Empty when unset.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Product kind being boosted. Empty when unset; the ad-creation flow emits
     * {@link BusinessAdProduct#BOOSTED_MESSAGE}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.ENUM)
    final BusinessAdProduct product;

    /**
     * Constructs a new {@code BusinessAdCreationRootInput}. Every argument may be {@code null} to leave
     * the corresponding field unset.
     *
     * @param adAccountId the funding ad-account identifier, or {@code null}
     * @param boostId     the boost identifier, or {@code null}
     * @param flowId      the funnel-run correlator, or {@code null}
     * @param pageId      the promoted page identifier, or {@code null}
     * @param product     the product kind, or {@code null}
     */
    BusinessAdCreationRootInput(String adAccountId, String boostId, String flowId, String pageId, BusinessAdProduct product) {
        this.adAccountId = adAccountId;
        this.boostId = boostId;
        this.flowId = flowId;
        this.pageId = pageId;
        this.product = product;
    }

    /**
     * Returns the funding ad-account identifier.
     *
     * @return an {@link Optional} carrying the ad-account identifier, or empty when unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the identifier of the boost being created.
     *
     * @return an {@link Optional} carrying the boost identifier, or empty when unset
     */
    public Optional<String> boostId() {
        return Optional.ofNullable(boostId);
    }

    /**
     * Returns the funnel-run correlator.
     *
     * @return an {@link Optional} carrying the correlator, or empty when unset
     */
    public Optional<String> flowId() {
        return Optional.ofNullable(flowId);
    }

    /**
     * Returns the identifier of the page being promoted.
     *
     * @return an {@link Optional} carrying the page identifier, or empty when unset
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns the product kind being boosted.
     *
     * @return an {@link Optional} carrying the product kind, or empty when unset
     */
    public Optional<BusinessAdProduct> product() {
        return Optional.ofNullable(product);
    }
}
