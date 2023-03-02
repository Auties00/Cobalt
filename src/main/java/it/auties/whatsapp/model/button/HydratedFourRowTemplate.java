package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.TemplateFormatterType;
import it.auties.whatsapp.model.message.standard.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a hydrated four row template
 */
@AllArgsConstructor
@NoArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("TemplateMessage.HydratedFourRowTemplate")
public final class HydratedFourRowTemplate implements TemplateFormatter {
    /**
     * The id of the template
     */
    @ProtobufProperty(index = 9, type = STRING)
    private String templateId;

    /**
     * The document title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link HydratedFourRowTemplateTitleType#DOCUMENT}.
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = DocumentMessage.class)
    private DocumentMessage titleDocument;

    /**
     * The text title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link HydratedFourRowTemplateTitleType#TEXT}.
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String titleText;

    /**
     * The image title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link HydratedFourRowTemplateTitleType#IMAGE}.
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage titleImage;

    /**
     * The video title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link HydratedFourRowTemplateTitleType#VIDEO}.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = VideoMessage.class)
    private VideoMessage titleVideo;

    /**
     * The location title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link HydratedFourRowTemplateTitleType#LOCATION}.
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = LocationMessage.class)
    private LocationMessage titleLocation;

    /**
     * The body of this row
     */
    @ProtobufProperty(index = 6, type = STRING)
    private String body;

    /**
     * The footer of this row
     */
    @ProtobufProperty(index = 7, type = STRING)
    private String footer;

    /**
     * The buttons of this row
     */
    @ProtobufProperty(index = 8, type = MESSAGE, implementation = HydratedTemplateButton.class, repeated = true)
    private List<HydratedTemplateButton> buttons;

    /**
     * Constructs a new builder to create a hydrated four row template
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "HydratedFourRowTemplateSimpleBuilder", builderMethodName = "simpleBuilder")
    private static HydratedFourRowTemplate customBuilder(HydratedFourRowTemplateTitle title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        IntStream.range(0, buttons.size()).forEach(index -> buttons.get(index).index(index + 1));
        var builder = HydratedFourRowTemplate.builder().body(body).footer(footer).buttons(buttons).templateId(id);
        switch (title){
            case DocumentMessage documentMessage -> builder.titleDocument(documentMessage);
            case TextMessage textMessage -> builder.titleText(textMessage.text());
            case ImageMessage imageMessage -> builder.titleImage(imageMessage);
            case VideoMessage videoMessage -> builder.titleVideo(videoMessage);
            case LocationMessage locationMessage -> builder.titleLocation(locationMessage);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns the type of title that this template wraps
     *
     * @return a non-null title type
     */
    public HydratedFourRowTemplateTitleType titleType() {
        return title().map(HydratedFourRowTemplateTitle::hydratedTitleType)
                .orElse(HydratedFourRowTemplateTitleType.NONE);
    }

    /**
     * Returns the title of this template
     *
     * @return an optional
     */
    public Optional<HydratedFourRowTemplateTitle> title() {
        if (titleDocument != null) {
            return Optional.of(titleDocument);
        }
        if (titleText != null) {
            return Optional.of(TextMessage.of(titleText));
        }
        if (titleImage != null) {
            return Optional.of(titleImage);
        }
        if (titleVideo != null) {
            return Optional.of(titleVideo);
        }
        if (titleLocation != null) {
            return Optional.of(titleLocation);
        }
        return Optional.empty();
    }

    /**
     * Returns the document title of this message if present
     *
     * @return an optional
     */
    public Optional<DocumentMessage> titleDocument() {
        return Optional.ofNullable(titleDocument);
    }

    /**
     * Returns the text title of this message if present
     *
     * @return an optional
     */
    public Optional<String> titleText() {
        return Optional.ofNullable(titleText);
    }

    /**
     * Returns the image title of this message if present
     *
     * @return an optional
     */
    public Optional<ImageMessage> titleImage() {
        return Optional.ofNullable(titleImage);
    }

    /**
     * Returns the video title of this message if present
     *
     * @return an optional
     */
    public Optional<VideoMessage> titleVideo() {
        return Optional.ofNullable(titleVideo);
    }

    /**
     * Returns the location title of this message if present
     *
     * @return an optional
     */
    public Optional<LocationMessage> titleLocation() {
        return Optional.ofNullable(titleLocation);
    }

    @Override
    public TemplateFormatterType templateType() {
        return TemplateFormatterType.HYDRATED_FOUR_ROW;
    }

}