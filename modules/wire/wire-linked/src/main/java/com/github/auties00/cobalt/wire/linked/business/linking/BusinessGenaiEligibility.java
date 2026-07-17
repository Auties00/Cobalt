package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Server-side view of a business account's eligibility for the
 * opt-in WhatsApp GenAI assistant features.
 *
 * <p>The WhatsApp Business GenAI surface bundles together generative
 * suggestion features (smart replies, tone tuning, draft assistance).
 * Before exposing the surface, the relay tells the client whether the
 * account passes the eligibility checks; failed accounts must wait
 * until they meet the criteria the server-side rollout currently
 * enforces.
 */
@ProtobufMessage(name = "BusinessGenaiEligibility")
public final class BusinessGenaiEligibility {
    /**
     * Whether the account is eligible to use the GenAI features.
     * {@code true} when the relay reports a successful eligibility
     * check; {@code false} otherwise.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean eligible;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param eligible whether the account is eligible
     */
    BusinessGenaiEligibility(boolean eligible) {
        this.eligible = eligible;
    }

    /**
     * Returns whether the account is eligible to use the GenAI features.
     *
     * @return {@code true} when the relay reports a successful
     *         eligibility check; {@code false} otherwise
     */
    public boolean eligible() {
        return eligible;
    }
}
