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
public class SnapshotSync implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = MESSAGE, concreteType = VersionSync.class)
  private VersionSync version;

  @ProtobufProperty(index = 2, type = MESSAGE,
          concreteType = RecordSync.class, repeated = true)
  private List<RecordSync> records;

  @ProtobufProperty(index = 3, type = BYTES)
  private byte[] mac;

  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = KeyId.class)
  private KeyId keyId;

  public static class SnapshotSyncBuilder {
    public SnapshotSyncBuilder records(List<RecordSync> records) {
      if (this.records == null) this.records = new ArrayList<>();
      this.records.addAll(records);
      return this;
    }
  }
}