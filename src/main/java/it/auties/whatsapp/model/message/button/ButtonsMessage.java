package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.Button;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

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
  @ProtobufProperty(index = 1, type = STRING)
  private String text;

  /**
   * The document message attached to this message
   */
  @ProtobufProperty(index = 2, type = MESSAGE, concreteType = DocumentMessage.class)
  private DocumentMessage document;

  /**
   * The image message attached to this message
   */
  @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
  private ImageMessage image;

  /**
   * The video message attached to this message
   */
  @ProtobufProperty(index = 4, type = MESSAGE, concreteType = VideoMessage.class)
  private VideoMessage video;

  /**
   * The location message attached to this message
   */
  @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
  private LocationMessage location;

  /**
   * The image message attached to this message
   */
  @ProtobufProperty(index = 6, type = STRING)
  private String contentText;

  /**
   * The footer text of this message
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String footerText;

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 8, type = MESSAGE, concreteType = ContextInfo.class)
  private ContextInfo contextInfo; // Overrides ContextualMessage's context info

  /**
   * The buttons that this message wraps
   */
  @ProtobufProperty(index = 9, type = MESSAGE,
          concreteType = Button.class, repeated = true)
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
  public enum HeaderType implements ProtobufMessage {
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

  public static class ButtonsMessageBuilder {
    public ButtonsMessageBuilder polygonVertices(List<Button> buttons){
      if(this.buttons == null) this.buttons = new ArrayList<>();
      this.buttons.addAll(buttons);
      return this;
    }
  }
}
