package it.auties.whatsapp.model.button.template.hydrated;

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
 * A model class that represents a hydrated four row template
 */
@ProtobufMessage(name = "Message.TemplateMessage.HydratedFourRowTemplate")
public record HydratedFourRowTemplate(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        Optional<DocumentMessage> titleDocument,
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        Optional<HydratedFourRowTemplateTextTitle> titleText,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        Optional<ImageMessage> titleImage,
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        Optional<VideoOrGifMessage> titleVideo,
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        Optional<LocationMessage> titleLocation,
        @ProtobufProperty(index = 6, type = ProtobufType.STRING)
        String body,
        @ProtobufProperty(index = 7, type = ProtobufType.STRING)
        Optional<String> footer,
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        List<HydratedTemplateButton> hydratedButtons,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        String templateId
) implements TemplateFormatter {
    @ProtobufBuilder(className = "HydratedFourRowTemplateSimpleBuilder")
    static HydratedFourRowTemplate customBuilder(HydratedFourRowTemplateTitle title, String body, String footer, List<HydratedTemplateButton> buttons, String templateId) {
        var builder = new HydratedFourRowTemplateBuilder()
                .templateId(templateId)
                .body(body)
                .hydratedButtons(getIndexedButtons(buttons))
                .footer(footer);
        switch (title) {
            case DocumentMessage documentMessage -> builder.titleDocument(documentMessage);
            case HydratedFourRowTemplateTextTitle hydratedFourRowTemplateTextTitle ->
                    builder.titleText(hydratedFourRowTemplateTextTitle);
            case ImageMessage imageMessage -> builder.titleImage(imageMessage);
            case VideoOrGifMessage videoMessage -> builder.titleVideo(videoMessage);
            case LocationMessage locationMessage -> builder.titleLocation(locationMessage);
            case null -> {
            }
        }
        return builder.build();
    }

    private static List<HydratedTemplateButton> getIndexedButtons(List<HydratedTemplateButton> buttons) {
        return IntStream.range(0, buttons.size()).mapToObj(index -> {
            var button = buttons.get(index);
            return new HydratedTemplateButton(button.quickReplyButton(), button.urlButton(), button.callButton(), index + 1);
        }).toList();
    }

    /**
     * Returns the type of title that this template wraps
     *
     * @return a non-null title type
     */
    public HydratedFourRowTemplateTitle.Type titleType() {
        return title().map(HydratedFourRowTemplateTitle::hydratedTitleType)
                .orElse(HydratedFourRowTemplateTitle.Type.NONE);
    }

    /**
     * Returns the title of this template
     *
     * @return an optional
     */
    public Optional<? extends HydratedFourRowTemplateTitle> title() {
        if (titleDocument.isPresent()) {
            return titleDocument;
        }

        if (titleText.isPresent()) {
            return titleText;
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
        return Type.HYDRATED_FOUR_ROW;
    }
}