package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A Click-to-WhatsApp marketing-messages ad campaign created from the
 * WhatsApp Business broadcast composer.
 *
 * <p>When a merchant promotes a broadcast, WhatsApp spins up a paid Meta ad
 * campaign that, when tapped, drives the recipient into a chat with the
 * business. Creating that campaign produces a small tree of advertising
 * objects: an ad-campaign group, the ad campaign itself, an ad group inside
 * it, the individual ad, and the ad creative the ad renders. The server
 * returns the identifiers of every object in that tree alongside the
 * campaign's display name, its current status, its lifetime spending cap,
 * and the time it begins running.
 *
 * <p>This model is that freshly created campaign as the server reports it.
 * All fields are optional because the server may omit any of them; the
 * status, lifetime budget, and start time are exposed as raw strings because
 * their value sets and encodings are not recoverable from the WhatsApp Web
 * client.
 */
@ProtobufMessage(name = "BusinessMarketingCampaign")
public final class BusinessMarketingCampaign {
    /**
     * Identifier of the ad-campaign group the new campaign belongs to, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String adCampaignGroupId;

    /**
     * Identifier of the created ad campaign, or {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String adCampaignId;

    /**
     * Identifier of the ad group inside the campaign, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String adGroupId;

    /**
     * Identifier of the individual ad, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String adId;

    /**
     * Identifier of the ad creative the ad renders, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String adCreativeId;

    /**
     * Display name of the campaign, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String name;

    /**
     * Current status of the campaign, as a server-defined marker. The full
     * marker set is not recoverable from the WhatsApp client, so the raw
     * marker is exposed as a string. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String status;

    /**
     * Lifetime spending cap of the campaign, as a server-defined amount
     * marker. The encoding is not recoverable from the WhatsApp client, so the
     * raw marker is exposed as a string. {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String lifetimeBudget;

    /**
     * Time the campaign begins running, as a server-defined timestamp marker.
     * The encoding is not recoverable from the WhatsApp client, so the raw
     * marker is exposed as a string. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String startTime;

    /**
     * Constructs a new {@code BusinessMarketingCampaign}. Any reference
     * argument may be {@code null} when the server omitted the corresponding
     * field.
     *
     * @param adCampaignGroupId the ad-campaign group id, or {@code null}
     * @param adCampaignId      the ad-campaign id, or {@code null}
     * @param adGroupId         the ad-group id, or {@code null}
     * @param adId              the ad id, or {@code null}
     * @param adCreativeId      the ad-creative id, or {@code null}
     * @param name              the display name, or {@code null}
     * @param status            the campaign status marker, or {@code null}
     * @param lifetimeBudget    the lifetime spending cap marker, or {@code null}
     * @param startTime         the start-time marker, or {@code null}
     */
    BusinessMarketingCampaign(String adCampaignGroupId, String adCampaignId, String adGroupId, String adId,
                              String adCreativeId, String name, String status, String lifetimeBudget,
                              String startTime) {
        this.adCampaignGroupId = adCampaignGroupId;
        this.adCampaignId = adCampaignId;
        this.adGroupId = adGroupId;
        this.adId = adId;
        this.adCreativeId = adCreativeId;
        this.name = name;
        this.status = status;
        this.lifetimeBudget = lifetimeBudget;
        this.startTime = startTime;
    }

    /**
     * Returns the identifier of the ad-campaign group the new campaign belongs
     * to.
     *
     * @return the ad-campaign group id, or empty when the server omitted it
     */
    public Optional<String> adCampaignGroupId() {
        return Optional.ofNullable(adCampaignGroupId);
    }

    /**
     * Returns the identifier of the created ad campaign.
     *
     * @return the ad-campaign id, or empty when the server omitted it
     */
    public Optional<String> adCampaignId() {
        return Optional.ofNullable(adCampaignId);
    }

    /**
     * Returns the identifier of the ad group inside the campaign.
     *
     * @return the ad-group id, or empty when the server omitted it
     */
    public Optional<String> adGroupId() {
        return Optional.ofNullable(adGroupId);
    }

    /**
     * Returns the identifier of the individual ad.
     *
     * @return the ad id, or empty when the server omitted it
     */
    public Optional<String> adId() {
        return Optional.ofNullable(adId);
    }

    /**
     * Returns the identifier of the ad creative the ad renders.
     *
     * @return the ad-creative id, or empty when the server omitted it
     */
    public Optional<String> adCreativeId() {
        return Optional.ofNullable(adCreativeId);
    }

    /**
     * Returns the display name of the campaign.
     *
     * @return the display name, or empty when the server omitted it
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the current status of the campaign.
     *
     * @return the status marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the lifetime spending cap of the campaign.
     *
     * @return the lifetime-budget marker, or empty when the server omitted it
     */
    public Optional<String> lifetimeBudget() {
        return Optional.ofNullable(lifetimeBudget);
    }

    /**
     * Returns the time the campaign begins running.
     *
     * @return the start-time marker, or empty when the server omitted it
     */
    public Optional<String> startTime() {
        return Optional.ofNullable(startTime);
    }
}
