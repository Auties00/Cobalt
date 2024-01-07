package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.TemplateFormatter;
import it.auties.whatsapp.model.button.template.highlyStructured.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoOrGifMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A model class that represents a four row template
 */
@ProtobufMessageName("Message.TemplateMessage.FourRowTemplate")
public record HighlyStructuredFourRowTemplate(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<DocumentMessage> titleDocument,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredMessage> titleHighlyStructured,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<ImageMessage> titleImage,
        @ProtobufProperty(index = 4, type = ProtobufType.OBJECT)
        Optional<VideoOrGifMessage> titleVideo,
        @ProtobufProperty(index = 5, type = ProtobufType.OBJECT)
        Optional<LocationMessage> titleLocation,
        @ProtobufProperty(index = 6, type = ProtobufType.OBJECT)
        HighlyStructuredMessage content,
        @ProtobufProperty(index = 7, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredMessage> footer,
        @ProtobufProperty(index = 8, type = ProtobufType.OBJECT)
        List<HighlyStructuredButtonTemplate> buttons
) implements TemplateFormatter {
    @ProtobufBuilder(className = "HighlyStructuredFourRowTemplateSimpleBuilder")
    static HighlyStructuredFourRowTemplate simpleBuilder(HighlyStructuredFourRowTemplateTitle title, HighlyStructuredMessage content, HighlyStructuredMessage footer, List<HighlyStructuredButtonTemplate> buttons) {
        var builder = new HighlyStructuredFourRowTemplateBuilder()
                .buttons(getIndexedButtons(buttons))
                .footer(footer);
        switch (title) {
            case DocumentMessage documentMessage -> builder.titleDocument(documentMessage);
            case HighlyStructuredMessage highlyStructuredMessage ->
                    builder.titleHighlyStructured(highlyStructuredMessage);
            case ImageMessage imageMessage -> builder.titleImage(imageMessage);
            case VideoOrGifMessage videoMessage -> builder.titleVideo(videoMessage);
            case LocationMessage locationMessage -> builder.titleLocation(locationMessage);
            case null -> {
            }
        }
        return builder.build();
    }

    private static List<HighlyStructuredButtonTemplate> getIndexedButtons(List<HighlyStructuredButtonTemplate> buttons) {
        return IntStream.range(0, buttons.size()).mapToObj(index -> {
            var button = buttons.get(index);
            return new HighlyStructuredButtonTemplate(button.highlyStructuredQuickReplyButton(), button.highlyStructuredUrlButton(), button.highlyStructuredCallButton(), index + 1);
        }).toList();
    }

    /**
     * Returns the type of title that this template wraps
     *
     * @return a non-null title type
     */
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return title().map(HighlyStructuredFourRowTemplateTitle::titleType)
                .orElse(HighlyStructuredFourRowTemplateTitle.Type.NONE);
    }

    /**
     * Returns the title of this template
     *
     * @return an optional
     */
    public Optional<? extends HighlyStructuredFourRowTemplateTitle> title() {
        if (titleDocument.isPresent()) {
            return titleDocument;
        }

        if (titleHighlyStructured.isPresent()) {
            return titleHighlyStructured;
        }

        if (titleImage.isPresent()) {
            return titleImage;
        }

        if (titleVideo.isPresent()) {
            return titleVideo;
        }

        return titleLocation;
    }

    @Override
    public Type templateType() {
        return TemplateFormatter.Type.FOUR_ROW;
    }
}