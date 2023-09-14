package it.auties.whatsapp.model.info;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

/**
 * The constants of this enumerated type describe the currencies supported for a transaction
 * described by a {@link PaymentInfo}
 */
public enum PaymentInfoCurrency implements ProtobufEnum {
    /**
     * Unknown currency
     */
    UNKNOWN_CURRENCY(0),
    /**
     * Indian rupees
     */
    INR(1);

    final int index;

    PaymentInfoCurrency(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
