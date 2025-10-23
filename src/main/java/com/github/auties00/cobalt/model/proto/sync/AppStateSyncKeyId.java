package com.github.auties00.cobalt.model.proto.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.BYTES;

@ProtobufMessage(name = "Message.AppStateSyncKeyId")
public record AppStateSyncKeyId(
        @ProtobufProperty(index = 1, type = BYTES) byte[] value
) {

}
