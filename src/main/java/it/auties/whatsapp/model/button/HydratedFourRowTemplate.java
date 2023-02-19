package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.standard.DocumentMessage;
import it.auties.whatsapp.model.message.standard.ImageMessage;
import it.auties.whatsapp.model.message.standard.LocationMessage;
import it.auties.whatsapp.model.message.standard.VideoMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * {@link HydratedFourRowTemplate#titleType()} == {@link TitleType#DOCUMENT_MESSAGE}.
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = DocumentMessage.class)
    private DocumentMessage documentTitle;

    /**
     * The text title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link TitleType#TEXT_TITLE}.
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String textTitle;

    /**
     * The image title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link TitleType#IMAGE_MESSAGE}.
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ImageMessage.class)
    private ImageMessage imageTitle;

    /**
     * The video title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link TitleType#VIDEO_MESSAGE}.
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = VideoMessage.class)
    private VideoMessage videoTitle;

    /**
     * The location title of this row. This property is defined only if
     * {@link HydratedFourRowTemplate#titleType()} == {@link TitleType#LOCATION_MESSAGE}.
     */
    @ProtobufProperty(index = 5, type = MESSAGE, implementation = LocationMessage.class)
    private LocationMessage locationTitle;

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
     * Constructs a new builder to create a four row template without a title
     *
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "EmptyFourRowTemplateBuilder", builderMethodName = "withoutTitleBuilder")
    private static HydratedFourRowTemplate emptyBuilder(String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).build();
    }

    private static HydratedFourRowTemplateBuilder createBuilder(String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        IntStream.range(0, buttons.size()).forEach(index -> buttons.get(index).index(index + 1));
        return HydratedFourRowTemplate.builder().body(body).footer(footer).buttons(buttons).templateId(id);
    }

    /**
     * Constructs a new builder to create a four row template with a document title
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "DocumentHydratedFourRowTemplateBuilder", builderMethodName = "withDocumentTitleBuilder")
    private static HydratedFourRowTemplate documentBuilder(DocumentMessage title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).documentTitle(title).build();
    }

    /**
     * Constructs a new builder to create a four row template with a text title
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "HighlyStructuredHydratedFourRowTemplateBuilder", builderMethodName = "withTextTitleBuilder")
    private static HydratedFourRowTemplate textBuilder(String title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).textTitle(title).build();
    }

    /**
     * Constructs a new builder to create a four row template with an image title
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "ImageHydratedFourRowTemplateBuilder", builderMethodName = "withImageTitleBuilder")
    private static HydratedFourRowTemplate imageBuilder(ImageMessage title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).imageTitle(title).build();
    }

    /**
     * Constructs a new builder to create a four row template with a video title
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "VideoHydratedFourRowTemplateBuilder", builderMethodName = "withVideoTitleBuilder")
    private static HydratedFourRowTemplate videoBuilder(VideoMessage title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).videoTitle(title).build();
    }

    /**
     * Constructs a new builder to create a four row template with a location title
     *
     * @param title   the title of this template
     * @param body    the body of this template
     * @param footer  the footer of this template
     * @param buttons the buttons of this template
     * @return a non-null new template
     */
    @Builder(builderClassName = "LocationHydratedFourRowTemplateBuilder", builderMethodName = "withLocationTitleBuilder")
    private static HydratedFourRowTemplate locationBuilder(LocationMessage title, String body, String footer, List<HydratedTemplateButton> buttons, String id) {
        return createBuilder(body, footer, buttons, id).locationTitle(title).build();
    }

    /**
     * Returns the type of title that this template wraps
     *
     * @return a non-null title type
     */
    public TitleType titleType() {
        if (documentTitle != null) {
            return TitleType.DOCUMENT_MESSAGE;
        }
        if (textTitle != null) {
            return TitleType.TEXT_TITLE;
        }
        if (imageTitle != null) {
            return TitleType.IMAGE_MESSAGE;
        }
        if (videoTitle != null) {
            return TitleType.VIDEO_MESSAGE;
        }
        if (locationTitle != null) {
            return TitleType.LOCATION_MESSAGE;
        }
        return TitleType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various types of title that a template can
     * wrap
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
         * Text title
         */
        TEXT_TITLE(2),
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
        public static TitleType of(int index) {
            return Arrays.stream(values()).filter(entry -> entry.index() == index).findFirst().orElse(TitleType.NONE);
        }
    }

    public static class HydratedFourRowTemplateBuilder {
        public HydratedFourRowTemplateBuilder buttons(List<HydratedTemplateButton> hydratedButtons) {
            if (this.buttons == null) {
                this.buttons = new ArrayList<>();
            }
            this.buttons.addAll(hydratedButtons);
            return this;
        }
    }
}