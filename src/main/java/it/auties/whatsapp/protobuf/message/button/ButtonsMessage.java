package it.auties.whatsapp.protobuf.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.protobuf.button.Button;
import it.auties.whatsapp.protobuf.info.ContextInfo;
import it.auties.whatsapp.protobuf.message.model.ButtonMessage;
import it.auties.whatsapp.protobuf.message.model.ContextualMessage;
import it.auties.whatsapp.protobuf.message.standard.DocumentMessage;
import it.auties.whatsapp.protobuf.message.standard.ImageMessage;
import it.auties.whatsapp.protobuf.message.standard.LocationMessage;
import it.auties.whatsapp.protobuf.message.standard.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.List;

/**
 * A model class that represents a WhatsappMessage that contains buttons inside.
 * Not much is known about this type of message as no one has encountered it.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class ButtonsMessage extends ContextualMessage implements ButtonMessage {
  /**
   * The text of this message
   */
  @JsonProperty("1")
  @JsonPropertyDescription("string")
  private String text;

  /**
   * The document message attached to this message
   */
  @JsonProperty("2")
  @JsonPropertyDescription("document")
  private DocumentMessage document;

  /**
   * The image message attached to this message
   */
  @JsonProperty("3")
  @JsonPropertyDescription("image")
  private ImageMessage image;

  /**
   * The video message attached to this message
   */
  @JsonProperty("4")
  @JsonPropertyDescription("video")
  private VideoMessage video;

  /**
   * The location message attached to this message
   */
  @JsonProperty("5")
  @JsonPropertyDescription("location")
  private LocationMessage location;

  /**
   * The image message attached to this message
   */
  @JsonProperty("6")
  @JsonPropertyDescription("string")
  private String contentText;

  /**
   * The footer text of this message
   */
  @JsonProperty("7")
  @JsonPropertyDescription("string")
  private String footerText;

  /**
   * The context info of this message
   */
  @JsonProperty("8")
  @JsonPropertyDescription("context")
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The buttons that this message wraps
   */
  @JsonProperty("9")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @JsonPropertyDescription("button")
  private List<Button> buttons;

  /**
   * Returns the type of header of this message
   *
   * @return a non-null header type
   */
  public HeaderType headerType() {
    if (text != null) return HeaderType.TEXT;
    if (document != null) return HeaderType.DOCUMENT;
    if (image != null) return HeaderType.IMAGE;
    if (video != null) return HeaderType.VIDEO;
    if (location != null) return HeaderType.LOCATION;
    return HeaderType.UNKNOWN;
  }

  /**
   * The constants of this enumerated type describe the various of types of headers that a {@link ButtonsMessage} can have
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HeaderType {
    /**
     * Unknown
     */
    UNKNOWN(0),

    /**
     * Text message
     */
    TEXT(1),

    /**
     * Document message
     */
    DOCUMENT(2),

    /**
     * Image message
     */
    IMAGE(3),

    /**
     * Video message
     */
    VIDEO(4),

    /**
     * Location message
     */
    LOCATION(5);

    @Getter
    private final int index;

    @JsonCreator
    public static HeaderType forIndex(int index) {
      return Arrays.stream(values())
          .filter(entry -> entry.index() == index)
          .findFirst()
          .orElse(HeaderType.UNKNOWN);
    }
  }
}
