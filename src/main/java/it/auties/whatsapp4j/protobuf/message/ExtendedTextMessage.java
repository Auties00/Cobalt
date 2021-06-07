package it.auties.whatsapp4j.protobuf.message;

import com.fasterxml.jackson.annotation.*;
import java.util.*;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * A model class that represents a WhatsappMessage sent by a contact and that holds text inside.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class ExtendedTextMessage implements ContextualMessage {
  @JsonProperty(value = "18")
  private boolean doNotPlayInline;

  @JsonProperty(value = "17")
  private ContextInfo contextInfo;

  @JsonProperty(value = "16")
  private byte[] jpegThumbnail;

  @JsonProperty(value = "10")
  private ExtendedTextMessagePreviewType previewType;

  @JsonProperty(value = "9")
  private ExtendedTextMessageFontType font;

  @JsonProperty(value = "8")
  private int backgroundArgb;

  @JsonProperty(value = "7")
  private int textArgb;

  @JsonProperty(value = "6")
  private String title;

  @JsonProperty(value = "5")
  private String description;

  @JsonProperty(value = "4")
  private String canonicalUrl;

  @JsonProperty(value = "2")
  private String matchedText;

  @JsonProperty(value = "1")
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
