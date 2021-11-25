package it.auties.whatsapp.protobuf.temp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class ExtendedTextMessage {
  @JsonProperty(value = "25")
  @JsonPropertyDescription("uint32")
  private int thumbnailWidth;

  @JsonProperty(value = "24")
  @JsonPropertyDescription("uint32")
  private int thumbnailHeight;

  @JsonProperty(value = "23")
  @JsonPropertyDescription("int64")
  private long mediaKeyTimestamp;

  @JsonProperty(value = "22")
  @JsonPropertyDescription("bytes")
  private byte[] mediaKey;

  @JsonProperty(value = "21")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailEncSha256;

  @JsonProperty(value = "20")
  @JsonPropertyDescription("bytes")
  private byte[] thumbnailSha256;

  @JsonProperty(value = "19")
  @JsonPropertyDescription("string")
  private String thumbnailDirectPath;

  @JsonProperty(value = "18")
  @JsonPropertyDescription("bool")
  private boolean doNotPlayInline;

  @JsonProperty(value = "17")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  @JsonPropertyDescription("bytes")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("ExtendedTextMessagePreviewType")
  private ExtendedTextMessagePreviewType previewType;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("ExtendedTextMessageFontType")
  private ExtendedTextMessageFontType font;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("fixed32")
  private int backgroundArgb;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("fixed32")
  private int textArgb;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("string")
  private String title;

  @JsonProperty(value = "5")
  @JsonPropertyDescription("string")
  private String description;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("string")
  private String canonicalUrl;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("string")
  private String matchedText;

  @JsonProperty(value = "1")
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
