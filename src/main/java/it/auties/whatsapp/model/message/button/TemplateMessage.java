package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.button.FourRowTemplate;
import it.auties.whatsapp.model.button.HydratedFourRowTemplate;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Arrays;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static java.util.Objects.requireNonNullElseGet;

/**
 * A model class that represents a message sent in a WhatsappBusiness chat that provides a list of buttons to choose from.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Accessors(fluent = true)
public final class TemplateMessage extends ContextualMessage implements ButtonMessage {
    /**
     * Four row template.
     * This property is defined only if {@link TemplateMessage#formatType()} == {@link Format#FOUR_ROW_TEMPLATE}.
     */
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = FourRowTemplate.class)
    private FourRowTemplate fourRowTemplate;

    /**
     * Hydrated four row template.
     * This property is defined only if {@link TemplateMessage#formatType()} == {@link Format#HYDRATED_FOUR_ROW_TEMPLATE}.
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HydratedFourRowTemplate.class)
    private HydratedFourRowTemplate hydratedFourRowTemplate;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 3, type = MESSAGE, implementation = ContextInfo.class)
    @Default
    private ContextInfo contextInfo = new ContextInfo();  

    /**
     * Hydrated template
     */
    @ProtobufProperty(index = 4, type = MESSAGE, implementation = HydratedFourRowTemplate.class)
    private HydratedFourRowTemplate hydratedTemplate;


    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage newFourRowTemplateMessage(@NonNull FourRowTemplate template) {
        return newFourRowTemplateMessage(template, null);
    }

    /**
     * Constructs a new template message
     *
     * @param template    the non-null template
     * @param contextInfo the nullable context info
     * @return a non-null template message
     */
    public static TemplateMessage newFourRowTemplateMessage(@NonNull FourRowTemplate template,
                                                            ContextInfo contextInfo) {
        return TemplateMessage.builder()
                .fourRowTemplate(template)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage newHydratedFourRowTemplateMessage(@NonNull HydratedFourRowTemplate template) {
        return newHydratedFourRowTemplateMessage(template, null);
    }

    /**
     * Constructs a new template message
     *
     * @param template    the non-null template
     * @param contextInfo the nullable context info
     * @return a non-null template message
     */
    public static TemplateMessage newHydratedFourRowTemplateMessage(@NonNull HydratedFourRowTemplate template,
                                                                    ContextInfo contextInfo) {
        return TemplateMessage.builder()
                .hydratedFourRowTemplate(template)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Constructs a new template message
     *
     * @param template the non-null template
     * @return a non-null template message
     */
    public static TemplateMessage newHydratedTemplateMessage(@NonNull HydratedFourRowTemplate template) {
        return newHydratedFourRowTemplateMessage(template, null);
    }

    /**
     * Constructs a new template message
     *
     * @param template    the non-null template
     * @param contextInfo the nullable context info
     * @return a non-null template message
     */
    public static TemplateMessage newHydratedTemplateMessage(@NonNull HydratedFourRowTemplate template,
                                                             ContextInfo contextInfo) {
        return TemplateMessage.builder()
                .hydratedTemplate(template)
                .contextInfo(requireNonNullElseGet(contextInfo, ContextInfo::new))
                .build();
    }

    /**
     * Returns the type of format of this message
     *
     * @return a non-null {@link Format}
     */
    public Format formatType() {
        if (fourRowTemplate != null)
            return Format.FOUR_ROW_TEMPLATE;
        if (hydratedFourRowTemplate != null)
            return Format.HYDRATED_FOUR_ROW_TEMPLATE;
        return Format.NONE;
    }

    @Override
    public MessageType type() {
        return MessageType.TEMPLATE;
    }

    /**
     * The constant of this enumerated type define the various of types of visual formats for a {@link TemplateMessage}
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Format {
        /**
         * No format
         */
        NONE(0),

        /**
         * Four row template
         */
        FOUR_ROW_TEMPLATE(1),

        /**
         * Hydrated four row template
         */
        HYDRATED_FOUR_ROW_TEMPLATE(2);

        @Getter
        private final int index;

        @JsonCreator
        public static Format of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(Format.NONE);
        }
    }
}
