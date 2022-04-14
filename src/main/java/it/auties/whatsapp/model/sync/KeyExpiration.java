package it.auties.whatsapp.model.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class KeyExpiration implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = INT32)
  private int expiredKeyEpoch;
}
