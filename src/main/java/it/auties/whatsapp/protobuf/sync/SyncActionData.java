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
public class SyncActionData {
  @JsonProperty("4")
  @JsonPropertyDescription("int32")
  private int version;

  @JsonProperty("3")
  @JsonPropertyDescription("bytes")
  private byte[] padding;

  @JsonProperty("2")
  @JsonPropertyDescription("SyncActionValue")
  private SyncActionValue value;

  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] index;
}
