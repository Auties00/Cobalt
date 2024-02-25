package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.whatsapp.model.message.model.ChatMessageKey;

import static it.auties.protobuf.model.ProtobufType.INT64;
import static it.auties.protobuf.model.ProtobufType.OBJECT;

@ProtobufMessageName("SyncActionMessage")
public record SyncActionMessage(@ProtobufProperty(index = 1, type = OBJECT) ChatMessageKey key,
                                @ProtobufProperty(index = 2, type = INT64) Long timestamp) implements ProtobufMessage {
}
