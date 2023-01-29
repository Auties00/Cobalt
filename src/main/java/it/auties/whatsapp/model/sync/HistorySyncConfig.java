package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.UINT32;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("HistorySyncConfig")
public class HistorySyncConfig
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, name = "fullSyncDaysLimit", type = UINT32)
  private int fullSyncDaysLimit;

  @ProtobufProperty(index = 2, name = "fullSyncSizeMbLimit", type = UINT32)
  private int fullSyncSizeMbLimit;

  @ProtobufProperty(index = 3, name = "storageQuotaMb", type = UINT32)
  private int storageQuotaMb;
}
