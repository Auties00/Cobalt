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
public class ExtendedTextMessage {

  @JsonProperty(value = "25", required = false)
  @JsonPropertyDescription("uint32")
  private int thumbnailWidth;

  @JsonProperty(value = "24", required = false)
  @JsonPropertyDescription("uint32")
  private int thumbnailHeight;

  @JsonProperty(value = "23", required = false)
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "22", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "21", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "20", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "19", required = false)
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "18", required = false)
  @JsonPropertyDescription("bool")
  private boolean doNotPlayInline;

  @JsonProperty(value = "17", required = false)
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16", required = false)
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "10", required = false)
  @JsonPropertyDescription("ExtendedTextMessagePreviewType")
  private ExtendedTextMessagePreviewType previewType;

  @JsonProperty(value = "9", required = false)
  @JsonPropertyDescription("ExtendedTextMessageFontType")
  private ExtendedTextMessageFontType font;

  @JsonProperty(value = "8", required = false)
  @JsonPropertyDescription("fixed32")
  private int backgroundArgb;

  @JsonProperty(value = "7", required = false)
  @JsonPropertyDescription("fixed32")
  private int textArgb;

  @JsonProperty(value = "6", required = false)
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "5", required = false)
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "4", required = false)
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty(value = "2", required = false)
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty(value = "1", required = false)
  @JsonPropertyDescription("string")
  private String text;

  @Accessors(fluent = true)
  public enum ExtendedTextMessageFontType {
    SANS_SERIF(0),
    SERIF(1),
    NORICAN_REGULAR(2),
    BRYNDAN_WRITE(3),
    BEBASNEUE_REGULAR(4),
    OSWALD_HEAVY(5);

    private final @Getter int index;

    ExtendedTextMessageFontType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ExtendedTextMessageFontType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  @Accessors(fluent = true)
  public enum ExtendedTextMessagePreviewType {
    NONE(0),
    VIDEO(1);

    private final @Getter int index;

    ExtendedTextMessagePreviewType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ExtendedTextMessagePreviewType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }
}
