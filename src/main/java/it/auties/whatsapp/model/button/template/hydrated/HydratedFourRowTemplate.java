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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A model class that represents a hydrated four row template
 */
@ProtobufMessage(name = "Message.TemplateMessage.HydratedFourRowTemplate")
public final class HydratedFourRowTemplate implements TemplateFormatter {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final DocumentMessage titleDocument;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final HydratedFourRowTemplateTextTitle titleText;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ImageMessage titleImage;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final VideoOrGifMessage titleVideo;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final LocationMessage titleLocation;

    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String body;

    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String footer;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<HydratedTemplateButton> hydratedButtons;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String templateId;

    HydratedFourRowTemplate(DocumentMessage titleDocument, HydratedFourRowTemplateTextTitle titleText,
                            ImageMessage titleImage, VideoOrGifMessage titleVideo, LocationMessage titleLocation,
                            String body, String footer, List<HydratedTemplateButton> hydratedButtons, String templateId) {
        this.titleDocument = titleDocument;
        this.titleText = titleText;
        this.titleImage = titleImage;
        this.titleVideo = titleVideo;
        this.titleLocation = titleLocation;
        this.body = body;
        this.footer = footer;
        this.hydratedButtons = Objects.requireNonNullElse(hydratedButtons, List.of());
        this.templateId = templateId;
    }

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

    public Optional<DocumentMessage> titleDocument() {
        return Optional.ofNullable(titleDocument);
    }

    public Optional<HydratedFourRowTemplateTextTitle> titleText() {
        return Optional.ofNullable(titleText);
    }

    public Optional<ImageMessage> titleImage() {
        return Optional.ofNullable(titleImage);
    }

    public Optional<VideoOrGifMessage> titleVideo() {
        return Optional.ofNullable(titleVideo);
    }

    public Optional<LocationMessage> titleLocation() {
        return Optional.ofNullable(titleLocation);
    }

    public String body() {
        return body;
    }

    public Optional<String> footer() {
        return Optional.ofNullable(footer);
    }

    public List<HydratedTemplateButton> hydratedButtons() {
        return hydratedButtons;
    }

    public String templateId() {
        return templateId;
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
        if (titleDocument != null) {
            return Optional.of(titleDocument);
        }else if (titleText != null) {
            return Optional.of(titleText);
        }else if (titleImage != null) {
            return Optional.of(titleImage);
        }else if (titleVideo != null) {
            return Optional.of(titleVideo);
        }else if(titleLocation != null){
            return Optional.of(titleLocation);
        }else {
            return Optional.empty();
        }
    }

    @Override
    public Type templateType() {
        return Type.HYDRATED_FOUR_ROW;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HydratedFourRowTemplate that
                && Objects.equals(titleDocument, that.titleDocument)
                && Objects.equals(titleText, that.titleText)
                && Objects.equals(titleImage, that.titleImage)
                && Objects.equals(titleVideo, that.titleVideo)
                && Objects.equals(titleLocation, that.titleLocation)
                && Objects.equals(body, that.body)
                && Objects.equals(footer, that.footer)
                && Objects.equals(hydratedButtons, that.hydratedButtons)
                && Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleDocument, titleText, titleImage, titleVideo, titleLocation,
                body, footer, hydratedButtons, templateId);
    }

    @Override
    public String toString() {
        return "HydratedFourRowTemplate[" +
                "titleDocument=" + titleDocument +
                ", titleText=" + titleText +
                ", titleImage=" + titleImage +
                ", titleVideo=" + titleVideo +
                ", titleLocation=" + titleLocation +
                ", body=" + body +
                ", footer=" + footer +
                ", hydratedButtons=" + hydratedButtons +
                ", templateId=" + templateId + ']';
    }
}