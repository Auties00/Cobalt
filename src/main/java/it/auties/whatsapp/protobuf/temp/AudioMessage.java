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
public class AudioMessage {
  @JsonProperty(value = "18")
  @JsonPropertyDescription("bytes")
  private byte[] streamingSidecar;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bool")
  private boolean ptt;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint32")
  private int seconds;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String url;
}
