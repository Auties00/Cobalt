package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.message.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("KeepInChatMessage")
public final class KeepInChatMessage implements Message {
    @ProtobufProperty(index = 1, name = "key", type = ProtobufType.MESSAGE)
    private MessageKey key;

    @ProtobufProperty(index = 2, name = "keepType", type = ProtobufType.MESSAGE)
    private KeepInChatType keepType;

    @ProtobufProperty(index = 3, name = "timestampMs", type = ProtobufType.INT64)
    private long timestampMilliseconds;

    @Override
    public MessageType type() {
        return MessageType.KEEP_IN_CHAT;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
