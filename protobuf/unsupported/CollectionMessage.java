package it.auties.whatsapp;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufProperty;
import java.util.*;
import lombok.*;
import lombok.experimental.*;
import lombok.extern.jackson.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class CollectionMessage {

  @ProtobufProperty(index = 1, type = STRING)
  private String bizJid;

  @ProtobufProperty(index = 2, type = STRING)
  private String id;

  @ProtobufProperty(index = 3, type = INT32)
  private int messageVersion;
}
