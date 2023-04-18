package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.standard.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a message that contains buttons inside
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Jacksonized
@Data
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
    private ContextInfo contextInfo;

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
     * Constructs a new builder to create a buttons message.
     * The result can be later sent using {@link Whatsapp#sendMessage(MessageInfo)}
     *
     * @param body        the text body of this message
     * @param footer      the footer of this message
     * @param contextInfo the context info that the new message wraps
     * @param buttons     the buttons of this message
     * @return a non-null new message
     */
    @Builder(builderClassName = "ButtonsMessageSimpleBuilder", builderMethodName = "simpleBuilder")
    private static ButtonsMessage customBuilder(ButtonsMessageHeader header, String body, String footer, ContextInfo contextInfo, List<Button> buttons) {
        var builder = ButtonsMessage.builder()
                .body(body)
                .footer(footer)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .buttons(requireNonNullElseGet(buttons, ArrayList::new));
        switch (header){
            case DocumentMessage documentMessage -> builder.headerDocument(documentMessage).headerType(HeaderType.DOCUMENT);
            case ImageMessage imageMessage -> builder.headerImage(imageMessage).headerType(HeaderType.IMAGE);
            case LocationMessage locationMessage -> builder.headerLocation(locationMessage).headerType(HeaderType.LOCATION);
            case TextMessage textMessage -> builder.headerText(textMessage.text()).headerType(HeaderType.TEXT);
            case VideoMessage videoMessage -> builder.headerVideo(videoMessage).headerType(HeaderType.VIDEO);
            case null -> builder.headerType(HeaderType.EMPTY);
        }
        return builder.build();
    }

    /**
     * Returns the type of this message
     *
     * @return a non-null type
     */
    @Override
    public MessageType type() {
        return MessageType.BUTTONS;
    }

    /**
     * Returns the type of header of this message
     *
     * @return a non-null type
     */
    public HeaderType headerType() {
        if (headerText != null) {
            return HeaderType.TEXT;
        }
        if (headerDocument != null) {
            return HeaderType.DOCUMENT;
        }
        if (headerImage != null) {
            return HeaderType.IMAGE;
        }
        if (headerVideo != null) {
            return HeaderType.VIDEO;
        }
        if(headerLocation != null){
            return HeaderType.LOCATION;
        }
        return HeaderType.EMPTY;
    }

    /**
     * Returns the header of this message
     *
     * @return an optional
     */
    public Optional<ButtonsMessageHeader> header(){
        if (headerText != null) {
            return Optional.of(TextMessage.of(headerText));
        }
        if (headerDocument != null) {
            return Optional.of(headerDocument);
        }
        if (headerImage != null) {
            return Optional.of(headerImage);
        }
        if (headerVideo != null) {
            return Optional.of(headerVideo);
        }
        if(headerLocation != null){
            return Optional.of(headerLocation);
        }
        return Optional.empty();
    }

    /**
     * Returns the text header of this message if present
     *
     * @return an optional
     */
    public Optional<String> headerText(){
        return Optional.ofNullable(headerText);
    }

    /**
     * Returns the document header of this message if present
     *
     * @return an optional
     */
    public Optional<DocumentMessage> headerDocument(){
        return Optional.ofNullable(headerDocument);
    }

    /**
     * Returns the image header of this message if present
     *
     * @return an optional
     */
    public Optional<ImageMessage> headerImage(){
        return Optional.ofNullable(headerImage);
    }

    /**
     * Returns the video header of this message if present
     *
     * @return an optional
     */
    public Optional<VideoMessage> headerVideo(){
        return Optional.ofNullable(headerVideo);
    }

    /**
     * Returns the location header of this message if present
     *
     * @return an optional
     */
    public Optional<LocationMessage> headerLocation(){
        return Optional.ofNullable(headerLocation);
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
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(HeaderType.UNKNOWN);
        }

        public boolean hasMedia(){
            return this == DOCUMENT
                    || this == IMAGE
                    || this == VIDEO;
        }
    }
}