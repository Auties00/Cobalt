package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.INT32;

@ProtobufMessage(name = "SyncActionValue.KeyExpiration")
public record KeyExpiration(
        @ProtobufProperty(index = 1, type = INT32) Integer expiredKeyEpoch) {
}
