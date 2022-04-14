package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MediaData {

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] key; 
}
