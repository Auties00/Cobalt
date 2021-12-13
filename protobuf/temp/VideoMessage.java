package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class VideoMessage {
  @JsonProperty(value = "23")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("bool")
  private boolean viewOnce;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("VideoMessageAttribution")
  private VideoMessageAttribution gifAttribution;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("bytes")
  private byte[] streamingSidecar;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "14")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "13")
  @JsonPropertyDescription("string")
  private String directPath;

  @JsonProperty(value = "12")
  @JsonPropertyDescription("InteractiveAnnotation")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<InteractiveAnnotation> interactiveAnnotations;

  @JsonProperty(value = "11")
  @JsonPropertyDescription("bytes")
  private byte[] fileEncSha256;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("uint32")
  private int width;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("uint32")
  private int height;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("bool")
  private boolean gifPlayback;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String caption;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

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
