package com.github.auties00.cobalt.model.proto.sync;

import com.github.auties00.cobalt.model.proto.message.model.ChatMessageKey;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.INT64;
import static it.auties.protobuf.model.ProtobufType.MESSAGE;

@ProtobufMessage(name = "SyncActionMessage")
public record SyncActionMessage(@ProtobufProperty(index = 1, type = MESSAGE) ChatMessageKey key,
                                @ProtobufProperty(index = 2, type = INT64) Long timestamp) {
}
