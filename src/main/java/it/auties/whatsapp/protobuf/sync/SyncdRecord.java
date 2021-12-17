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
public class SyncdRecord {
  @JsonProperty(value = "3")
  @JsonPropertyDescription("KeyId")
  private KeyId keyId;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SyncdValue")
  private SyncdValue value;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("SyncdIndex")
  private SyncdIndex index;
}
