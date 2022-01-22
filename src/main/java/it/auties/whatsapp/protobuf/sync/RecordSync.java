package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.binary.BinaryArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class RecordSync implements ParsableMutation {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdIndex")
  private IndexSync index;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdValue")
  private ValueSync value;

  @JsonProperty("3")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @Override
  public byte[] id() {
    return keyId().id();
  }

  @Override
  public BinaryArray valueBlob() {
    return BinaryArray.of(value.blob());
  }

  @Override
  public BinaryArray indexBlob() {
    return BinaryArray.of(index.blob());
  }
}
