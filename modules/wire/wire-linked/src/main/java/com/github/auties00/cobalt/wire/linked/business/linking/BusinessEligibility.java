package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Server-side view of a WhatsApp Business account's eligibility for the
 * Meta-Verified, marketing-messages and GenAI feature surfaces.
 *
 * <p>Each of the three feature surfaces is gated behind a separate
 * server-driven onboarding state. The relay exposes that state through
 * three optional projections on a single response stanza so the client can
 * fetch them in one round trip:
 *
 * <ul>
 *   <li>{@linkplain #metaVerified() Meta-Verified} — whether the business
 *       can subscribe to Meta-Verified and which onboarding interstitials
 *       must be surfaced.</li>
 *   <li>{@linkplain #marketingMessages() marketing messages} — whether the
 *       business can send paid marketing campaigns and when its existing
 *       allowlist expires.</li>
 *   <li>{@linkplain #genai() GenAI} — whether the business can use the
 *       opt-in GenAI assistant features.</li>
 * </ul>
 *
 * <p>Each projection is independently optional: the client tells the relay
 * which subset of feature toggles to evaluate at request time, and only
 * the toggles the request asked for come back populated.
 */
@ProtobufMessage(name = "BusinessEligibility")
public final class BusinessEligibility {
    /**
     * Optional Meta-Verified eligibility projection.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    BusinessMetaVerifiedEligibility metaVerified;

    /**
     * Optional marketing-messages eligibility projection.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    BusinessMarketingMessagesEligibility marketingMessages;

    /**
     * Optional GenAI eligibility projection.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    BusinessGenaiEligibility genai;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param metaVerified      the optional Meta-Verified projection
     * @param marketingMessages the optional marketing-messages projection
     * @param genai             the optional GenAI projection
     */
    BusinessEligibility(BusinessMetaVerifiedEligibility metaVerified,
                        BusinessMarketingMessagesEligibility marketingMessages,
                        BusinessGenaiEligibility genai) {
        this.metaVerified = metaVerified;
        this.marketingMessages = marketingMessages;
        this.genai = genai;
    }

    /**
     * Returns the optional Meta-Verified eligibility projection.
     *
     * @return an {@link Optional} carrying the projection, or empty when
     *         the request did not ask for it or the relay omitted it
     */
    public Optional<BusinessMetaVerifiedEligibility> metaVerified() {
        return Optional.ofNullable(metaVerified);
    }

    /**
     * Returns the optional marketing-messages eligibility projection.
     *
     * @return an {@link Optional} carrying the projection, or empty when
     *         the request did not ask for it or the relay omitted it
     */
    public Optional<BusinessMarketingMessagesEligibility> marketingMessages() {
        return Optional.ofNullable(marketingMessages);
    }

    /**
     * Returns the optional GenAI eligibility projection.
     *
     * @return an {@link Optional} carrying the projection, or empty when
     *         the request did not ask for it or the relay omitted it
     */
    public Optional<BusinessGenaiEligibility> genai() {
        return Optional.ofNullable(genai);
    }
}
