package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Per-page ad-creation verdict in a WhatsApp Ads advertising flow.
 *
 * <p>While creating a Click-to-WhatsApp ad the merchant picks the Facebook page
 * the ad runs under. The server resolves that page to its identifier and reports
 * whether the viewer is currently permitted to create ads for it. This model is
 * that resolved page id paired with the per-page create-ads verdict.
 */
@ProtobufMessage(name = "WhatsAppAdsPageEligibility")
public final class WhatsAppAdsPageEligibility {
    /**
     * Server-issued Facebook page identifier. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Whether the viewer is permitted to create ads for the resolved page.
     * {@code false} when the server omitted the verdict.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean canCreateAds;

    /**
     * Constructs a new {@code WhatsAppAdsPageEligibility}.
     *
     * @param pageId       the Facebook page identifier, or {@code null}
     * @param canCreateAds whether the viewer may create ads for the page
     */
    WhatsAppAdsPageEligibility(String pageId, boolean canCreateAds) {
        this.pageId = pageId;
        this.canCreateAds = canCreateAds;
    }

    /**
     * Returns the resolved Facebook page identifier.
     *
     * @return the page identifier, or empty when the server omitted it
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns whether the viewer is permitted to create ads for the resolved
     * page.
     *
     * @return {@code true} when the viewer may create ads, {@code false} when
     *         the server reported the verdict false or omitted it
     */
    public boolean canCreateAds() {
        return canCreateAds;
    }
}
