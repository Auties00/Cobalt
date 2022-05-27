package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateSyncKeyFingerprint implements ProtobufMessage {
  @ProtobufProperty(index = 1, type = UINT32)
  private Integer rawId;

  @ProtobufProperty(index = 2, type = UINT32)
  private Integer currentIndex;

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