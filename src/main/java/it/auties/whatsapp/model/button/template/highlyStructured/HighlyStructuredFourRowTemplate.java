package it.auties.whatsapp.model.button.template.highlyStructured;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.base.TemplateFormatter;
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
@ProtobufMessage(name = "Message.TemplateMessage.FourRowTemplate")
public record HighlyStructuredFourRowTemplate(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<DocumentMessage> titleDocument,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredMessage> titleHighlyStructured,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        Optional<ImageMessage> titleImage,
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        Optional<VideoOrGifMessage> titleVideo,
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        Optional<LocationMessage> titleLocation,
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        HighlyStructuredMessage content,
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        Optional<HighlyStructuredMessage> footer,
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        List<HighlyStructuredButtonTemplate> buttons
) implements TemplateFormatter {
    @ProtobufBuilder(className = "HighlyStructuredFourRowTemplateSimpleBuilder")
    static HighlyStructuredFourRowTemplate simpleBuilder(HighlyStructuredFourRowTemplateTitle title, HighlyStructuredMessage content, HighlyStructuredMessage footer, List<HighlyStructuredButtonTemplate> buttons) {
        var builder = new HighlyStructuredFourRowTemplateBuilder()
                .buttons(getIndexedButtons(buttons))
                .content(content)
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
        return Type.FOUR_ROW;
    }
}