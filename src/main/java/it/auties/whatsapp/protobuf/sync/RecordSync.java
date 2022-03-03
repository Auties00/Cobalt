package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.bytes.Bytes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class RecordSync implements ParsableMutation {
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
  public Bytes valueBlob() {
    return Bytes.of(value.blob());
  }

  @Override
  public Bytes indexBlob() {
    return Bytes.of(index.blob());
  }
}
