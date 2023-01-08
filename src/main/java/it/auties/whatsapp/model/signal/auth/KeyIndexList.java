package it.auties.whatsapp.model.signal.auth;

import static it.auties.protobuf.base.ProtobufType.UINT32;
import static it.auties.protobuf.base.ProtobufType.UINT64;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("ADVKeyIndexList")
public class KeyIndexList
    implements ProtobufMessage {

  @ProtobufProperty(index = 1, type = UINT32)
  private Integer rawId;

  @ProtobufProperty(index = 2, type = UINT64)
  private Long timestamp;

  @ProtobufProperty(index = 3, type = UINT32)
  private Integer currentIndex;

  @ProtobufProperty(index = 4, type = UINT32, repeated = true, packed = true)
  private List<Integer> validIndexes;

  public static class KeyIndexListBuilder {

    public KeyIndexListBuilder clientFeatures(List<Integer> validIndexes) {
      if (this.validIndexes == null) {
        this.validIndexes = new ArrayList<>();
      }
      this.validIndexes.addAll(validIndexes);
      return this;
    }
  }
}