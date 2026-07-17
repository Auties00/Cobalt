package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Linked Meta ad-platform account projection used by a
 * {@link BusinessLinkedAdAccounts} snapshot.
 *
 * <p>Each linked account is identified by its server-issued ad-platform
 * identifier and carries the two booleans describing its click-to-WhatsApp
 * advertising state: whether it currently runs an active CTWA ad and whether
 * it has ever created any ad.
 */
@ProtobufMessage(name = "BusinessLinkedAdAccount")
public final class BusinessLinkedAdAccount {
    /**
     * Server-issued ad-platform account identifier. {@code null} when the
     * relay omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Whether the account currently runs an active click-to-WhatsApp ad.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean hasActiveClickToWhatsAppAd;

    /**
     * Whether the account has ever created an ad.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean hasCreatedAd;

    /**
     * Constructs a new {@code BusinessLinkedAdAccount}.
     *
     * @param id                         the ad-platform account identifier, or
     *                                   {@code null}
     * @param hasActiveClickToWhatsAppAd whether an active CTWA ad is currently
     *                                   running
     * @param hasCreatedAd               whether an ad has ever been created
     */
    BusinessLinkedAdAccount(String id, boolean hasActiveClickToWhatsAppAd, boolean hasCreatedAd) {
        this.id = id;
        this.hasActiveClickToWhatsAppAd = hasActiveClickToWhatsAppAd;
        this.hasCreatedAd = hasCreatedAd;
    }

    /**
     * Returns the server-issued ad-platform account identifier.
     *
     * @return the identifier, or empty when the relay omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns whether the account currently runs an active click-to-WhatsApp
     * ad.
     *
     * @return {@code true} when the relay reported an active ad, {@code false}
     *         otherwise
     */
    public boolean hasActiveClickToWhatsAppAd() {
        return hasActiveClickToWhatsAppAd;
    }

    /**
     * Returns whether the account has ever created an ad.
     *
     * @return {@code true} when the relay reported a created ad, {@code false}
     *         otherwise
     */
    public boolean hasCreatedAd() {
        return hasCreatedAd;
    }
}
