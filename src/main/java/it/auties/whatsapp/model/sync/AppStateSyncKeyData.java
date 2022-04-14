package it.auties.whatsapp.model.sync;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

import it.auties.protobuf.api.model.ProtobufMessage;
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
public class AppStateSyncKeyData implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = BYTES)
  private byte[] keyData;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = AppStateSyncKeyFingerprint.class)
  private AppStateSyncKeyFingerprint fingerprint;

  @ProtobufProperty(index = 3, type = INT64)
  private long timestamp;
}
