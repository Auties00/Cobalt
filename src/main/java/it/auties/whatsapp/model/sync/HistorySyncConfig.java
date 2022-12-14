package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("HistorySyncConfig")
public class HistorySyncConfig implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "fullSyncDaysLimit", type = ProtobufType.UINT32)
    private int fullSyncDaysLimit;

    @ProtobufProperty(index = 2, name = "fullSyncSizeMbLimit", type = ProtobufType.UINT32)
    private int fullSyncSizeMbLimit;

    @ProtobufProperty(index = 3, name = "storageQuotaMb", type = ProtobufType.UINT32)
    private int storageQuotaMb;
}
