package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PushName implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = STRING)
  private String id;

  @ProtobufProperty(index = 2, type = STRING)
  private String name;
}
