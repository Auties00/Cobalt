package it.auties.whatsapp.protobuf.beta;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ExternalBlobReference {

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint64")
  private long fileSizeBytes;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String handle;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;
}
