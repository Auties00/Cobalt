package com.github.auties00.cobalt.model.auth;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

@ProtobufEnum
public enum ADVEncryptionType {
    E2EE(0),
    HOSTED(1);

    final int index;
    ADVEncryptionType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }
}
