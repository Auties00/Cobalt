package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum ButtonBodyType implements ProtobufEnum {
    UNKNOWN(0),
    TEXT(1),
    NATIVE_FLOW(2);

    final int index;
    ButtonBodyType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
