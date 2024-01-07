package it.auties.whatsapp.model.message.button;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessageName;
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
@ProtobufMessageName("Message.ButtonsMessage")
public record ButtonsMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<ButtonsMessageHeaderText> headerText,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<DocumentMessage> headerDocument,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ImageMessage> headerImage,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<VideoOrGifMessage> headerVideo,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<LocationMessage> headerLocation,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        Optional<String> body,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> footer,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo,
        @ProtobufProperty(index = 9, type = ProtobufType.OBJECT)
        List<Button> buttons,
        @ProtobufProperty(index = 10, type = ProtobufType.OBJECT)
        Type headerType
) implements ButtonMessage, ContextualMessage {
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
        if (headerText.isPresent()) {
            return headerText;
        }

        if (headerDocument.isPresent()) {
            return headerDocument;
        }

        if (headerImage.isPresent()) {
            return headerImage;
        }

        if (headerVideo.isPresent()) {
            return headerVideo;
        }

        return headerLocation;
    }
}