package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalInt;

/**
 * Pending Click-to-WhatsApp marketing-messages campaign declared by the
 * client when asking for a fresh quote.
 *
 * <p>The metered-messaging quote endpoint factors the impact of every
 * campaign the user has already drafted but not yet dispatched: each
 * pending campaign reserves a number of free messages from the monthly
 * quota plus an optional dispatch timestamp that influences how the
 * quote is amortised across the billing window. The client therefore
 * sends the relay an array of these entries so the quote can deduct
 * the reserved counts before computing the new charge.
 */
@ProtobufMessage(name = "BusinessMeteredMessagingPendingCampaign")
public final class BusinessMeteredMessagingPendingCampaign {
    /**
     * Number of free reserved messages this previously-issued campaign
     * holds against the monthly quota.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int freeReservedMsgs;

    /**
     * Optional dispatch timestamp (epoch seconds). May be {@code null}
     * when the campaign has not yet been scheduled.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer sendTimestamp;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param freeReservedMsgs the reserved-message count
     * @param sendTimestamp    the optional send timestamp
     */
    BusinessMeteredMessagingPendingCampaign(int freeReservedMsgs, Integer sendTimestamp) {
        this.freeReservedMsgs = freeReservedMsgs;
        this.sendTimestamp = sendTimestamp;
    }

    /**
     * Returns the reserved-message count.
     *
     * @return the count
     */
    public int freeReservedMsgs() {
        return freeReservedMsgs;
    }

    /**
     * Returns the optional dispatch timestamp.
     *
     * @return an {@link OptionalInt} carrying the epoch-second
     *         timestamp, or empty when the campaign has not yet been
     *         scheduled
     */
    public OptionalInt sendTimestamp() {
        if (sendTimestamp == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(sendTimestamp);
    }
}
