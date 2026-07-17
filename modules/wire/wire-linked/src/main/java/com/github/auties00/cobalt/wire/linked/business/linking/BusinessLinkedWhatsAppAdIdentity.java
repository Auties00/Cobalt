package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Linked WhatsApp Ad-account identity projection within a
 * {@link BusinessLinkedAccounts} bundle.
 *
 * <p>The ad identity represents the Meta-side Click-to-WhatsApp ad
 * account that may bill campaigns to this business. The relay exposes
 * the identifier plus the same two boolean ad-status flags surfaced on
 * the Facebook Page projection so the client can decide whether to show
 * the "create ad" entry point.
 */
@ProtobufMessage(name = "BusinessLinkedWhatsAppAdIdentity")
public final class BusinessLinkedWhatsAppAdIdentity {
    /**
     * The opaque WhatsApp Ad-account identifier.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Whether this ad identity has ever published a Click-to-WhatsApp
     * ad.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean hasCreatedAd;

    /**
     * Whether this ad identity currently has at least one active
     * Click-to-WhatsApp ad.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    boolean hasActiveCtwaAd;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param id              the ad-account identifier
     * @param hasCreatedAd    whether the identity has ever published an
     *                        ad
     * @param hasActiveCtwaAd whether the identity currently has an
     *                        active CTWA ad
     */
    BusinessLinkedWhatsAppAdIdentity(String id, boolean hasCreatedAd, boolean hasActiveCtwaAd) {
        this.id = id;
        this.hasCreatedAd = hasCreatedAd;
        this.hasActiveCtwaAd = hasActiveCtwaAd;
    }

    /**
     * Returns the WhatsApp Ad-account identifier.
     *
     * @return the identifier; never {@code null} for a parsed
     *         projection
     */
    public String id() {
        return id;
    }

    /**
     * Returns whether this ad identity has ever published a CTWA ad.
     *
     * @return {@code true} when the identity has ever published an ad
     */
    public boolean hasCreatedAd() {
        return hasCreatedAd;
    }

    /**
     * Returns whether this ad identity currently has an active CTWA
     * ad.
     *
     * @return {@code true} when at least one CTWA ad is currently
     *         running on this identity
     */
    public boolean hasActiveCtwaAd() {
        return hasActiveCtwaAd;
    }
}
