package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Server-side view of a business account's eligibility for the
 * marketing-messages feature.
 *
 * <p>Marketing messages are paid broadcast campaigns that a small
 * business can send to its opted-in customers. Each account passes
 * through a server-driven enrolment funnel before the feature is fully
 * unlocked; the relay surfaces the funnel state plus, when the feature
 * is provisional, the epoch second at which the current allowlist
 * expires so the client can show a "X days left" banner.
 */
@ProtobufMessage(name = "BusinessMarketingMessagesEligibility")
public final class BusinessMarketingMessagesEligibility {
    /**
     * The four-way enrolment status.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    BusinessMarketingMessagesStatus status;

    /**
     * Optional expiration timestamp (epoch seconds) for the current
     * allowlist. {@code null} when the relay omitted the attribute.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer expiration;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param status     the enrolment status
     * @param expiration the optional expiration timestamp; may be
     *                   {@code null}
     */
    BusinessMarketingMessagesEligibility(BusinessMarketingMessagesStatus status, Integer expiration) {
        this.status = status;
        this.expiration = expiration;
    }

    /**
     * Returns the enrolment status.
     *
     * @return the status; never {@code null} for a parsed projection
     */
    public BusinessMarketingMessagesStatus status() {
        return status;
    }

    /**
     * Returns the optional expiration timestamp.
     *
     * @return an {@link OptionalInt} carrying the epoch-second
     *         expiration, or empty when the relay omitted the attribute
     */
    public OptionalInt expiration() {
        if (expiration == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(expiration);
    }
}
