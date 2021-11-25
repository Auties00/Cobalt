package it.auties.whatsapp.protobuf.temp;

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
  @JsonProperty(value = "4")
  @JsonPropertyDescription("int32")
  private int version;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] padding;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("SyncActionValue")
  private SyncActionValue value;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("bytes")
  private byte[] index;
}
