package com.github.auties00.cobalt.wire.linked.business;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Linked-account targets resolved for a WhatsApp Business broadcast.
 *
 * <p>Onboarding a broadcast (paid marketing) flow requires the merchant's
 * WhatsApp account to be linked to a Meta business, a payment account, an
 * advertising account, and a Facebook page. WhatsApp resolves those four
 * targets up-front so the composer can show which entities the broadcast
 * will be associated with before the merchant commits.
 *
 * <p>This model is the resolved set of targets exactly as the server reports
 * them. Each target is identified by its server-issued id; the ids are
 * exposed as plain strings because they are Meta marketing-platform
 * identifiers, not WhatsApp addresses. Any field may be empty when the
 * server omitted it, typically because the corresponding link has not been
 * established yet.
 */
@ProtobufMessage(name = "BusinessBroadcastTargetInfo")
public final class BusinessBroadcastTargetInfo {
    /**
     * Identifier of the Meta business the broadcast is associated with, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String businessId;

    /**
     * Identifier of the payment account funding the broadcast, or
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String businessPaymentAccountId;

    /**
     * Identifier of the advertising account funding the campaign created
     * from the broadcast, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String adAccountId;

    /**
     * Identifier of the Facebook page the broadcast is published on behalf
     * of, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String pageId;

    /**
     * Constructs a new {@code BusinessBroadcastTargetInfo}. Any reference
     * argument may be {@code null} when the server omitted the
     * corresponding link.
     *
     * @param businessId               the Meta business id, or {@code null}
     * @param businessPaymentAccountId the payment-account id, or {@code null}
     * @param adAccountId              the advertising-account id, or {@code null}
     * @param pageId                   the Facebook page id, or {@code null}
     */
    BusinessBroadcastTargetInfo(String businessId, String businessPaymentAccountId,
                                String adAccountId, String pageId) {
        this.businessId = businessId;
        this.businessPaymentAccountId = businessPaymentAccountId;
        this.adAccountId = adAccountId;
        this.pageId = pageId;
    }

    /**
     * Returns the identifier of the Meta business the broadcast is
     * associated with.
     *
     * @return the business id, or empty when the server omitted it
     */
    public Optional<String> businessId() {
        return Optional.ofNullable(businessId);
    }

    /**
     * Returns the identifier of the payment account funding the broadcast.
     *
     * @return the payment-account id, or empty when the server omitted it
     */
    public Optional<String> businessPaymentAccountId() {
        return Optional.ofNullable(businessPaymentAccountId);
    }

    /**
     * Returns the identifier of the advertising account funding the
     * campaign.
     *
     * @return the advertising-account id, or empty when the server omitted
     *         it
     */
    public Optional<String> adAccountId() {
        return Optional.ofNullable(adAccountId);
    }

    /**
     * Returns the identifier of the Facebook page the broadcast is
     * published on behalf of.
     *
     * @return the page id, or empty when the server omitted it
     */
    public Optional<String> pageId() {
        return Optional.ofNullable(pageId);
    }

    /**
     * Returns a hash code derived from this target info's four resolved
     * identifiers.
     *
     * @return the hash code of the four identifiers
     */
    @Override
    public int hashCode() {
        return Objects.hash(businessId, businessPaymentAccountId, adAccountId, pageId);
    }

    /**
     * Returns whether this target info is equal to the given object.
     *
     * <p>Two target infos are considered equal when all four resolved
     * identifiers match.
     *
     * @param other the object to compare with
     * @return {@code true} if the other object is a
     *         {@code BusinessBroadcastTargetInfo} with the same identifiers
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BusinessBroadcastTargetInfo that
                && Objects.equals(this.businessId, that.businessId)
                && Objects.equals(this.businessPaymentAccountId, that.businessPaymentAccountId)
                && Objects.equals(this.adAccountId, that.adAccountId)
                && Objects.equals(this.pageId, that.pageId);
    }

    /**
     * Returns a debug string describing this target info.
     *
     * @return a debug string
     */
    @Override
    public String toString() {
        return "BusinessBroadcastTargetInfo[" +
                "businessId=" + businessId +
                ", businessPaymentAccountId=" + businessPaymentAccountId +
                ", adAccountId=" + adAccountId +
                ", pageId=" + pageId +
                "]";
    }
}
