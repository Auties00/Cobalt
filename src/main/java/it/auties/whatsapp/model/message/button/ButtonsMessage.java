package it.auties.whatsapp.model.message.button;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static java.util.Objects.requireNonNullElseGet;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.Button;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents a message that contains buttons inside
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
   * The text attached to this message
   */
  @ProtobufProperty(index = 1, type = STRING)
  private String headerText;

  /**
   * The document message attached to this message
   */
  @ProtobufProperty(index = 2, type = MESSAGE, implementation = DocumentMessage.class)
  private DocumentMessage headerDocument;

  /**
   * The image message attached to this message
   */
  @ProtobufProperty(index = 3, type = MESSAGE, implementation = ImageMessage.class)
  private ImageMessage headerImage;

  /**
   * The video message attached to this message
   */
  @ProtobufProperty(index = 4, type = MESSAGE, implementation = VideoMessage.class)
  private VideoMessage headerVideo;

  /**
   * The location message attached to this message
   */
  @ProtobufProperty(index = 5, type = MESSAGE, implementation = LocationMessage.class)
  private LocationMessage headerLocation;

  /**
   * The body of this message
   */
  @ProtobufProperty(index = 6, type = STRING)
  private String body;

  /**
   * The footer of this message
   */
  @ProtobufProperty(index = 7, type = STRING)
  private String footer;

  /**
   * The context info of this message
   */
  @ProtobufProperty(index = 8, type = MESSAGE, implementation = ContextInfo.class)
  @Builder.Default
  private ContextInfo contextInfo = new ContextInfo();

  /**
   * The buttons that this message wraps
   */
  @ProtobufProperty(index = 9, type = MESSAGE, implementation = Button.class, repeated = true)
  private List<Button> buttons;

  /**
   * The type of header
   */
  @ProtobufProperty(index = 10, type = MESSAGE, implementation = ButtonsMessage.HeaderType.class)
  private HeaderType headerType;

  /**
   * Constructs a new builder to create a buttons message without a header. The result can be later
   * sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "EmptyButtonsMessageBuilder", builderMethodName = "withoutHeaderBuilder")
  private static ButtonsMessage emptyBuilder(String body, String footer, ContextInfo contextInfo,
      List<Button> buttons) {
    return createBuilder(HeaderType.EMPTY, body, footer, contextInfo, buttons).build();
  }

  /**
   * Constructs a new builder to create a buttons message with a text header. The result can be
   * later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param header      the text header
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "TextButtonsMessageBuilder", builderMethodName = "withTextHeaderBuilder")
  private static ButtonsMessage textBuilder(String header, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return createBuilder(HeaderType.TEXT, body, footer, contextInfo, buttons).headerText(header)
        .build();
  }

  /**
   * Constructs a new builder to create a buttons message with a document header. The result can be
   * later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param header      the document header
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "DocumentButtonsMessageBuilder", builderMethodName = "withDocumentHeaderBuilder")
  private static ButtonsMessage documentBuilder(DocumentMessage header, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return createBuilder(HeaderType.DOCUMENT, body, footer, contextInfo, buttons).headerDocument(
        header).build();
  }

  /**
   * Constructs a new builder to create a buttons message with an image header. The result can be
   * later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param header      the document header
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "ImageButtonsMessageBuilder", builderMethodName = "withImageHeaderBuilder")
  private static ButtonsMessage imageBuilder(ImageMessage header, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return createBuilder(HeaderType.IMAGE, body, footer, contextInfo, buttons).headerImage(header)
        .build();
  }

  /**
   * Constructs a new builder to create a buttons message with a video header. The result can be
   * later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param header      the document header
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "VideoButtonsMessageBuilder", builderMethodName = "withVideoHeaderBuilder")
  private static ButtonsMessage videoBuilder(VideoMessage header, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return createBuilder(HeaderType.VIDEO, body, footer, contextInfo, buttons).headerVideo(header)
        .build();
  }

  /**
   * Constructs a new builder to create a buttons message with a location header. The result can be
   * later sent using {@link Whatsapp#sendMessage(MessageInfo)}
   *
   * @param header      the document header
   * @param body        the text body of this message
   * @param footer      the text footer of this message
   * @param contextInfo the context info that the new message wraps
   * @param buttons     the buttons of this message
   * @return a non-null new message
   */
  @Builder(builderClassName = "LocationHeaderButtonsMessageBuilder", builderMethodName = "withLocationHeaderBuilder")
  private static ButtonsMessage locationBuilder(LocationMessage header, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return createBuilder(HeaderType.LOCATION, body, footer, contextInfo, buttons).headerLocation(
        header).build();
  }

  private static ButtonsMessageBuilder createBuilder(HeaderType image, String body, String footer,
      ContextInfo contextInfo, List<Button> buttons) {
    return ButtonsMessage.builder().headerType(image).body(body).footer(footer)
        .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
        .buttons(requireNonNullElseGet(buttons, List::of));
  }

  @Override
  public MessageType type() {
    return MessageType.BUTTONS;
  }

  public ButtonsMessage.HeaderType headerType() {
    if (headerText != null) {
      return ButtonsMessage.HeaderType.EMPTY;
    }
    if (headerDocument != null) {
      return ButtonsMessage.HeaderType.TEXT;
    }
    if (headerImage != null) {
      return ButtonsMessage.HeaderType.DOCUMENT;
    }
    if (headerVideo != null) {
      return ButtonsMessage.HeaderType.IMAGE;
    }
    return ButtonsMessage.HeaderType.VIDEO;
  }

  /**
   * The constants of this enumerated type describe the various of types of headers that a
   * {@link ButtonsMessage} can have
   */
  @AllArgsConstructor
  @Accessors(fluent = true)
  public enum HeaderType implements ProtobufMessage {

    /**
     * Unknown
     */
    UNKNOWN(0),
    /**
     * Empty
     */
    EMPTY(1),
    /**
     * Text message
     */
    TEXT(2),
    /**
     * Document message
     */
    DOCUMENT(3),
    /**
     * Image message
     */
    IMAGE(4),
    /**
     * Video message
     */
    VIDEO(5),
    /**
     * Location message
     */
    LOCATION(6);
    @Getter
    private final int index;

    @JsonCreator
    public static HeaderType of(int index) {
      return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst()
          .orElse(HeaderType.UNKNOWN);
    }
  }

  public static class ButtonsMessageBuilder {
    public ButtonsMessageBuilder buttons(List<Button> buttons) {
      if (this.buttons == null) {
        this.buttons = new ArrayList<>();
      }
      this.buttons.addAll(buttons);
      return this;
    }
  }
}