package com.github.auties00.cobalt.wire.linked.business.waa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Eligibility verdict for the caller's WhatsApp Ads ad-account.
 *
 * <p>Before a merchant may enter a Click-to-WhatsApp advertising flow the server
 * runs a per-flow eligibility check on the caller's ad account. The result is a
 * single server-defined verdict marker the WhatsApp client compares against the
 * literal {@code "DENY"} to gate entry: any other marker is treated as eligible.
 *
 * <p>This model is that verdict. The marker is exposed as a raw string because
 * the server value set is not fully recoverable from the WhatsApp client.
 */
@ProtobufMessage(name = "WhatsAppAdsEligibility")
public final class WhatsAppAdsEligibility {
    /**
     * Server-defined eligibility verdict marker. {@code null} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String verdict;

    /**
     * Constructs a new {@code WhatsAppAdsEligibility}.
     *
     * @param verdict the eligibility verdict marker, or {@code null}
     */
    WhatsAppAdsEligibility(String verdict) {
        this.verdict = verdict;
    }

    /**
     * Returns the server-defined eligibility verdict marker.
     *
     * @return the verdict marker, or empty when the server omitted it
     */
    public Optional<String> verdict() {
        return Optional.ofNullable(verdict);
    }

    /**
     * Returns whether the caller is eligible to enter the advertising flow.
     *
     * <p>The WhatsApp client treats any verdict other than the literal
     * {@code "DENY"} as eligible; an absent verdict is treated as ineligible.
     *
     * @return {@code true} when the verdict is present and not the literal
     *         {@code "DENY"}, {@code false} otherwise
     */
    public boolean eligible() {
        return verdict != null && !"DENY".equals(verdict);
    }
}
