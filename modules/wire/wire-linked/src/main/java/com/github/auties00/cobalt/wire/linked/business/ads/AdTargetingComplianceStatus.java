package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Verdict on whether a WhatsApp Business ad targeting spec is subject to a
 * country or region's advertising-disclosure rules.
 *
 * <p>Several jurisdictions require WhatsApp Business merchants to surface
 * extra transparency information about who is shown a "Click-to-WhatsApp"
 * ad (the paid promotions that open a chat with the business when tapped)
 * before the merchant is allowed to publish. Each set of rules is keyed off
 * the ad targeting spec: who the merchant wants to reach. Before showing
 * the disclosure controls the WhatsApp Business client asks the server
 * whether a given targeting spec falls under those rules.
 *
 * <p>This model collapses that verdict into one shape so a caller checks
 * {@link #subject()} regardless of which jurisdiction's rules it queried
 * (the EU Digital Services Act, the Taiwan financial-services rules, the
 * Australian financial-services rules, the Singapore universal-advertising
 * rules, or the India financial-services rules).
 */
@ProtobufMessage(name = "AdTargetingComplianceStatus")
public final class AdTargetingComplianceStatus {
    /**
     * Whether the targeting spec is subject to the queried jurisdiction's
     * advertising-disclosure rules. {@code false} both when the server
     * explicitly reported the spec as outside the rules and when it
     * omitted the verdict entirely.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean subject;

    /**
     * Constructs a new {@code AdTargetingComplianceStatus}.
     *
     * @param subject whether the targeting spec is subject to the queried
     *                jurisdiction's advertising-disclosure rules
     */
    AdTargetingComplianceStatus(boolean subject) {
        this.subject = subject;
    }

    /**
     * Returns whether the targeting spec is subject to the queried
     * jurisdiction's advertising-disclosure rules.
     *
     * @return {@code true} when the server reported the spec subject to the
     *         rules
     */
    public boolean subject() {
        return subject;
    }
}
