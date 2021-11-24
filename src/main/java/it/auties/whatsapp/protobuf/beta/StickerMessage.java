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
public class StickerMessage {

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] pngThumbnail;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("bool")
  private boolean isAnimated;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] firstFrameSidecar;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("uint32")
  private int firstFrameLength;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String url;
}
