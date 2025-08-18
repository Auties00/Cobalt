package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

@ProtobufEnum(name = "KeepType")
public enum KeepInChatType {
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
