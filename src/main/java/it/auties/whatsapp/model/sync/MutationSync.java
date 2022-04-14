package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MutationSync implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = RecordSync.Operation.class)
  private RecordSync.Operation operation;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = RecordSync.class)
  private RecordSync record;
}
