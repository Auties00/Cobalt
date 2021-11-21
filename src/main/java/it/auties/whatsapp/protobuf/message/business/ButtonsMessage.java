package it.auties.whatsapp.protobuf.message.business;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.standard.DocumentMessage;
import it.auties.whatsapp.protobuf.message.standard.ImageMessage;
import it.auties.whatsapp.protobuf.message.standard.LocationMessage;
import it.auties.whatsapp.protobuf.message.standard.VideoMessage;
import it.auties.whatsapp.protobuf.model.button.Button;
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
  private LocationMessage locationMessage;

  @JsonProperty(value = "4")
  private VideoMessage videoMessage;

  @JsonProperty(value = "3")
  private ImageMessage imageMessage;

  @JsonProperty(value = "2")
  private DocumentMessage documentMessage;

  @JsonProperty(value = "1")
  private String text;

  @JsonProperty(value = "10")
  private ButtonsMessageHeaderType headerType;

  @JsonProperty(value = "9")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  private List<Button> buttons;

  @JsonProperty(value = "8")
  private ContextInfo contextInfo;

  @JsonProperty(value = "7")
  private String footerText;

  @JsonProperty(value = "6")
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
