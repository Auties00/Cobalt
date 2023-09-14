package it.auties.whatsapp.model.message.payment;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum PaymentOrderSurface implements ProtobufEnum {
    /**
     * Catalog
     */
    CATALOG(1);

    final int index;

    PaymentOrderSurface(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
