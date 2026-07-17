package com.github.auties00.cobalt.wire.linked.business.marketing;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Snapshot of an SMB metered-messaging account's running balance.
 *
 * <p>The balance comes back as a {@code (billing, available)} pair:
 * the {@linkplain #billing() billing} field is the amount the relay
 * has already accrued against the account this billing window, and the
 * {@linkplain #available() available} field is the remaining
 * pre-purchased credit. Both values are denominated in
 * currency-minor units and must be interpreted using the
 * {@linkplain #offset() decimal offset}.
 */
@ProtobufMessage(name = "BusinessMeteredMessagingAccountBalance")
public final class BusinessMeteredMessagingAccountBalance {
    /**
     * Amount accrued against the account this billing window
     * (currency-minor units).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    int billing;

    /**
     * Remaining pre-purchased credit (currency-minor units).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    int available;

    /**
     * Decimal offset for the currency (number of fractional digits).
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int offset;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param billing   the billed-this-window amount
     * @param available the remaining credit
     * @param offset    the decimal offset
     */
    BusinessMeteredMessagingAccountBalance(int billing, int available, int offset) {
        this.billing = billing;
        this.available = available;
        this.offset = offset;
    }

    /**
     * Returns the billed-this-window amount in currency-minor units.
     *
     * @return the billed amount
     */
    public int billing() {
        return billing;
    }

    /**
     * Returns the remaining credit in currency-minor units.
     *
     * @return the available credit
     */
    public int available() {
        return available;
    }

    /**
     * Returns the decimal offset for the currency.
     *
     * @return the offset
     */
    public int offset() {
        return offset;
    }
}
