package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.button.TemplateFormatterType;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
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

/**
 * A model class that represents a four row template
 */
@AllArgsConstructor
@NoArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("TemplateMessage.FourRowTemplate")
public final class FourRowTemplate implements TemplateFormatter {
    /**
     * The document title of this row. This property is defined only if
     * {@link FourRowTemplate#titleType()} == {@link FourRowTemplateTitleType#DOCUMENT}.
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = DocumentMessage.class)
    private DocumentMessage titleDocument;

    /**
     * The highly structured title of this row. This property is defined only if
     * {@link FourRowTemplate#titleType()} == {@link FourRowTemplateTitleType#HIGHLY_STRUCTURED}.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage titleHighlyStructured;

    /**
     * The image title of this row. This property is defined only if
     * {@link FourRowTemplate#titleType()} == {@link FourRowTemplateTitleType#IMAGE}.
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage titleImage;

    /**
     * The video title of this row This property is defined only if
     * {@link FourRowTemplate#titleType()} == {@link FourRowTemplateTitleType#VIDEO}.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = VideoMessage.class)
    private VideoMessage titleVideo;

    /**
     * The location title of this row. This property is defined only if
     * {@link FourRowTemplate#titleType()} == {@link FourRowTemplateTitleType#LOCATION}.
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = LocationMessage.class)
    private LocationMessage titleLocation;

    /**
     * The content of this row
     */
    @ProtobufProperty(index = 6, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage content;

    /**
     * The footer of this row
     */
    @ProtobufProperty(index = 7, type = MESSAGE, implementation = HighlyStructuredMessage.class)
    private HighlyStructuredMessage footer;

    /**
     * The buttons of this row
     */
    @ProtobufProperty(index = 8, type = MESSAGE, implementation = ButtonTemplate.class, repeated = true)
    private List<ButtonTemplate> buttons;

    /**
     * Constructs a new builder to create a four row template
     *
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "FourRowTemplateSimpleBuilder", builderMethodName = "simpleBuilder")
    private static FourRowTemplate customBuilder(FourRowTemplateTitle title, HighlyStructuredMessage content, HighlyStructuredMessage footer, List<ButtonTemplate> buttons) {
        IntStream.range(0, buttons.size()).forEach(index -> buttons.get(index).index(index + 1));
        var builder = FourRowTemplate.builder().content(content).footer(footer).buttons(buttons);
        switch (title){
            case DocumentMessage documentMessage -> builder.titleDocument(documentMessage);
            case HighlyStructuredMessage highlyStructuredMessage -> builder.titleHighlyStructured(highlyStructuredMessage);
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
    public FourRowTemplateTitleType titleType() {
        return title().map(FourRowTemplateTitle::titleType)
                .orElse(FourRowTemplateTitleType.NONE);
    }

    /**
     * Returns the title of this template
     *
     * @return an optional
     */
    public Optional<FourRowTemplateTitle> title() {
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
     * Returns the highly structured title of this message if present
     *
     * @return an optional
     */
    public Optional<HighlyStructuredMessage> titleHighlyStructured() {
        return Optional.ofNullable(titleHighlyStructured);
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
        return TemplateFormatterType.FOUR_ROW;
    }

}