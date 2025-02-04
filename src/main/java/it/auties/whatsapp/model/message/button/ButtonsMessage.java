package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.Button;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader.Type;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

import java.util.List;
import java.util.Optional;

/**
 * A model class that represents a message that contains buttons inside
 */
@ProtobufMessage(name = "Message.ButtonsMessage")
public final class ButtonsMessage implements ButtonMessage, ContextualMessage<ButtonsMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private final ButtonsMessageHeaderText headerText;
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final DocumentMessage headerDocument;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final ImageMessage headerImage;
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final VideoOrGifMessage headerVideo;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final LocationMessage headerLocation;
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    private final String body;
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    private final String footer;
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
    private final List<Button> buttons;
    @ProtobufProperty(index = 10, type = ProtobufType.ENUM)
    private final Type headerType;


    public ButtonsMessage(ButtonsMessageHeaderText headerText, DocumentMessage headerDocument, ImageMessage headerImage, VideoOrGifMessage headerVideo, LocationMessage headerLocation, String body, String footer, ContextInfo contextInfo, List<Button> buttons, Type headerType) {
        this.headerText = headerText;
        this.headerDocument = headerDocument;
        this.headerImage = headerImage;
        this.headerVideo = headerVideo;
        this.headerLocation = headerLocation;
        this.body = body;
        this.footer = footer;
        this.contextInfo = contextInfo;
        this.buttons = buttons;
        this.headerType = headerType;
    }

    @ProtobufBuilder(className = "ButtonsMessageSimpleBuilder")
    static ButtonsMessage customBuilder(ButtonsMessageHeader header, String body, String footer, ContextInfo contextInfo, List<Button> buttons) {
        var builder = new ButtonsMessageBuilder()
                .body(body)
                .footer(footer)
                .contextInfo(contextInfo)
                .buttons(buttons);
        switch (header) {
            case ButtonsMessageHeaderText textMessage -> builder.headerText(textMessage)
                    .headerType(Type.TEXT);
            case DocumentMessage documentMessage -> builder.headerDocument(documentMessage)
                    .headerType(Type.DOCUMENT);
            case ImageMessage imageMessage -> builder.headerImage(imageMessage)
                    .headerType(Type.IMAGE);
            case VideoOrGifMessage videoMessage -> builder.headerVideo(videoMessage)
                    .headerType(Type.VIDEO);
            case LocationMessage locationMessage -> builder.headerLocation(locationMessage)
                    .headerType(Type.LOCATION);
            case null -> builder.headerType(Type.UNKNOWN);
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
     * Returns the header of this message
     *
     * @return an optional
     */
    public Optional<? extends ButtonsMessageHeader> header() {
        if (headerText != null) {
            return Optional.of(headerText);
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

        return Optional.ofNullable(headerLocation);
    }

    public Optional<ButtonsMessageHeaderText> headerText() {
        return Optional.ofNullable(headerText);
    }

    public Optional<DocumentMessage> headerDocument() {
        return Optional.ofNullable(headerDocument);
    }

    public Optional<ImageMessage> headerImage() {
        return Optional.ofNullable(headerImage);
    }

    public Optional<VideoOrGifMessage> headerVideo() {
        return Optional.ofNullable(headerVideo);
    }

    public Optional<LocationMessage> headerLocation() {
        return Optional.ofNullable(headerLocation);
    }

    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    public List<Button> buttons() {
        return buttons;
    }

    public Type headerType() {
        return headerType;
    }

    @Override
    public ButtonsMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }

    @Override
    public String toString() {
        return "ButtonsMessage[" +
                "headerText=" + headerText + ", " +
                "headerDocument=" + headerDocument + ", " +
                "headerImage=" + headerImage + ", " +
                "headerVideo=" + headerVideo + ", " +
                "headerLocation=" + headerLocation + ", " +
                "body=" + body + ", " +
                "footer=" + footer + ", " +
                "contextInfo=" + contextInfo + ", " +
                "buttons=" + buttons + ", " +
                "headerType=" + headerType + ']';
    }
}