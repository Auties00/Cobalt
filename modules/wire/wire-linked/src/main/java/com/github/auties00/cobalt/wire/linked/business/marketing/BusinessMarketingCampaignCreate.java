package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for creating a Click-to-WhatsApp marketing-messages ad
 * campaign from the WhatsApp Business broadcast composer.
 *
 * <p>A marketing-messages campaign is a paid Meta ad campaign that drives
 * recipients into a WhatsApp Business chat. Spinning one up takes the
 * funding ad account, the campaign's human-readable name, its lifetime
 * spending cap, the linked Facebook page, and the WhatsApp Business
 * Account the campaign is tied to.
 *
 * <p>{@link #adAccountId()} names the Meta ad account funding the
 * campaign. {@link #campaignName()} is the human-readable campaign name
 * shown in the ad surfaces. {@link #lifetimeBudget()} caps the campaign's
 * total spend; the value is expressed in the billing currency's lowest
 * unit (for example cents for USD) as a string to preserve the server's
 * exact precision. {@link #pageId()} identifies the linked Facebook page
 * that owns the ad. {@link #whatsAppBusinessAccountId()} is the WhatsApp
 * Business Account identifier the campaign is tied to.
 */
@ProtobufMessage(name = "BusinessMarketingCampaignCreate")
public final class BusinessMarketingCampaignCreate {
    /**
     * Meta ad-account identifier funding the campaign. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Human-readable campaign name shown in the ad surfaces. Unset omits
     * the variable.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String campaignName;

    /**
     * Lifetime spending cap for the campaign, expressed in the billing
     * currency's lowest unit (for example cents for USD) as a string. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String lifetimeBudget;

    /**
     * Linked Facebook page identifier that owns the ad. Unset omits the
     * variable.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String pageId;

    /**
     * WhatsApp Business Account identifier the campaign is tied to. Unset
     * omits the variable.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String whatsAppBusinessAccountId;

    /**
     * Constructs a new {@code BusinessMarketingCampaignCreate}. Every
     * argument may be {@code null} to omit the corresponding variable from
     * the request.
     *
     * @param adAccountId               the Meta ad-account identifier
     *                                  funding the campaign, or
     *                                  {@code null}
     * @param campaignName              the human-readable campaign name,
     *                                  or {@code null}
     * @param lifetimeBudget            the lifetime spending cap in the
     *                                  billing currency's lowest unit, or
     *                                  {@code null}
     * @param pageId                    the linked Facebook page
     *                                  identifier, or {@code null}
     * @param whatsAppBusinessAccountId the WhatsApp Business Account
     *                                  identifier, or {@code null}
     */
    public BusinessMarketingCampaignCreate(String adAccountId, String campaignName, String lifetimeBudget,
                                           String pageId, String whatsAppBusinessAccountId) {
        this.adAccountId = adAccountId;
        this.campaignName = campaignName;
        this.lifetimeBudget = lifetimeBudget;
        this.pageId = pageId;
        this.whatsAppBusinessAccountId = whatsAppBusinessAccountId;
    }

    /**
     * Returns the Meta ad-account identifier funding the campaign.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the human-readable campaign name.
     *
     * @return an {@link Optional} carrying the name, or empty when unset
     */
    public Optional<String> campaignName() {
        return Optional.ofNullable(campaignName);
    }

    /**
     * Returns the lifetime spending cap.
     *
     * @return an {@link Optional} carrying the cap in the billing
     *         currency's lowest unit, or empty when unset
     */
    public Optional<String> lifetimeBudget() {
        return Optional.ofNullable(lifetimeBudget);
    }

    /**
     * Returns the linked Facebook page identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns the WhatsApp Business Account identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty when
     *         unset
     */
    public Optional<String> whatsAppBusinessAccountId() {
        return Optional.ofNullable(whatsAppBusinessAccountId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessMarketingCampaignCreate) obj;
        return Objects.equals(adAccountId, that.adAccountId)
                && Objects.equals(campaignName, that.campaignName)
                && Objects.equals(lifetimeBudget, that.lifetimeBudget)
                && Objects.equals(pageId, that.pageId)
                && Objects.equals(whatsAppBusinessAccountId, that.whatsAppBusinessAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adAccountId, campaignName, lifetimeBudget, pageId, whatsAppBusinessAccountId);
    }

    @Override
    public String toString() {
        return "BusinessMarketingCampaignCreate[" +
                "adAccountId=" + adAccountId + ", " +
                "campaignName=" + campaignName + ", " +
                "lifetimeBudget=" + lifetimeBudget + ", " +
                "pageId=" + pageId + ", " +
                "whatsAppBusinessAccountId=" + whatsAppBusinessAccountId + ']';
    }
}
