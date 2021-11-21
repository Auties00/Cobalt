package it.auties.whatsapp.protobuf.model.syncd;

import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty(value = "6")
  private byte[] fileEncSha256;

  @JsonProperty(value = "5")
  private byte[] fileSha256;

  @JsonProperty(value = "4")
  private long fileSizeBytes;

  @JsonProperty(value = "3")
  private String handle;

  @JsonProperty(value = "2")
  private String directPath;

  @JsonProperty(value = "1")
  private byte[] mediaKey;
}
