package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

@ProtobufMessage(name = "DeviceProps.HistorySyncConfig")
public record HistorySyncConfig(
        @ProtobufProperty(index = 1, type = ProtobufType.UINT32)
        Integer fullSyncDaysLimit,
        @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
        Integer fullSyncSizeMbLimit,
        @ProtobufProperty(index = 3, type = ProtobufType.UINT32)
        Integer storageQuotaMb,
        @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
        Boolean inlineInitialPayloadInE2EeMsg,
        @ProtobufProperty(index = 5, type = ProtobufType.UINT32)
        Integer recentSyncDaysLimit,
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        Boolean supportCallLogHistory,
        @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
        Boolean supportBotUserAgentChatHistory
) {

}
