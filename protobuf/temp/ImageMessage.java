package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ImageMessage {
  @JsonProperty(value = "28")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "27")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "26")
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "25")
  @JsonPropertyDescription("bool")
  private boolean viewOnce;

  @JsonProperty(value = "24")
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileEncSha256;

  @JsonProperty(value = "23")
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileSha256;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> scanLengths;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("bytes")
  private byte[] scansSidecar;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("uint32")
  private int experimentGroupId;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("uint32")
  private int firstScanLength;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("bytes")
  private byte[] firstScanSidecar;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("InteractiveAnnotation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String url;
}
