package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.MESSAGE;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.model.MessageKey;
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
public class SyncActionMessage
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = MESSAGE, implementation = MessageKey.class)
  private MessageKey key;

  @ProtobufProperty(index = 2, type = INT64)
  private Long timestamp;
}
