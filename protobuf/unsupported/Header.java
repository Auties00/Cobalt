package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class Header {

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("VideoMessage")
  private VideoMessage videoMessage;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage imageMessage;

  @JsonProperty(value = "3", required = false)
  @JsonPropertyDescription("DocumentMessage")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("bool")
  private boolean hasMediaAttachment;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String subtitle;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String title;

  public Media mediaType() {
    if (documentMessage != null) return Media.DOCUMENT_MESSAGE;
    if (imageMessage != null) return Media.IMAGE_MESSAGE;
    if (jpegThumbnail != null) return Media.JPEG_THUMBNAIL;
    if (videoMessage != null) return Media.VIDEO_MESSAGE;
    return Media.UNKNOWN;
  }

  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum Media {
    UNKNOWN(0),
    DOCUMENT_MESSAGE(3),
    IMAGE_MESSAGE(4),
    JPEG_THUMBNAIL(6),
    VIDEO_MESSAGE(7);

    @Getter
    private final int index;

    @JsonCreator
    public static Media forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Media.UNKNOWN);
    }
  }
}
