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
public class ButtonsMessage {
  @JsonProperty(value = "5")
  @JsonPropertyDescription("LocationMessage")
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  @JsonPropertyDescription("VideoMessage")
  private VideoMessage videoMessage;

  @JsonProperty(value = "3")
  @JsonPropertyDescription("ImageMessage")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  @JsonPropertyDescription("DocumentMessage")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "1")
  @JsonPropertyDescription("string")
  private String text;

  @JsonProperty(value = "10")
  @JsonPropertyDescription("ButtonsMessageHeaderType")
  private ButtonsMessageHeaderType headerType;

  @JsonProperty(value = "9")
  @JsonPropertyDescription("Button")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Button> buttons;

  @JsonProperty(value = "8")
  @JsonPropertyDescription("ContextInfo")
  private ContextInfo contextInfo;

  @JsonProperty(value = "7")
  @JsonPropertyDescription("string")
  private String footerText;

  @JsonProperty(value = "6")
  @JsonPropertyDescription("string")
  private String contentText;

  @Accessors(fluent = true)
  public enum ButtonsMessageHeaderType {
    UNKNOWN(0),
    EMPTY(1),
    TEXT(2),
    DOCUMENT(3),
    IMAGE(4),
    VIDEO(5),
    LOCATION(6);

    private final @Getter int index;

    ButtonsMessageHeaderType(int index) {
      this.index = index;
    }

    @JsonCreator
    public static ButtonsMessageHeaderType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(null);
    }
  }

  public Header headerCase() {
    if (text != null) return Header.TEXT;
    if (documentMessage != null) return Header.DOCUMENT_MESSAGE;
    if (imageMessage != null) return Header.IMAGE_MESSAGE;
    if (videoMessage != null) return Header.VIDEO_MESSAGE;
    if (locationMessage != null) return Header.LOCATION_MESSAGE;
    return Header.UNKNOWN;
  }

  @Accessors(fluent = true)
  public enum Header {
    UNKNOWN(0),
    TEXT(1),
    DOCUMENT_MESSAGE(2),
    IMAGE_MESSAGE(3),
    VIDEO_MESSAGE(4),
    LOCATION_MESSAGE(5);

    private final @Getter int index;

    Header(int index) {
      this.index = index;
    }

    @JsonCreator
    public static Header forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(Header.UNKNOWN);
    }
  }
}
