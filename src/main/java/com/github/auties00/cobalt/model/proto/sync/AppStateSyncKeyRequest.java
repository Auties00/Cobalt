package com.github.auties00.cobalt.model.proto.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import java.util.List;

import static it.auties.protobuf.model.ProtobufType.MESSAGE;

@ProtobufMessage(name = "Message.AppStateSyncKeyRequest")
public record AppStateSyncKeyRequest(
        @ProtobufProperty(index = 1, type = MESSAGE) List<AppStateSyncKeyId> keyIds) {
}
