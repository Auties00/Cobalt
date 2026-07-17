package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Outcome of provisioning the caller's WhatsApp Ads ad account.
 *
 * <p>Entering a Click-to-WhatsApp advertising flow either provisions a new
 * advertising account for the merchant or resumes onboarding of an existing one.
 * The server reports the assigned account identifier together with the
 * onboarding status marker the WhatsApp client uses to decide what to render
 * next.
 *
 * <p>This model is that onboarding result. The {@code accountId} doubles as the
 * success signal: WhatsApp Web treats an absent id as an onboarding failure.
 */
@ProtobufMessage(name = "WhatsAppAdsAdAccount")
public final class WhatsAppAdsAdAccount {
    /**
     * Server-issued advertising account identifier. {@code null} when the server
     * omitted it (which the WhatsApp client treats as an onboarding failure).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String accountId;

    /**
     * Server-defined onboarding status marker. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String status;

    /**
     * Constructs a new {@code WhatsAppAdsAdAccount}.
     *
     * @param accountId the advertising account identifier, or {@code null}
     * @param status    the onboarding status marker, or {@code null}
     */
    WhatsAppAdsAdAccount(String accountId, String status) {
        this.accountId = accountId;
        this.status = status;
    }

    /**
     * Returns the server-issued advertising account identifier.
     *
     * @return the account identifier, or empty when the server omitted it
     */
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    /**
     * Returns the server-defined onboarding status marker.
     *
     * @return the status marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }
}
