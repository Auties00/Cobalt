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
public class ImageMessage {

  @JsonProperty(value = "28", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "27", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "26", required = false)
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("bool")
  private boolean viewOnce;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileEncSha256;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] midQualityFileSha256;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("uint32")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Integer> scanLengths;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] scansSidecar;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("uint32")
  private int experimentGroupId;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("uint32")
  private int firstScanLength;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] firstScanSidecar;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("InteractiveAnnotation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String url;
}
