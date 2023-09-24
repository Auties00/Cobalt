package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.info.MessageInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

@ProtobufMessageName("HistorySyncMsg")
public record HistorySyncMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        @NonNull
        MessageInfo messageInfo,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
        long messageOrderId
) implements ProtobufMessage {

}