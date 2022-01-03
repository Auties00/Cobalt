package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class ExternalBlobReference {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String handle;

  @JsonProperty("4")
  @JsonPropertyDescription("uint64")
  private long fileSizeBytes;

  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty("6")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;
}
