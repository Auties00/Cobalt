package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum GroupInviteType implements ProtobufEnum {
    DEFAULT(0),
    PARENT(1);

    final int index;

    GroupInviteType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
