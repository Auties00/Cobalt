package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Eligibility verdict for whether the linked WhatsApp Business account may
 * onboard the Meta AI business agent.
 *
 * <p>WhatsApp Business surfaces an AI-tools tile to merchants whose account
 * has been cleared by the server to use the auto-reply assistant. The tile
 * shows or hides the onboarding affordance based on this verdict; merchants
 * whose account is not yet eligible do not see the onboarding entry point.
 *
 * <p>This model is that verdict.
 */
@ProtobufMessage(name = "BusinessAiToolsEligibility")
public final class BusinessAiToolsEligibility {
    /**
     * Whether the linked account may onboard the AI business agent.
     * {@code false} both when the server explicitly reported the account
     * ineligible and when it omitted the verdict entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean eligible;

    /**
     * Constructs a new {@code BusinessAiToolsEligibility}.
     *
     * @param eligible whether the linked account may onboard the AI
     *                 business agent
     */
    BusinessAiToolsEligibility(boolean eligible) {
        this.eligible = eligible;
    }

    /**
     * Returns whether the linked account may onboard the AI business
     * agent.
     *
     * @return {@code true} when the server reported the account eligible
     */
    public boolean eligible() {
        return eligible;
    }
}
