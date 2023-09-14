package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum ButtonsResponseType implements ProtobufEnum {
    UNKNOWN(0),
    SELECTED_DISPLAY_TEXT(1);

    final int index;

    ButtonsResponseType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
