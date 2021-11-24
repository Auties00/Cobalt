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
public class DocumentMessage {

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("uint32")
  private int thumbnailWidth;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("uint32")
  private int thumbnailHeight;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "15", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("bool")
  private boolean contactVcard;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("string")
  private String fileName;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint32")
  private int pageCount;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String url;
}
