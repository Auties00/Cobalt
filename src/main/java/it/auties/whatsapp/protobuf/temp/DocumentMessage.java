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
public class DocumentMessage {
  @JsonProperty(value = "19")
  @JsonPropertyDescription("uint32")
  private int thumbnailWidth;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("uint32")
  private int thumbnailHeight;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "15")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("bool")
  private boolean contactVcard;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("string")
  private String fileName;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("uint32")
  private int pageCount;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String url;
}
