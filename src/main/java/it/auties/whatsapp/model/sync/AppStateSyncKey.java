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
public class AppStateSyncKey implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = AppStateSyncKeyId.class)
  private AppStateSyncKeyId keyId;

  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = AppStateSyncKeyData.class)
  private AppStateSyncKeyData keyData;
}
