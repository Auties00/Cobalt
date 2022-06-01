package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Objects;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class VersionSync implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = UINT64)
  private Long version;
}
