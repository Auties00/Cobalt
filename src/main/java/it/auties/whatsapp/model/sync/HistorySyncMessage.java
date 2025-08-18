package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.ChatMessageInfo;

import java.util.Objects;

@ProtobufMessage(name = "HistorySyncMsg")
public record HistorySyncMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        ChatMessageInfo messageInfo,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long messageOrderId
) {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof HistorySyncMessage that && Objects.equals(messageInfo, that.messageInfo());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageInfo);
    }
}