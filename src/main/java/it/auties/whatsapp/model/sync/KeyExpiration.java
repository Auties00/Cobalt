package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.base.ProtobufType.INT32;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
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
public class KeyExpiration
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = INT32)
  private int expiredKeyEpoch;
}
