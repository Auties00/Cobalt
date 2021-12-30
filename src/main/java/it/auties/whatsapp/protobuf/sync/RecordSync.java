package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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
public class RecordSync {
  @JsonProperty("1")
  @JsonPropertyDescription("SyncdIndex")
  private IndexSync index;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncdValue")
  private ValueSync value;

  @JsonProperty("3")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;
}
