package it.auties.whatsapp.model.info;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class WebNotificationsInfo
    implements Info {

  @ProtobufProperty(index = 2, type = UINT64)
  private long timestamp;

  @ProtobufProperty(index = 3, type = UINT32)
  private int unreadChats;

  @ProtobufProperty(index = 4, type = UINT32)
  private int notifyMessageCount;

  @ProtobufProperty(index = 5, type = MESSAGE, implementation = MessageInfo.class, repeated = true)
  private List<MessageInfo> notifyMessages;
}
