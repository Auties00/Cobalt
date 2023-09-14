package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.model.ProtobufEnum;

public enum InviteLinkGroupType implements ProtobufEnum {
    DEFAULT(0),
    PARENT(1),
    SUB(2),
    DEFAULT_SUB(3);

    final int index;

    InviteLinkGroupType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return this.index;
    }
}
