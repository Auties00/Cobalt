package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.protobuf.media.AttachmentProvider;
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
public final class ExternalBlobReference implements AttachmentProvider {
  @JsonProperty("1")
  @JsonPropertyDescription("bytes")
  private byte[] key; 

  @JsonProperty("2")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty("3")
  @JsonPropertyDescription("string")
  private String handle;

  @JsonProperty("4")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty("5")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty("6")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @Override
  public String url() {
    return null;
  }

  @Override
  public String name() {
    return "md-app-state";
  }

  @Override
  public String keyName() {
    return "WhatsApp App State Keys";
  }
}
