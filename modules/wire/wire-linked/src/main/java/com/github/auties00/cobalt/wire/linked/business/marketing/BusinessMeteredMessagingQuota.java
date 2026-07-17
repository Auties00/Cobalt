package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Snapshot of the monthly free-message quota an SMB metered-messaging
 * account currently has.
 *
 * <p>The quota is a count of free outbound messages the relay grants
 * each billing window. The {@linkplain #remaining() remaining} field
 * shows how many of those free messages have not yet been consumed,
 * the {@linkplain #totalMonthly() total} field shows the size of the
 * monthly allocation, and the optional
 * {@linkplain #singleCredits() single-credit} and
 * {@linkplain #totalAvailableCredits() total-available-credits} fields
 * surface promotional one-shot credits the relay has additionally
 * issued to this account.
 */
@ProtobufMessage(name = "BusinessMeteredMessagingQuota")
public final class BusinessMeteredMessagingQuota {
    /**
     * Remaining free messages this billing window.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int remaining;

    /**
     * Size of the monthly free-message allocation.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    int totalMonthly;

    /**
     * Optional one-shot single-credits balance.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    Integer singleCredits;

    /**
     * Optional total-available-credits projection (sum of monthly
     * remaining + promotional one-shot credits).
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    Integer totalAvailableCredits;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param remaining             the remaining quota
     * @param totalMonthly          the monthly allocation
     * @param singleCredits         the optional single-credits balance
     * @param totalAvailableCredits the optional total credits
     */
    BusinessMeteredMessagingQuota(int remaining, int totalMonthly,
                             Integer singleCredits, Integer totalAvailableCredits) {
        this.remaining = remaining;
        this.totalMonthly = totalMonthly;
        this.singleCredits = singleCredits;
        this.totalAvailableCredits = totalAvailableCredits;
    }

    /**
     * Returns the remaining free-message count.
     *
     * @return the remaining quota
     */
    public int remaining() {
        return remaining;
    }

    /**
     * Returns the monthly free-message allocation size.
     *
     * @return the monthly allocation
     */
    public int totalMonthly() {
        return totalMonthly;
    }

    /**
     * Returns the optional single-credits balance.
     *
     * @return an {@link OptionalInt} carrying the balance, or empty
     *         when the relay omitted it
     */
    public OptionalInt singleCredits() {
        return singleCredits == null ? OptionalInt.empty() : OptionalInt.of(singleCredits);
    }

    /**
     * Returns the optional total-available-credits projection.
     *
     * @return an {@link OptionalInt} carrying the credit, or empty
     *         when the relay omitted it
     */
    public OptionalInt totalAvailableCredits() {
        return totalAvailableCredits == null ? OptionalInt.empty() : OptionalInt.of(totalAvailableCredits);
    }
}
