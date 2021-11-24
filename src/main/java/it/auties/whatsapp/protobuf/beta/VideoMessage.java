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
public class VideoMessage {

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("bool")
  private boolean viewOnce;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("VideoMessageAttribution")
  private VideoMessageAttribution gifAttribution;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] streamingSidecar;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "14", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "13", required = false)
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "12", required = false)
  @JsonPropertyDescription("InteractiveAnnotation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "11", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("bool")
  private boolean gifPlayback;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("uint32")
  private int seconds;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("uint64")
  private long fileLength;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] fileSha256;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String mimetype;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String url;

  @Accessors(fluent = true)
  public enum VideoMessageAttribution {
    NONE(0),
    GIPHY(1),
    TENOR(2);

    private final @Getter int index;

    VideoMessageAttribution(int index) {
      this.index = index;
    }

    @JsonCreator
    public static VideoMessageAttribution forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
