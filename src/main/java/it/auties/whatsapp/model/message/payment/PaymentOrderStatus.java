package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum PaymentOrderStatus implements ProtobufEnum {
    /**
     * Inquiry
     */
    INQUIRY(1);

    final int index;

    PaymentOrderStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
