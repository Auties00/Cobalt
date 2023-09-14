package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;


public record HistorySyncConfig(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        int fullSyncDaysLimit,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        int fullSyncSizeMbLimit,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        int storageQuotaMb
) implements ProtobufMessage {

}
