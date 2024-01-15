package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.model.ProtobufEnum;

@ProtobufMessageName("KeepType")
public enum KeepInChatType implements ProtobufEnum {
    UNKNOWN(0),
    KEEP_FOR_ALL(1),
    UNDO_KEEP_FOR_ALL(2);

    final int index;

    KeepInChatType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
