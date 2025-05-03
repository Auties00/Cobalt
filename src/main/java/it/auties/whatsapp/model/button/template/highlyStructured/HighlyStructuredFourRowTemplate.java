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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a four row template
 */
@ProtobufMessage(name = "Message.TemplateMessage.FourRowTemplate")
public final class HighlyStructuredFourRowTemplate implements TemplateFormatter {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final DocumentMessage titleDocument;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage titleHighlyStructured;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final ImageMessage titleImage;

    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final VideoOrGifMessage titleVideo;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final LocationMessage titleLocation;

    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage content;

    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    final HighlyStructuredMessage footer;

    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<HighlyStructuredButtonTemplate> buttons;

    HighlyStructuredFourRowTemplate(DocumentMessage titleDocument, HighlyStructuredMessage titleHighlyStructured,
                                    ImageMessage titleImage, VideoOrGifMessage titleVideo, LocationMessage titleLocation,
                                    HighlyStructuredMessage content, HighlyStructuredMessage footer,
                                    List<HighlyStructuredButtonTemplate> buttons) {
        this.titleDocument = titleDocument;
        this.titleHighlyStructured = titleHighlyStructured;
        this.titleImage = titleImage;
        this.titleVideo = titleVideo;
        this.titleLocation = titleLocation;
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.footer = footer;
        this.buttons = Objects.requireNonNullElse(buttons, List.of());
    }

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
            case null -> {}
        }
        return builder.build();
    }

    private static List<HighlyStructuredButtonTemplate> getIndexedButtons(List<HighlyStructuredButtonTemplate> buttons) {
        var list = new ArrayList<HighlyStructuredButtonTemplate>(buttons.size());
        for (var index = 0; index < buttons.size(); index++) {
            var button = buttons.get(index);
            var highlyStructuredQuickReplyButton = button.highlyStructuredQuickReplyButton()
                    .orElse(null);
            var highlyStructuredUrlButton = button.highlyStructuredUrlButton()
                    .orElse(null);
            var highlyStructuredCallButton = button.highlyStructuredCallButton()
                    .orElse(null);
            var apply = new HighlyStructuredButtonTemplate(highlyStructuredQuickReplyButton, highlyStructuredUrlButton, highlyStructuredCallButton, index + 1);
            list.set(index, apply);
        }
        return list;
    }

    public Optional<DocumentMessage> titleDocument() {
        return Optional.ofNullable(titleDocument);
    }

    public Optional<HighlyStructuredMessage> titleHighlyStructured() {
        return Optional.ofNullable(titleHighlyStructured);
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

    public HighlyStructuredMessage content() {
        return content;
    }

    public Optional<HighlyStructuredMessage> footer() {
        return Optional.ofNullable(footer);
    }

    public List<HighlyStructuredButtonTemplate> buttons() {
        return buttons;
    }

    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return title().map(HighlyStructuredFourRowTemplateTitle::titleType)
                .orElse(HighlyStructuredFourRowTemplateTitle.Type.NONE);
    }

    public Optional<? extends HighlyStructuredFourRowTemplateTitle> title() {
        if (titleDocument != null) {
            return Optional.of(titleDocument);
        }

        if (titleHighlyStructured != null) {
            return Optional.of(titleHighlyStructured);
        }

        if (titleImage != null) {
            return Optional.of(titleImage);
        }

        if (titleVideo != null) {
            return Optional.of(titleVideo);
        }

        return Optional.ofNullable(titleLocation);
    }

    @Override
    public Type templateType() {
        return Type.FOUR_ROW;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HighlyStructuredFourRowTemplate that
                && Objects.equals(titleDocument, that.titleDocument)
                && Objects.equals(titleHighlyStructured, that.titleHighlyStructured)
                && Objects.equals(titleImage, that.titleImage)
                && Objects.equals(titleVideo, that.titleVideo)
                && Objects.equals(titleLocation, that.titleLocation)
                && Objects.equals(content, that.content)
                && Objects.equals(footer, that.footer)
                && Objects.equals(buttons, that.buttons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleDocument, titleHighlyStructured, titleImage, titleVideo, titleLocation, content, footer, buttons);
    }

    @Override
    public String toString() {
        return "HighlyStructuredFourRowTemplate[" +
                "titleDocument=" + titleDocument + ", " +
                "titleHighlyStructured=" + titleHighlyStructured + ", " +
                "titleImage=" + titleImage + ", " +
                "titleVideo=" + titleVideo + ", " +
                "titleLocation=" + titleLocation + ", " +
                "content=" + content + ", " +
                "footer=" + footer + ", " +
                "buttons=" + buttons + ']';
    }
}