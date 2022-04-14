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
public class AppStateSyncKeyFingerprint implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = UINT32)
  private int rawId;

  @ProtobufProperty(index = 2, type = UINT32)
  private int currentIndex;

  @ProtobufProperty(index = 3, type = UINT32, repeated = true, packed = true)
  private List<Integer> deviceIndexes;

  public static class AppStateSyncKeyFingerprintBuilder {
    public AppStateSyncKeyFingerprintBuilder deviceIndexes(List<Integer> deviceIndexes) {
      if (this.deviceIndexes == null) this.deviceIndexes = new ArrayList<>();
      this.deviceIndexes.addAll(deviceIndexes);
      return this;
    }
  }
}