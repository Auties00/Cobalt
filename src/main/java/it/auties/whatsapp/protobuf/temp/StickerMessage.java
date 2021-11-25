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
public class StickerMessage {
  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] pngThumbnail;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("bool")
  private boolean isAnimated;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("bytes")
  private byte[] firstFrameSidecar;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("uint32")
  private int firstFrameLength;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String url;
}
