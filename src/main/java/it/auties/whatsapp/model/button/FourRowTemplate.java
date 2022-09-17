package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

/**
 * A model class that represents a four row template
 */
@AllArgsConstructor
@Data
@Builder(builderMethodName = "newRawFourRowTemplate")
@Jacksonized
@Accessors(fluent = true)
public class FourRowTemplate implements ProtobufMessage {
    /**
     * The document title of this row
     */
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = DocumentMessage.class)
    private DocumentMessage documentTitle;

    /**
     * The highly structured title of this row
     */
    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = HighlyStructuredMessage.class)
    private HighlyStructuredMessage highlyStructuredTitle;

    /**
     * The image title of this row
     */
    @ProtobufProperty(index = 3, type = MESSAGE, concreteType = ImageMessage.class)
    private ImageMessage imageTitle;

    /**
     * The video title of this row
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = VideoMessage.class)
    private VideoMessage videoTitle;

    /**
     * The location title of this row
     */
    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = LocationMessage.class)
    private LocationMessage locationTitle;

    /**
     * The content of this template
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = HighlyStructuredMessage.class)
    private HighlyStructuredMessage content;

    /**
     * The footer of this template
     */
    @ProtobufProperty(index = 7, type = MESSAGE, concreteType = HighlyStructuredMessage.class)
    private HighlyStructuredMessage footer;

    /**
     * The buttons of this template
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = ButtonTemplate.class, repeated = true)
    private List<ButtonTemplate> buttons;

    /**
     * Constructs a new builder to create a four row template without title
     *
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "EmptyFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithoutTitleBuilder")
    private static FourRowTemplate emptyBuilder(HighlyStructuredMessage content, HighlyStructuredMessage footer,
                                                List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).build();

    }

    /**
     * Constructs a new builder to create a four row template with a document title
     *
     * @param title   the title of this template
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "DocumentFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithDocumentTitleBuilder")
    private static FourRowTemplate documentBuilder(DocumentMessage title, HighlyStructuredMessage content,
                                                   HighlyStructuredMessage footer, List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).documentTitle(title)
                .build();

    }

    /**
     * Constructs a new builder to create a four row template with a highly structured title
     *
     * @param title   the title of this template
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "HighlyStructuredFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithHighlyStructuredTitleBuilder")
    private static FourRowTemplate highlyStructuredBuilder(HighlyStructuredMessage title,
                                                           HighlyStructuredMessage content,
                                                           HighlyStructuredMessage footer,
                                                           List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).highlyStructuredTitle(title)
                .build();
    }

    /**
     * Constructs a new builder to create a four row template with an image title
     *
     * @param title   the title of this template
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "ImageFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithImageTitleBuilder")
    private static FourRowTemplate imageBuilder(ImageMessage title, HighlyStructuredMessage content,
                                                HighlyStructuredMessage footer, List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).imageTitle(title)
                .build();

    }

    /**
     * Constructs a new builder to create a four row template with a video title
     *
     * @param title   the title of this template
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "VideoFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithVideoTitleBuilder")
    private static FourRowTemplate videoBuilder(VideoMessage title, HighlyStructuredMessage content,
                                                HighlyStructuredMessage footer, List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).videoTitle(title)
                .build();

    }

    /**
     * Constructs a new builder to create a four row template with a location title
     *
     * @param title   the title of this template
     * @param content the content of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "LocationFourRowTemplateBuilder", builderMethodName = "newFourRowTemplateWithLocationTitleBuilder")
    private static FourRowTemplate locationBuilder(LocationMessage title, HighlyStructuredMessage content,
                                                   HighlyStructuredMessage footer, List<ButtonTemplate> buttons) {
        return createBuilder(content, footer, buttons).locationTitle(title)
                .build();

    }

    private static FourRowTemplateBuilder createBuilder(HighlyStructuredMessage content, HighlyStructuredMessage footer,
                                                        List<ButtonTemplate> buttons) {
        return FourRowTemplate.newRawFourRowTemplate()
                .content(content)
                .footer(footer)
                .buttons(buttons);
    }


    /**
     * Returns the type of title that this template wraps
     *
     * @return a non-null title type
     */
    public TitleType titleType() {
        if (documentTitle != null)
            return TitleType.DOCUMENT_MESSAGE;
        if (highlyStructuredTitle != null)
            return TitleType.HIGHLY_STRUCTURED_MESSAGE;
        if (imageTitle != null)
            return TitleType.IMAGE_MESSAGE;
        if (videoTitle != null)
            return TitleType.VIDEO_MESSAGE;
        if (locationTitle != null)
            return TitleType.LOCATION_MESSAGE;
        return TitleType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various types of title that a template can have
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum TitleType implements ProtobufMessage {
        /**
         * No title
         */
        NONE(0),

        /**
         * Document title
         */
        DOCUMENT_MESSAGE(1),

        /**
         * Highly structured message title
         */
        HIGHLY_STRUCTURED_MESSAGE(2),

        /**
         * Image title
         */
        IMAGE_MESSAGE(3),

        /**
         * Video title
         */
        VIDEO_MESSAGE(4),

        /**
         * Location title
         */
        LOCATION_MESSAGE(5);

        @Getter
        private final int index;

        @JsonCreator
        public static TitleType forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(TitleType.NONE);
        }
    }

    public static class FourRowTemplateBuilder {
        public FourRowTemplateBuilder hydratedButtons(List<ButtonTemplate> buttons) {
            if (this.buttons == null)
                this.buttons = new ArrayList<>();
            this.buttons.addAll(buttons);
            return this;
        }
    }
}
