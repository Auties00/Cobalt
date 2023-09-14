package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum PaymentServiceType implements ProtobufEnum {
    /**
     * Unknown service provider
     */
    UNKNOWN(0),
    /**
     * Facebook Pay
     */
    FACEBOOK_PAY(1),
    /**
     * Novi
     */
    NOVI(2),
    /**
     * Upi
     */
    UPI(3);

    final int index;

    PaymentServiceType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
