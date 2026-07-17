package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-side view of a business account's eligibility for the
 * Meta-Verified subscription.
 *
 * <p>Meta-Verified is the paid identity-confirmation badge offered by
 * Meta to small business owners. Before the WhatsApp Business client lets
 * the user start the subscription flow, it asks the relay whether the
 * account meets the prerequisites and what onboarding interstitials must
 * be surfaced first.
 */
@ProtobufMessage(name = "BusinessMetaVerifiedEligibility")
public final class BusinessMetaVerifiedEligibility {
    /**
     * Whether the relay reports the account as eligible to subscribe to
     * Meta-Verified. {@code true} when the account passes the
     * server-side eligibility checks; {@code false} when it does not.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    boolean eligible;

    /**
     * Whether the privacy-interstitial must be surfaced to new users
     * before the subscription flow can start. May be {@code null} when
     * the relay omitted the toggle.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    Boolean shouldShowPrivacyInterstitialToNewUsers;

    /**
     * Optional opaque server-side payload that the WhatsApp Business
     * client forwards verbatim into the Meta-Verified onboarding flow.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String additionalParams;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param eligible                                whether the account
     *                                                is eligible
     * @param shouldShowPrivacyInterstitialToNewUsers the optional
     *                                                privacy-interstitial
     *                                                toggle
     * @param additionalParams                        the optional opaque
     *                                                params
     */
    BusinessMetaVerifiedEligibility(boolean eligible,
                                    Boolean shouldShowPrivacyInterstitialToNewUsers,
                                    String additionalParams) {
        this.eligible = eligible;
        this.shouldShowPrivacyInterstitialToNewUsers = shouldShowPrivacyInterstitialToNewUsers;
        this.additionalParams = additionalParams;
    }

    /**
     * Returns whether the account is eligible for Meta-Verified.
     *
     * @return {@code true} when the account passes the eligibility
     *         checks, {@code false} when it does not
     */
    public boolean eligible() {
        return eligible;
    }

    /**
     * Returns the optional privacy-interstitial toggle.
     *
     * @return an {@link Optional} carrying the boolean toggle, or empty
     *         when the relay omitted it
     */
    public Optional<Boolean> shouldShowPrivacyInterstitialToNewUsers() {
        return Optional.ofNullable(shouldShowPrivacyInterstitialToNewUsers);
    }

    /**
     * Returns the optional opaque additional-params payload.
     *
     * @return an {@link Optional} carrying the payload, or empty when
     *         the relay omitted it
     */
    public Optional<String> additionalParams() {
        return Optional.ofNullable(additionalParams);
    }
}
