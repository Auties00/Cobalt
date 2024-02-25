package it.auties.whatsapp.model.call;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum CallStatus implements ProtobufEnum {
    RINGING(0),
    ACCEPTED(1),
    REJECTED(2),
    TIMED_OUT(3);

    final int index;

    CallStatus(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
